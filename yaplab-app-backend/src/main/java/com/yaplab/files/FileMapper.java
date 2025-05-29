package com.yaplab.files;

import org.springframework.stereotype.Service;

/**
 * Service layer to map file from DTOs to entities and vice versa.
 */
@Service
public class FileMapper {

    /**
     * Returns a fileResponseDTO from the file entity
     */
    public FileUploadResponseDTO toFileUploadResponseDTO(File file){

        if(file == null){
            return null;
        }else{
        return new FileUploadResponseDTO(
                file.getId(),
                file.getFileName(),
                file.getFileType(),
                file.getFileSize(),
                file.getFileUrl(),
                file.getUploadedBy() != null ? file.getUploadedBy().getId(): null,
                file.getUploadedBy() != null ? file.getUploadedBy().getUserName(): null,
                file.getUploadedAt().toString()
        );
        }
    }
}
