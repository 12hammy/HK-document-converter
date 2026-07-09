package com.example.demo.hamidu;



import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

    public class ConverterApp {
        public static void main(String[] args) {
            Scanner scanner = new Scanner(System.in);

            System.out.println("=== ADVANCED SECURE FILE CONVERTER INTEGRATED ===");
            System.out.println("[MFUMO] Tafadhali chagua faili la CSV unalotaka kubadilisha kutoka kwenye kompyuta yako...");

            // 1. Kufungua Dirisha la Kuchagua Faili (File Chooser Window)
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Chagua Faili la CSV la Kubadilisha");

            // Inaruhusu faili za .csv pekee kuonekana
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Faili za CSV (*.csv)", "csv");
            fileChooser.setFileFilter(filter);

            int userSelection = fileChooser.showOpenDialog(null);

            if (userSelection != JFileChooser.APPROVE_OPTION) {
                System.out.println("[INFO] Ulighairi kuchagua faili. Programu inafungwa.");
                scanner.close();
                return;
            }

            // Kupata faili halisi lililochaguliwa na mtumiaji
            File selectedFile = fileChooser.getSelectedFile();
            String sourceCsvPath = selectedFile.getAbsolutePath();
            String parentDirectory = selectedFile.getParent(); // Mahali faili lilipo sasa

            System.out.println("[MAFANIKIO] Umechagua faili: " + selectedFile.getName());
            System.out.println("Njia ya faili (Path): " + sourceCsvPath);

            // 2. Kuchagua Aina ya Kubadilisha
            System.out.println("\nChagua aina ya ubadilishaji unaotaka kufanya:");
            System.out.println("1. CSV kwenda JSON (Pamoja na Data Analytics)");
            System.out.println("2. CSV kwenda Secure PDF (Pamoja na AES-256 Encryption)");
            System.out.print("Ingiza chaguo lako (1 au 2): ");
            String choice = scanner.nextLine();

            IFileConverter converter = null;
            String destFile = "";

            // Kuhifadhi faili jipya kwenye folda lile lile ambalo faili la asili lipo
            if (choice.equals("1")) {
                converter = ConverterFactory.getConverter("CSV_TO_JSON", null);
                destFile = parentDirectory + File.separator + "converted_" + System.currentTimeMillis() + ".json";
            } else if (choice.equals("2")) {
                System.out.print("[USALAMA] Ingiza nenosiri (Password) ya kufungia PDF: ");
                String password = scanner.nextLine();
                converter = ConverterFactory.getConverter("SECURE_PDF", password);
                destFile = parentDirectory + File.separator + "secure_converted_" + System.currentTimeMillis() + ".pdf";
            } else {
                System.out.println("Chaguo sio sahihi! Programu inafungwa.");
                scanner.close();
                return;
            }

            // 3. Kuanza ubadilishaji na Kuonyesha Mahali faili jipya lilipo
            try {
                System.out.println("\n[MCHAKATO] Inabadilisha faili yako, tafadhali subiri...");
                ConvertedFileData result = converter.convert(sourceCsvPath, destFile);

                System.out.println("\n================ KAZI IMEMALIZIKA ================");
                System.out.println(" Jina la Faili Jipya  : " + result.getFileName());
                System.out.println(" Mahali Lilipohifadhiwa: " + destFile); // << HAPA UTAONA MAHALI LILIPO!
                System.out.println(" Idadi ya Data Rows   : " + result.getTotalRowsProcessed());
                System.out.println(" Ukubwa wa Faili (RAM): " + result.getFileSizeInBytes() + " Bytes");
                System.out.println(" Hali ya Usimbaji (AES): " + (result.isEncrypted() ? "IMELINDWA KWA PASSWORD" : "HAINA PASSWORD"));
                System.out.println("==================================================");

                // Kufungua folda lililo na faili jipya kiotomatiki ili mtumiaji alione mara moja
                try {
                    java.awt.Desktop.getDesktop().open(new File(parentDirectory));
                    System.out.println("[MFUMO] Folda lenye faili jipya limefunguliwa kiotomatiki kwenye kompyuta yako.");
                } catch (Exception e) {
                    System.out.println("[INFO] Folda halikuweza kufunguka kiotomatiki, lakini faili lipo hapo juu.");
                }

            } catch (IOException e) {
                System.out.println("[KOSA MKUBWA] Mchakato umeshindwa: " + e.getMessage());
            }

            scanner.close();
        }
    }




