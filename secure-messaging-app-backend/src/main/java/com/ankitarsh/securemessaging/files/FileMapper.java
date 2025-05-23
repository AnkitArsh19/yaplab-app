package com.ankitarsh.securemessaging.files;

import org.springframework.stereotype.Service;

@Service
public class FileMapper {

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
