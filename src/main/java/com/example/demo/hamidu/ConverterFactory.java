package com.example.demo.hamidu;

public class ConverterFactory {

        public static IFileConverter getConverter(String type, String securityKey) {
            if (type.equalsIgnoreCase("CSV_TO_JSON")) {
                return new CsvToJsonConverter();
            } else if (type.equalsIgnoreCase("SECURE_PDF")) {
                return new SecureDataToPdfConverter(securityKey);
            }
            throw new IllegalArgumentException("Aina hii ya ubadilishaji haitambuliki!");
        }


}
