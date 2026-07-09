package com.example.demo.hamidu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/converter")
@CrossOrigin(origins = "*") // Inaruhusu frontend yoyote kuwasiliana nayo
public class FileConverterController {

    @Autowired
    private UniversalConverterService converterService;

    @PostMapping("/convert")
    public ResponseEntity<byte[]> convertFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetExt") String targetExt) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            // MAREKEBISHO: Hapa tumeweka 'targetExt' badala ya targetExtension ya zamani
            byte[] convertedBytes = converterService.convertFile(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    targetExt
            );

            // Kutengeneza jina jipya la faili
            String originalName = file.getOriginalFilename();
            String baseName = originalName != null && originalName.contains(".") ?
                    originalName.substring(0, originalName.lastIndexOf(".")) : "converted_file";

            // MAREKEBISHO: Hapa pia tumeweka 'targetExt' ili kupata jina sahihi la faili
            String newFileName = baseName + "_converted." + targetExt.toLowerCase();

            // Kurudisha faili kama download kwa mtumiaji
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename(newFileName).build());

            return new ResponseEntity<>(convertedBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Error-Message", e.getMessage())
                    .body(null);
        }
    }
}
