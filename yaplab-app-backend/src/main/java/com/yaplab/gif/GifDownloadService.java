package com.yaplab.gif;

import com.yaplab.files.File;
import com.yaplab.files.FileMapper;
import com.yaplab.files.FileUploadResponseDTO;
import com.yaplab.files.FilesRepository;
import com.yaplab.user.User;
import com.yaplab.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service class for downloading GIFs from a URL and storing them as files.
 * It uses RestTemplate to fetch the GIF data and saves it to the local filesystem.
 * The file metadata is stored in the database using FilesRepository.
 */
@Service
public class GifDownloadService {

    // Directory where uploaded files are stored, can be overridden in application properties
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final RestTemplate restTemplate;
    private final FilesRepository filesRepository;
    private final UserService userService;
    private final FileMapper fileMapper;

    public GifDownloadService(FilesRepository filesRepository, UserService userService, FileMapper fileMapper) {
        this.restTemplate = new RestTemplate();
        this.filesRepository = filesRepository;
        this.userService = userService;
        this.fileMapper = fileMapper;
    }

    /**
     * Downloads a GIF from URL and stores it as a file
     * @param gifUrl The URL of the GIF to download
     * @param title The title/name for the GIF
     * @param userId The ID of the user downloading the GIF
     * @return FileUploadResponseDTO with file details
     */
    @Transactional
    public FileUploadResponseDTO downloadAndStoreGif(String gifUrl, String title, Long userId) throws IOException {
        byte[] gifData = restTemplate.getForObject(gifUrl, byte[].class);
        if (gifData == null) {
            throw new IOException("Failed to download GIF from URL: " + gifUrl);
        }        String fileName = System.currentTimeMillis() + "_" + sanitizeFileName(title) + ".gif";
        // Use the gifs subdirectory and absolute path
        String subDir = "gifs";
        
        // Ensure upload directory structure exists with absolute path
        Path baseUploadPath = Paths.get(uploadDir).toAbsolutePath();
        Path uploadPath = baseUploadPath.resolve(subDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Store the relative path for the file (includes subdirectory)
        String relativePath = subDir + "/" + fileName;
        
        // Save file to disk
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, gifData);

        User user = userService.getUserEntityByID(userId);
        
        // Save to database first to get the ID
        File file = new File(
                relativePath, // Store relative path instead of just filename
                "image/gif",
                (long) gifData.length,
                null, // URL is set after saving
                user
        );
        
        File savedFile = filesRepository.save(file);
        
        // Now set the correct fileUrl using the generated ID
        savedFile.setFileUrl("/files/download/" + savedFile.getId());

        return fileMapper.toFileUploadResponseDTO(savedFile);
    }

    /**
     * Sanitizes filename by removing invalid characters
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "gif";
        }
        
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                      .replaceAll("_{2,}", "_")
                      .substring(0, Math.min(fileName.length(), 50));
    }
}
