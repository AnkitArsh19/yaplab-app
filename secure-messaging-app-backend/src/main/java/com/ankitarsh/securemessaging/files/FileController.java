package com.ankitarsh.securemessaging.files;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

   @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId
    ) {
        try {
            FileUploadResponseDTO fileInfo = fileService.getFileInfo(fileId);
            Resource resource = fileService.downloadFile(fileId); // FileService now returns Resource

            // Determine content type
            Path filePath = Paths.get(fileService.getUploadDir()).resolve(fileInfo.fileName()); // Get path to probe content type
            String contentType = Files.probeContentType(filePath);

            HttpHeaders headers = new HttpHeaders();
            // Corrected Content-Disposition header
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileInfo.fileName() + "\"");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .headers(headers)
                    .body(resource);

        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (IllegalArgumentException e) {
            // This would catch the "File not found with ID" from FileService
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 404 Not Found with no body
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // 500 Internal Server Error
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponseDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) {
        try {
            FileUploadResponseDTO response = fileService.uploadFile(file, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // e.g., file size exceeds limit
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long fileId
    ) {
        try {
            fileService.deleteFile(fileId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileUploadResponseDTO> getFileInfo(
            @PathVariable Long fileId
    ){
        try {
            FileUploadResponseDTO fileInfo = fileService.getFileInfo(fileId);
            return ResponseEntity.ok(fileInfo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 404 Not Found
        }
    }
}
