package com.yaplab.files;

import com.yaplab.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface to manage File entity.
 * Extends JPARepository to perform CRUD operations.
 */
@Repository
public interface FilesRepository extends JpaRepository<File, Long> {
    
    /**
     * Deletes all files uploaded by a specific user
     * @param uploadedBy The user who uploaded the files
     */
    void deleteByUploadedBy(User uploadedBy);
}
