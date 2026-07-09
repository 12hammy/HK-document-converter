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
import java.util.Iterator;

@Service
public class UniversalConverterService {

    public byte[] convertFile(byte[] fileBytes, String originalName, String targetExt) throws Exception {
        String sourceExt = getFileExtension(originalName).toLowerCase();
        targetExt = targetExt.toLowerCase();

        // 1. Data to JSON
        if (sourceExt.equals("csv") && targetExt.equals("json")) return convertCsvToJson(fileBytes);
        if (sourceExt.equals("xlsx") && targetExt.equals("json")) return convertExcelToJson(fileBytes);

        // 2. Mifumo tofauti kwenda PDF (Kulingana na HTML Select targetExt)
        if ((sourceExt.equals("png") || sourceExt.equals("jpg") || sourceExt.equals("jpeg")) && targetExt.equals("pdf_from_image")) return convertImageToPdf(fileBytes);
        if (sourceExt.equals("docx") && targetExt.equals("pdf_from_word")) return convertWordToPdf(fileBytes);
        if (sourceExt.equals("xlsx") && targetExt.equals("pdf_from_excel")) return convertExcelToPdf(fileBytes);

        // 3. PDF kwenda Mifumo mingine
        if (sourceExt.equals("pdf") && targetExt.equals("docx")) return convertPdfToWord(fileBytes);
        if (sourceExt.equals("pdf") && targetExt.equals("xlsx")) return convertPdfToExcel(fileBytes);
        if (sourceExt.equals("pdf") && targetExt.equals("jpg")) return convertPdfToImage(fileBytes);
        if (sourceExt.equals("pdf") && targetExt.equals("txt")) return convertPdfToText(fileBytes);

        // 4. Mifumo mingine kwenda Word / Text
        if (sourceExt.equals("docx") && targetExt.equals("txt")) return convertWordToText(fileBytes);
        if (sourceExt.equals("xlsx") && targetExt.equals("docx")) return convertExcelToWord(fileBytes); // MPYA!

        throw new IllegalArgumentException("Ubadilishaji kutoka ." + sourceExt + " kwenda ." + targetExt + " haujatengenezwa bado!");
    }

    private byte[] convertExcelToPdf(byte[] excelBytes) throws Exception {
        Workbook wb = null;
        com.lowagie.text.Document document = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes));
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // Kutengeneza karatasi ya PDF kwa kutumia OpenPDF
            document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate()); // Landscape ili Excel ienee
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            // Pata idadi ya seli (columns) kwenye safu ya kwanza
            int maxCols = 0;
            for (Row row : sheet) {
                if (row.getLastCellNum() > maxCols) {
                    maxCols = row.getLastCellNum();
                }
            }

            if (maxCols == 0) maxCols = 1;

            // Tengeneza jedwali rasmi la PDF lenye idadi sahihi ya seli
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
            throw new RuntimeException("Kosa la ubadilishaji: " + e.getMessage(), e);
        } finally {
            if (wb != null) wb.close();
            if (document != null && document.isOpen()) document.close();
            out.close();
        }
    }



    private byte[] convertExcelToWord(byte[] excelBytes) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes)); XWPFDocument wordDoc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                StringBuilder rowText = new StringBuilder();
                for (Cell cell : row) rowText.append(cell.toString().trim()).append("\t");
                wordDoc.createParagraph().createRun().setText(rowText.toString().trim());
            }
            wordDoc.write(out); return out.toByteArray();
        }
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
        try (PDDocument pdfDoc = PDDocument.load(pdfBytes); XWPFDocument wordDoc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String[] lines = new PDFTextStripper().getText(pdfDoc).split("\n");
            for (String line : lines) { wordDoc.createParagraph().createRun().setText(line.trim()); }
            wordDoc.write(out); return out.toByteArray();
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

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
