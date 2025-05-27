package com.yaplab.files;

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
            Resource resource = fileService.downloadFile(fileId);

            Path filePath = Paths.get(fileService.getUploadDir()).resolve(fileInfo.fileName());
            String contentType = Files.probeContentType(filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileInfo.fileName() + "\"");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .headers(headers)
                    .body(resource);

        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
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
            return ResponseEntity.badRequest().body(null);
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
