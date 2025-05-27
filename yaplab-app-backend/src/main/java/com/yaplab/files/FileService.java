package com.yaplab.files;

import com.yaplab.user.UserService;
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

@Service
public class FileService {
    private final FileMapper fileMapper;
    private final FilesRepository filesRepository;
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    private final UserService userService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public FileService(FileMapper fileMapper, FilesRepository filesRepository, UserService userService) {
        this.fileMapper = fileMapper;
        this.filesRepository = filesRepository;
        this.userService = userService;
    }

    public FileUploadResponseDTO uploadFile(MultipartFile file, Long id) throws IOException {

        if(file.getSize()>MAX_FILE_SIZE){
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
        return fileMapper.toFileUploadResponseDTO(savedFile);
    }

    public Resource downloadFile(Long fileId) throws IOException {
        File file = filesRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));
        Path filePath = Paths.get(uploadDir).resolve(file.getFileName());

        try {
            return new InputStreamResource(Files.newInputStream(filePath));
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException("File not found on disk: " + file.getFileName());
        } catch (IOException e) {
            throw new IOException("Error reading file: " + file.getFileName(), e);
        }
    }

    public void deleteFile(Long id) throws IOException {
        File file = filesRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        Path filePath = Paths.get(uploadDir).resolve(file.getFileName());
        try {
            Files.deleteIfExists(filePath);
            filesRepository.delete(file);
        } catch (IOException e) {
            throw new IOException("Failed to delete file on disk: " + file.getFileName(), e);
        }
    }

    public FileUploadResponseDTO getFileInfo(Long id){
        File file = filesRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        return fileMapper.toFileUploadResponseDTO(file);
    }

    public String getUploadDir(){
        return uploadDir;
    }
    
}
