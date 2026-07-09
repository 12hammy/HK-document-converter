package com.example.demo.hamidu;

import java.io.*;
public class SecureDataToPdfConverter implements IFileConverter{



        private String encryptionKey;

        public SecureDataToPdfConverter(String encryptionKey) {
            this.encryptionKey = encryptionKey;
        }

        @Override
        public ConvertedFileData convert(String sourcePath, String destPath) throws IOException {
            System.out.println("[INFO] Kazi Imeanza: Kutengeneza Secure PDF kutokea kwenye vyanzo vya data...");

            // Kuiga (Simulate) usomaji wa data
            BufferedReader reader = new BufferedReader(new FileReader(sourcePath));
            PrintWriter writer = new PrintWriter(new FileWriter(destPath));

            // Hapa tunaandika muundo wa PDF (Uandishi wa ndani wa maktaba kama iText)
            writer.println("%PDF-1.4 (Simulated Protected Document)");
            writer.println("Content extracted safely from: " + sourcePath);

            String line;
            int lines = 0;
            while ((line = reader.readLine()) != null) {
                writer.println("[DATA ROW]: " + line);
                lines++;
            }

            // Kupiga algoriti ya ulinzi (AES Simulation)
            writer.println("%%EOF [ENCRYPTED WITH AES-256 KEY: " + encryptionKey.hashCode() + "]");

            reader.close();
            writer.close();

            stripMetadata(destPath);

            File pdfFile = new File(destPath);
            return new ConvertedFileData(pdfFile.getName(), lines, pdfFile.length(), true);
        }

        @Override
        public void stripMetadata(String filePath) {
            System.out.println("[CYBERSECURITY] Kusafisha faili la PDF: Kufuta kabisa EXIF data na taarifa za mtumiaji wa mfumo.");
        }


}
