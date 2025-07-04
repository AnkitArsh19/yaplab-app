package com.yaplab.files;

import org.springframework.core.io.Resource;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * REST Controller for handling file operations.
 * Provides endpoints for uploading, downloading and deleting file.
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    /**
     * Constructor based dependency injection
     */
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Returns a file as a resource for the client to download.
     * File is wrapped as a resource using file service.
     * The content type is determined using the extension of the file.
     * A new header is created and tells browser to show the file in the window.
     * Response entity is created using custom headers.
     * File added in HTTP header and streamed on the client side
     * @param fileId ID of the file
     */
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
            // Content-Disposition header is set inline to display the file in the browser
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

    /**
     * Uploads a file to the server
     * @param file File uploaded by the user
     * @param userId User ID of the user
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponseDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) {
        try {
            FileUploadResponseDTO response = fileService.uploadFile(file, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("File upload failed (IllegalArgumentException): {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (IOException e) {
            logger.error("File upload failed (IOException): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Deletes a file from the server
     * @param fileId File ID of the file to be deleted
     */
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

    /**
     * Returns the information about the uploaded file
     * @param fileId File ID of the file
     */
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

    /**
     * Serves group profile pictures from the uploads/groups directory
     * @param fileName The name of the profile picture file
     * @return ResponseEntity containing the image resource
     */
    @GetMapping("/serve/groups/{fileName}")
    public ResponseEntity<Resource> serveGroupProfilePicture(@PathVariable String fileName) {
        try {
            // Create absolute path for uploads/groups directory
            Path projectRoot = Paths.get("").toAbsolutePath();
            Path filePath = projectRoot.resolve("uploads").resolve("groups").resolve(fileName);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new PathResource(filePath);
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "image/jpeg"; // Default to JPEG
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache for 1 hour
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Serves user profile pictures from the uploads/users directory
     * @param fileName The name of the profile picture file
     * @return ResponseEntity containing the image resource
     */
    @GetMapping("/serve/users/{fileName}")
    public ResponseEntity<Resource> serveUserProfilePicture(@PathVariable String fileName) {
        try {
            // Create absolute path for uploads/users directory
            Path projectRoot = Paths.get("").toAbsolutePath();
            Path filePath = projectRoot.resolve("uploads").resolve("users").resolve(fileName);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new PathResource(filePath);
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "image/jpeg"; // Default to JPEG
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache for 1 hour
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
