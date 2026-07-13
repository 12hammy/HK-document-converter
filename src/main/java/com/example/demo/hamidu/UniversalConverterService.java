package com.example.demo.hamidu;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.image.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.json.JSONObject; // Hakikisha unayo maktaba ya JSON, kama huna tutaiweka chini

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


@Service
public class UniversalConverterService {

    public byte[] convertFile(byte[] fileBytes, String originalName, String targetExt) throws Exception {
        String sourceExt = getFileExtension(originalName).toLowerCase();
        targetExt = targetExt.toLowerCase();

        // MWONGOZO: Amri hizi zitalazimisha terminal ikuambie nini kinaingia
        System.out.println("===> SOURCE EXTENSION: " + sourceExt);
        System.out.println("===> TARGET EXTENSION: " + targetExt);


        // 1. Data to JSON
        if (sourceExt.equals("csv") && targetExt.equals("json")) return convertCsvToJson(fileBytes);
        if (sourceExt.equals("xlsx") && targetExt.equals("json")) return convertExcelToJson(fileBytes);

        // 2. Mifumo tofauti kwenda PDF (SASA INAKUBALI CSV PIA!)
        if ((sourceExt.equals("png") || sourceExt.equals("jpg") || sourceExt.equals("jpeg")) && targetExt.equals("pdf_from_image")) return convertImageToPdf(fileBytes);
        if (sourceExt.equals("docx") && targetExt.equals("pdf_from_word")) return convertWordToPdf(fileBytes);

        // HAPA TUMEREKEBISHA: Inaruhusu XLSX AU CSV kwenda kwenye PDF
        if ((sourceExt.equals("xlsx") || sourceExt.equals("csv")) && targetExt.equals("pdf_from_excel")) {
            return convertExcelToPdf(fileBytes, sourceExt);
        }

        // 3. PDF kwenda Mifumo mingine
        if (sourceExt.equals("pdf") && targetExt.equals("docx")) return convertPdfToWord(fileBytes);
        if (sourceExt.equals("pdf") && targetExt.equals("xlsx")) return convertPdfToExcel(fileBytes);
        if (sourceExt.equals("pdf") && targetExt.equals("jpg")) return convertPdfToImage(fileBytes);
        if (sourceExt.equals("pdf") && targetExt.equals("txt")) return convertPdfToText(fileBytes);

        // 4. Mifumo mingine kwenda Word / Text
        if (sourceExt.equals("docx") && targetExt.equals("txt")) return convertWordToText(fileBytes);
        if ((sourceExt.equals("xlsx") || sourceExt.equals("csv")) && targetExt.equals("docx")) {
            return convertExcelToWord(fileBytes, sourceExt); // Tumeongeza parameter ya sourceExt hapa
        }

        throw new IllegalArgumentException("Ubadilishaji kutoka ." + sourceExt + " kwenda ." + targetExt + " haujatengenezwa bado!");
    }

    // HAPA TUMEREKEBISHA: Tumeongeza parameter ya 'sourceExt' ili ijue kama inasoma CSV au XLSX
    private byte[] convertExcelToPdf(byte[] fileBytes, String sourceExt) throws Exception {
        Workbook wb = null;
        com.lowagie.text.Document document = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // Likiwa faili la CSV, tunaligeuza kwanza kuwa muundo wa Excel (Workbook) ndani ya RAM
            if (sourceExt.equals("csv")) {
                wb = convertCsvToWorkbook(fileBytes);
            } else {
                wb = new XSSFWorkbook(new ByteArrayInputStream(fileBytes));
            }

            Sheet sheet = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // Kutengeneza karatasi ya PDF kwa kutumia OpenPDF
            document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate()); // Landscape
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            int maxCols = 0;
            for (Row row : sheet) {
                if (row.getLastCellNum() > maxCols) {
                    maxCols = row.getLastCellNum();
                }
            }

            if (maxCols == 0) maxCols = 1;

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(maxCols);
            table.setWidthPercentage(100);

            com.lowagie.text.Font font = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 9);

            for (Row row : sheet) {
                for (int i = 0; i < maxCols; i++) {
                    Cell cell = row.getCell(i);
                    String cellValue = (cell != null) ? formatter.formatCellValue(cell) : "";

                    com.lowagie.text.pdf.PdfPCell pdfCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(cellValue, font));
                    pdfCell.setPadding(5);
                    table.addCell(pdfCell);
                }
            }

            document.add(table);
            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            // Inalazimisha kosa lisiloonekana lichapishwe kwenye Render Logs kwa usalama wetu!
            System.err.println("[EXCEL/CSV TO PDF ERROR]: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Kosa la ubadilishaji: " + e.getMessage(), e);
        } finally {
            if (wb != null) wb.close();
            if (document != null && document.isOpen()) document.close();
            out.close();
        }
    }

    // KAZI MPYA: Inabadilisha herufi za CSV kuwa Workbook ya Excel ili isomeke kwenye jedwali la PDF bila kufeli
    private Workbook convertCsvToWorkbook(byte[] csvBytes) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("CSV Data");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8))) {
            String line;
            int rowIndex = 0;
            while ((line = reader.readLine()) != null) {
                Row row = sheet.createRow(rowIndex++);
                // Tenganisha maneno kwa kutumia koma (CSV Standard)
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (int colIndex = 0; colIndex < values.length; colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    // Ondoa alama za nukuu zilizozidi kwenye CSV
                    String cleanValue = values[colIndex].trim().replaceAll("^\"|\"$", "");
                    cell.setCellValue(cleanValue);
                }
            }
        }
        return workbook;
    }

    private byte[] convertExcelToWord(byte[] fileBytes, String sourceExt) throws Exception {
        System.out.println("[INFO] Inabadilisha " + sourceExt.toUpperCase() + " kwenda Word (.docx)...");

        // TAYARISHA DOCKUMENT YA WORD
        try (XWPFDocument wordDoc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // --- NJIA A: KAMA FAILI NI CSV ---
            if (sourceExt.equals("csv")) {
                System.out.println("[INFO] Inasoma data za CSV moja kwa moja...");

                // Kusoma kila mstari wa CSV kwa kutumia standard encoding
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileBytes), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;

                    // Kwenye Word, kama hatuweki jedwali gumu, tunaweza kuandika mistari kwa mtindo wa aya (paragraphs) ulio safi
                    while ((line = reader.readLine()) != null) {
                        // Tenganisha maneno kwa koma au semicolon (Inasoma zote mbili kulinda usalama wako!)
                        String[] columns = line.split("[,;](?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                        XWPFParagraph paragraph = wordDoc.createParagraph();
                        XWPFRun run = paragraph.createRun();

                        StringBuilder rowContent = new StringBuilder();
                        for (String col : columns) {
                            // Safisha alama za nukuu zilizozidi
                            String cleanCol = col.trim().replaceAll("^\"|\"$", "");
                            rowContent.append(cleanCol).append("\t\t"); // Weka tab mbili ili yatengane vizuri kama safu
                        }

                        run.setText(rowContent.toString().trim());
                    }
                }
            }
            // --- NJIA B: KAMA FAILI NI EXCEL (.XLSX) ---
            else {
                System.out.println("[INFO] Inasoma data za Excel (.xlsx) kwa kutumia Apache POI...");
                try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
                    Sheet sheet = wb.getSheetAt(0);
                    DataFormatter formatter = new DataFormatter();

                    for (Row row : sheet) {
                        XWPFParagraph paragraph = wordDoc.createParagraph();
                        XWPFRun run = paragraph.createRun();
                        StringBuilder rowText = new StringBuilder();

                        for (Cell cell : row) {
                            rowText.append(formatter.formatCellValue(cell)).append("\t\t");
                        }
                        run.setText(rowText.toString().trim());
                    }
                }
            }

            // ANDIKA MATOKEO KWENYE FILE LA WORD LINADOWNLOADIKA
            wordDoc.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            // Amri hii italazimisha kosa lolote linalofanya ifeli lionekane sasa hivi kwenye terminal ya IntelliJ!
            System.err.println("[CRITICAL ERROR IN CONVERSION]: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


    // Saidia kuweka njia ya kupata jina la faili
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }





private byte[] convertPdfToExcel(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfBytes); Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Data"); String pdfText = new PDFTextStripper().getText(doc);
            String[] lines = pdfText.split("\n"); int rowNum = 0;
            for (String line : lines) {
                Row row = sheet.createRow(rowNum++); String[] tokens = line.split("\\s{2,}|\\t"); int cellNum = 0;
                for (String t : tokens) row.createCell(cellNum++).setCellValue(t.trim());
            }
            wb.write(out); return out.toByteArray();
        }
    }

    private byte[] convertPdfToImage(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfBytes); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage bim = new PDFRenderer(doc).renderImageWithDPI(0, 300); ImageIO.write(bim, "jpg", out); return out.toByteArray();
        }
    }

    private byte[] convertPdfToText(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfBytes)) { return new PDFTextStripper().getText(doc).getBytes(); }
    }

    private byte[] convertWordToText(byte[] docxBytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes)); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (XWPFParagraph p : doc.getParagraphs()) out.write((p.getText() + "\n").getBytes());
            return out.toByteArray();
        }
    }

    private byte[] convertImageToPdf(byte[] imgBytes) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(); doc.addPage(page); BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(imgBytes));
            if (bimg == null) throw new IOException("Picha ina hitilafu!");
            PDImageXObject pdImage = JPEGFactory.createFromImage(doc, bimg);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                float scale = Math.min(page.getMediaBox().getWidth() / pdImage.getWidth(), page.getMediaBox().getHeight() / pdImage.getHeight());
                stream.drawImage(pdImage, 20, 20, pdImage.getWidth() * scale - 40, pdImage.getHeight() * scale - 40);
            }
            doc.save(out); return out.toByteArray();
        }
    }

    private byte[] convertExcelToJson(byte[] excelBytes) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes)); ByteArrayOutputStream out = new ByteArrayOutputStream(); BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out))) {
            Sheet sheet = wb.getSheetAt(0); Iterator<Row> rowIterator = sheet.iterator(); if (!rowIterator.hasNext()) throw new IOException("Excel haina data!");
            Row headerRow = rowIterator.next(); String[] headers = new String[headerRow.getLastCellNum()];
            for (int i = 0; i < headers.length; i++) { Cell c = headerRow.getCell(i); headers[i] = c != null ? c.toString().trim() : "Column_" + i; }
            w.write("[\n"); boolean firstRow = true;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next(); if (!firstRow) w.write(",\n"); firstRow = false; w.write("  {\n");
                for (int i = 0; i < headers.length; i++) {
                    Cell c = row.getCell(i); String val = c != null ? c.toString().trim() : ""; w.write("    \"" + headers[i] + "\": \"" + val.replace("\"", "\\\"") + "\"");
                    if (i < headers.length - 1) w.write(","); w.write("\n");
                }
                w.write("  }");
            }
            w.write("\n]"); w.flush(); return out.toByteArray();
        }
    }


    private byte[] convertPdfToWord(byte[] pdfBytes) throws Exception {
        System.out.println("[INFO] Inabadilisha PDF kwenda Word - Ultimate Base64 Cloud OCR...");

        try (org.apache.pdfbox.pdmodel.PDDocument pdfDoc = org.apache.pdfbox.pdmodel.PDDocument.load(new ByteArrayInputStream(pdfBytes));
             XWPFDocument wordDoc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 1. Kagua maandishi ya kawaida kwanza
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String normalText = stripper.getText(pdfDoc);

            if (normalText == null || normalText.trim().isEmpty() || normalText.trim().length() < 10) {
                System.out.println("[INFO] Inagundua: Scanned PDF/Picha. Inatuma Base64 kwenda Cloud API...");

                // Geuza PDF kuwa Base64 string safi
                String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
                String payload = "data:application/pdf;base64," + base64Pdf;

                // Tengeneza JSON Object ya kutuma kama parameters
                URL url = new URL("https://ocr.space");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // Vigezo safi vya kutuma (Lugha ni Kiingereza inayohimili na herufi za Kiswahili)
                String postData = "apikey=helloworld" +
                        "&language=eng" +
                        "&isHandwritten=true" +
                        "&OcrEngine=2" +
                        "&base64Image=" + URLEncoder.encode(payload, "UTF-8");

                // Tupa data kwenye mtandao
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                // Usomaji salama unaozuia kabisa NullPointerException
                InputStream is = conn.getInputStream();
                if (is == null) {
                    is = conn.getErrorStream();
                }

                if (is != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        System.out.println("[API RESPONSE]: " + response.toString());

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.has("ParsedResults")) {
                            org.json.JSONArray parsedResults = jsonResponse.getJSONArray("ParsedResults");
                            for (int i = 0; i < parsedResults.length(); i++) {
                                String parsedText = parsedResults.getJSONObject(i).getString("ParsedText");

                                if (parsedText != null && !parsedText.trim().isEmpty()) {
                                    String[] lines = parsedText.split("\r\n|\n");
                                    for (String line : lines) {
                                        if (line.trim().length() > 0) {
                                            XWPFParagraph p = wordDoc.createParagraph();
                                            p.createRun().setText(line.trim());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println("[INFO] Inasoma PDF ya kawaida ya kompyuta...");
                String[] lines = normalText.split("\n");
                for (String line : lines) {
                    XWPFParagraph p = wordDoc.createParagraph();
                    p.createRun().setText(line.trim());
                }
            }

            wordDoc.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            System.err.println("[CRITICAL BASE64 OCR ERROR]: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }



    private byte[] convertWordToPdf(byte[] docxBytes) throws Exception {
        try (XWPFDocument wordDoc = new XWPFDocument(new ByteArrayInputStream(docxBytes)); PDDocument pdfDoc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(); pdfDoc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(pdfDoc, page)) {
                stream.beginText(); stream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12); stream.newLineAtOffset(50, 750);
                for (XWPFParagraph paragraph : wordDoc.getParagraphs()) {
                    String cleanText = paragraph.getText().replaceAll("[\\n\\r]", " ");
                    if (!cleanText.isEmpty()) {
                        if (cleanText.length() > 80) cleanText = cleanText.substring(0, 80);
                        stream.showText(cleanText); stream.newLineAtOffset(0, -15);
                    }
                }
                stream.endText();
            }
            pdfDoc.save(out); return out.toByteArray();
        }
    }

    private byte[] convertCsvToJson(byte[] csvBytes) throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csvBytes)));
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out))) {

            String headerLine = r.readLine();
            if (headerLine == null) throw new IOException("CSV haina data!");
            String[] headers = headerLine.split(",");
            String line;
            int rowCount = 0;
            w.write("[\n");

            while ((line = r.readLine()) != null) {
                String[] values = line.split(",");
                if (rowCount > 0) w.write(",\n");
                w.write("  {\n");

                for (int i = 0; i < headers.length; i++) {
                    String val = (i < values.length) ? values[i].trim() : "";
                    char q = '"';
                    w.write("    " + q + headers[i].trim() + q + ": " + q + val.replace(String.valueOf(q), "\\" + q) + q);
                    if (i < headers.length - 1) w.write(",");
                    w.write("\n");
                }
                w.write("  }");
                rowCount++;
            }
            w.write("\n]");
            w.flush();
            return out.toByteArray();
        }
    }
}

