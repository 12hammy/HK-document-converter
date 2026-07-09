package com.example.demo.hamidu;

import java.io.IOException;
public interface IFileConverter {


        // Mbinu kuu ya kubadilisha faili
        ConvertedFileData convert(String sourcePath, String destPath) throws IOException;

        // Mbinu ya kusafisha metadata kwa ajili ya usalama wa mtandao
        void stripMetadata(String filePath);


}
