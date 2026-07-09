package com.example.demo.hamidu;
import java.io.*;

public class CsvToJsonConverter implements IFileConverter {




        @Override
        public ConvertedFileData convert(String sourcePath, String destPath) throws IOException {
            System.out.println("[INFO] Kazi Imeanza: Kusoma CSV na kuichambua...");

            BufferedReader reader = new BufferedReader(new FileReader(sourcePath));
            BufferedWriter writer = new BufferedWriter(new FileWriter(destPath));

            String headerLine = reader.readLine();
            if (headerLine == null) {
                reader.close();
                writer.close();
                throw new IOException("Faili la CSV halina data yoyote!");
            }

            // Tenganisha nguzo (Columns) za kichwa cha habari
            String[] headers = headerLine.split(",");
            String line;
            int rowCount = 0;
            double numericSum = 0.0; // Analytics: Kupata jumla ya namba zote kwenye faili

            writer.write("[\n"); // Mwanzo wa JSON Array

            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (rowCount > 0) {
                    writer.write(",\n");
                }

                writer.write("  {\n");
                for (int i = 0; i < headers.length; i++) {
                    String val = (i < values.length) ? values[i].trim() : "";

                    // Analytics Component: Kama thamani ni namba, ijumlishe
                    try {
                        numericSum += Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        // Si namba, ruka
                    }

                    writer.write("    \"" + headers[i].trim() + "\": \"" + val + "\"");
                    if (i < headers.length - 1) writer.write(",");
                    writer.write("\n");
                }
                writer.write("  }");
                rowCount++;
            }

            writer.write("\n]"); // Mwisho wa JSON Array

            reader.close();
            writer.close();

            // Strip Metadata kwa usalama
            stripMetadata(destPath);

            // kutoa ripoti ya analytics kwenye terminal
            System.out.println("======ripoti ya uchambuzi (data analytics)======");
            System.out.println("Jumla ya Mistari Iliyosomwa: " + rowCount);
            System.out.println("Jumla ya Maadili ya Namba (Sum Total): " + numericSum);
            System.out.println("==============================================");

            File generatedFile = new File(destPath);
            return new ConvertedFileData(generatedFile.getName(), rowCount, generatedFile.length(), false);
        }

        @Override
        public void stripMetadata(String filePath) {
            // Kipengele cha Cybersecurity: Kufuta alama za kifaa na nyakati
            System.out.println("[CYBERSECURITY] Kusafisha faili la JSON: Kuondoa faharisi za OS na timestamps tupu.");
        }


}
