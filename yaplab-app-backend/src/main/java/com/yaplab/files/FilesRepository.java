package com.yaplab.files;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface to manage File entity.
 * Extends JPARepository to perform CRUD operations.
 */
public interface FilesRepository extends JpaRepository<File, Long> {
}
