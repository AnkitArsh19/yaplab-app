package com.yaplab.files;

import com.yaplab.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * File entity to store filenames, size, type url and user details.
 */
@Entity
@Table(name = "files")
public class File {

    /**
     * Unique identifier for each file which is assigned automatically.
     * Long is preferred for large datasets.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Filename as a string
     */
    @Column(nullable = false)
    private String fileName;

    /**
     * Type of the file audio, video, etc
     */
    @Column(nullable = false)
    private String fileType;

    /**
     * Size of the file(in Bytes)
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * Url of the file which stores the location of a physical path
     */
    @Column(nullable = false)
    private String fileUrl;

    /**
     * User details of the uploader
     * Many files can be uploaded by one user
     */
    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    /**
     * Time of upload(permanent)
     */
    @Column(nullable = false, updatable = false)
    private final Instant uploadedAt = Instant.now();

    /**
     * Default constructor
     */
    public File() {}

    /**
     * Parameterized constructor
     */
    public File(String fileName, String fileType, Long fileSize, String fileUrl, User uploadedBy) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
        this.uploadedBy = uploadedBy;
    }

    /**
     * Getters and setters
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}