package com.yaplab.files;

import com.yaplab.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service class for handling file related operations like upload, download, delete, etc.
 */
@Service
public class FileService {

    /**
     * Logger for FileService
     * This logger is used to log various events and errors in the FileService class.
     * It helps in debugging and tracking the flow of operations related to file management.
     */
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    /**
     * Directory where uploaded files are stored.
     */
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;    /**
     * Maximum file size allowed is 50MB for all file types.
     */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * Constructor based dependency injection
     */
    private final FileMapper fileMapper;
    private final FilesRepository filesRepository;
    private final UserService userService;

    public FileService(FileMapper fileMapper, FilesRepository filesRepository, UserService userService) {
        this.fileMapper = fileMapper;
        this.filesRepository = filesRepository;
        this.userService = userService;
    }

    /**
     * Uploads the file in the directory if it doesn't exist.
     * Checks for file size and creates a consistent filename.
     * @param file represents a multipart file received in a multipart request
     * @param id ID of the file.
     * @return an upload responseDTO
     */
    @Transactional
    public FileUploadResponseDTO uploadFile(MultipartFile file, Long id) throws IOException {        String contentType = file.getContentType();
        
        if(file.getSize() > MAX_FILE_SIZE){
            logger.warn("File upload failed for user ID {}: File size exceeds max limit ({} bytes)", id, MAX_FILE_SIZE);
            throw new IllegalArgumentException("File size exceeds 50MB");
        }
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        
        // Determine subdirectory based on file type
        String subDir = getSubDirectoryForFileType(contentType);
        
        // Create absolute path for upload directory
        Path baseUploadPath = Paths.get(uploadDir).toAbsolutePath();
        Path uploadPath = baseUploadPath.resolve(subDir);

        
        // Create the type-specific directory structure
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            logger.error("Failed to create directory: {}", uploadPath, e);
            throw new IOException("Failed to create upload directory: " + uploadPath, e);
        }
        
        // Store the relative path for the file (includes subdirectory)
        String relativePath = subDir + "/" + fileName;
        Path filePath = uploadPath.resolve(fileName);
        
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            logger.error("Failed to save file to: {}", filePath, e);
            throw new IOException("Failed to save file: " + filePath, e);
        }File files = new File(
                relativePath, // Store relative path instead of just filename
                contentType,
                file.getSize(),
                null,
                userService.getUserEntityByID(id)
        );
        File savedFile = filesRepository.save(files);
        savedFile.setFileUrl("/files/download/" + savedFile.getId());
        return fileMapper.toFileUploadResponseDTO(savedFile);
    }

    /**
     * Method to download a file from the directory
     * Input stream resource represents a stream of incoming data that can only be read
     * @param fileId ID of the file
     * @return a resource of type inputStreamResource
     */
    public Resource downloadFile(Long fileId) throws IOException {
        File file = filesRepository.findById(fileId)
                .orElseThrow(() -> {
                    logger.warn("File download failed: File not found with ID: {}", fileId);
                    return new IllegalArgumentException("File not found with ID: " + fileId);
                });
        
        // Use absolute path for file resolution
        Path baseUploadPath = Paths.get(uploadDir).toAbsolutePath();
        Path filePath = baseUploadPath.resolve(file.getFileName());

        try {
            return new InputStreamResource(Files.newInputStream(filePath));
        } catch (NoSuchFileException e) {
            logger.error("File not found on disk: {}", filePath);
            throw new FileNotFoundException("File not found on disk: " + filePath);
        } catch (IOException e) {
            logger.error("Error reading file: {}", filePath, e);
            throw new IOException("Error reading file: " + filePath, e);
        }
    }

    /**
     * Deletes the file from the directory and it's details from database
     * @param id ID of the file
     */
    @Transactional
    public void deleteFile(Long id) throws IOException {
        File file = filesRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("File deletion failed: File not found with ID: {}", id);
                    return new IllegalArgumentException("File not found");
                });
        
        // Use absolute path for file resolution
        Path baseUploadPath = Paths.get(uploadDir).toAbsolutePath();
        Path filePath = baseUploadPath.resolve(file.getFileName());
        
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            filesRepository.delete(file);
        } catch (IOException e) {
            logger.error("Failed to delete file on disk: {}", filePath, e);
            throw new IOException("Failed to delete file on disk: " + filePath, e);
        }
    }

    /**
     * Returns the details of the file from its ID
     * @param id ID of the file
     */
    public FileUploadResponseDTO getFileInfo(Long id){
        File file = filesRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Get file info failed: File not found with ID: {}", id);
                    return new IllegalArgumentException("File not found");
                });
        return fileMapper.toFileUploadResponseDTO(file);
    }

    /**
     * Returns the location of the file
     */
    public String getUploadDir(){
        return uploadDir;
    }

    /**
     * Determines the subdirectory for file storage based on content type
     * @param contentType MIME type of the file
     * @return subdirectory name
     */
    private String getSubDirectoryForFileType(String contentType) {
        if (contentType == null) {
            return "other";
        }
        
        if (contentType.startsWith("image/")) {
            if (contentType.equals("image/gif")) {
                return "gifs";
            }
            return "images";
        } else if (contentType.startsWith("audio/")) {
            return "audio";
        } else if (contentType.startsWith("video/")) {
            return "videos";
        } else {
            return "documents";
        }
    }

}
