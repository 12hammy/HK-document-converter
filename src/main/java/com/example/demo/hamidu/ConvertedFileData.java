package com.example.demo.hamidu;

public class ConvertedFileData {

        private String fileName;
        private int totalRowsProcessed;
        private long fileSizeInBytes;
        private boolean isEncrypted;

        public ConvertedFileData(String fileName, int totalRowsProcessed, long fileSizeInBytes, boolean isEncrypted) {
            this.fileName = fileName;
            this.totalRowsProcessed = totalRowsProcessed;
            this.fileSizeInBytes = fileSizeInBytes;
            this.isEncrypted = isEncrypted;
        }

        // Getters pekee ili kuzuia mabadiliko ya nje (Immutability)
        public String getFileName() { return fileName; }
        public int getTotalRowsProcessed() { return totalRowsProcessed; }
        public long getFileSizeInBytes() { return fileSizeInBytes; }
        public boolean isEncrypted() { return isEncrypted; }


}
