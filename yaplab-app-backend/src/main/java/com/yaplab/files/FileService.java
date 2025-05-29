package com.yaplab.files;

import com.yaplab.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
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
    private String uploadDir;

    /**
     * Maximum file size allowed is only 50MB.
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
    public FileUploadResponseDTO uploadFile(MultipartFile file, Long id) throws IOException {
        logger.info("Attempting to upload file: {} for user ID: {}", file.getOriginalFilename(), id);

        if(file.getSize()>MAX_FILE_SIZE){
            logger.warn("File upload failed for user ID {}: File size exceeds max limit ({} bytes)", id, MAX_FILE_SIZE);
            throw new IllegalArgumentException("File size exceeds 50MB");
        }
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir);
        if(!Files.exists(uploadPath)){
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        File files = new File(
                fileName,
                file.getContentType(),
                file.getSize(),
                null,
                userService.getUserEntityByID(id)
        );
        File savedFile = filesRepository.save(files);
        savedFile.setFileUrl("/file/download/" + savedFile.getId());
        filesRepository.save(savedFile);
        logger.info("File uploaded successfully with ID: {} for user ID: {}", savedFile.getId(), id);
        return fileMapper.toFileUploadResponseDTO(savedFile);
    }

    /**
     * Method to download a file from the directory
     * Input stream resource represents a stream of incoming data that can only be read
     * @param fileId ID of the file
     * @return a resource of type inputStreamResource
     */
    public Resource downloadFile(Long fileId) throws IOException {
        logger.info("Attempting to download file with ID: {}", fileId);
        File file = filesRepository.findById(fileId)
                .orElseThrow(() -> {
                    logger.warn("File download failed: File not found with ID: {}", fileId);
                    return new IllegalArgumentException("File not found with ID: " + fileId);
                });
        Path filePath = Paths.get(uploadDir).resolve(file.getFileName());

        try {
            logger.info("File found on disk: {}", file.getFileName());
            return new InputStreamResource(Files.newInputStream(filePath));
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException("File not found on disk: " + file.getFileName()); // Logged by the exception handler if not caught
        } catch (IOException e) {
            throw new IOException("Error reading file: " + file.getFileName(), e);
        }
    }

    /**
     * Deletes the file from the directory and it's details from database
     * @param id ID of the file
     */
    public void deleteFile(Long id) throws IOException {
        logger.info("Attempting to delete file with ID: {}", id);
        File file = filesRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("File deletion failed: File not found with ID: {}", id);
                    return new IllegalArgumentException("File not found");
                });
        Path filePath = Paths.get(uploadDir).resolve(file.getFileName());
        try {
            Files.deleteIfExists(filePath);
            filesRepository.delete(file);
            logger.info("File deleted successfully with ID: {}", id);
        } catch (IOException e) {
            throw new IOException("Failed to delete file on disk: " + file.getFileName(), e); // Logged by the exception handler if not caught
        }
    }

    /**
     * Returns the details of the file from its ID
     * @param id ID of the file
     */
    public FileUploadResponseDTO getFileInfo(Long id){
        logger.info("Attempting to get file info for ID: {}", id);
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
}
