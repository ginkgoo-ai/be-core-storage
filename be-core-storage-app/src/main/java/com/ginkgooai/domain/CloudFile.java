package com.ginkgooai.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;

/**
 * @author: david
 * @date: 16:26 2025/2/21
 */
@Entity
@Table(name = "cloud_file")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "storage_name", nullable = false, unique = true)
    private String storageName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "storage_path")
    private String storagePath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Column(name = "uploader_id")
    private Long uploaderId;

    @Column(name = "bucket_name", nullable = false)
    private String bucketName;

    @Column(name = "video_thumbnail_id")
    private String videoThumbnailId;

    @Column(name = "video_thumbnail_url")
    private String videoThumbnailUrl;

    @Column(name = "video_duration")
    private Long videoDuration;

    @Column(name = "video_resolution")
    private String videoResolution;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    public static CloudFile fromFile(File file, String bucketName,
                                     String storageName, String storagePath) {
        CloudFile cloudFile = new CloudFile();
        cloudFile.setOriginalName(file.getName());
        cloudFile.setStorageName(storageName);
        cloudFile.setFileType(file.getName().substring(file.getName().lastIndexOf('.')));
        cloudFile.setFileSize(file.length());
        cloudFile.setStoragePath(storagePath);
        cloudFile.setBucketName(bucketName);
        return cloudFile;
    }


    public static CloudFile fromFile(MultipartFile file, String bucketName,
                                     String storageName, String storagePath) {
        return initBaseCloudFile(file, bucketName, storageName, storagePath);
    }

    public static CloudFile fromFile(MultipartFile file, String bucketName,
                                     String storageName, String storagePath,
                                     VideoMetadata videoMetadata) {
        return initBaseCloudFile(file, bucketName, storageName, storagePath)
                .setVideoMetadata(videoMetadata);
    }


    private static CloudFile initBaseCloudFile(MultipartFile file, String bucketName,
                                               String storageName, String storagePath) {
        CloudFile cloudFile = new CloudFile();
        cloudFile.setOriginalName(file.getOriginalFilename());
        cloudFile.setStorageName(storageName);
        cloudFile.setFileType(file.getContentType());
        cloudFile.setFileSize(file.getSize());
        cloudFile.setStoragePath(storagePath);
        cloudFile.setBucketName(bucketName);
        return cloudFile;
    }

    public CloudFile setVideoMetadata(VideoMetadata metadata) {
        if (metadata != null) {
            this.videoThumbnailId = metadata.getThumbnailFileId();
            this.videoThumbnailUrl = metadata.getThumbnailUrl();
            this.videoDuration = metadata.getDuration();
            this.videoResolution = metadata.getResolution();
        }
        return this;
    }
}
