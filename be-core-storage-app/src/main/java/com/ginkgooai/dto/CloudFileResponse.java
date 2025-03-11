package com.ginkgooai.dto;

import com.ginkgooai.domain.CloudFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Cloud file data transfer object")
public class CloudFileResponse {

    @Schema(description = "File ID")
    private String id;

    @Schema(description = "Original file name")
    private String originalName;

    @Schema(description = "Storage file name")
    private String storageName;

    @Schema(description = "Storage file path")
    private String storagePath;

    @Schema(description = "File type")
    private String fileType;

    @Schema(description = "File size")
    private Long fileSize;

    @Schema(name = "Video thumbnail ID")
    private String videoThumbnailId;

    @Schema(name = "Video thumbnail URL")
    private String videoThumbnailUrl;

    @Schema(name = "Video duration")
    private Long videoDuration;

    @Schema(name = "Video resolution")
    private String videoResolution;

    public static CloudFileResponse fromCloudFile(CloudFile cloudFile, String storagePath) {

        return CloudFileResponse.builder()
                .id(cloudFile.getId())
                .originalName(cloudFile.getOriginalName())
                .storageName(cloudFile.getStorageName())
                .storagePath(storagePath)
                .fileType(cloudFile.getFileType())
                .fileSize(cloudFile.getFileSize())

                .build();
    }

    public static CloudFileResponse fromCloudFile(CloudFile cloudFile, String storagePath, String thumbnailUrl) {

        CloudFileResponse cloudFileResponse = fromCloudFile(cloudFile, storagePath);
        cloudFileResponse.setVideoThumbnailId(cloudFile.getVideoThumbnailId());
        cloudFileResponse.setVideoThumbnailUrl(thumbnailUrl);
        cloudFileResponse.setVideoDuration(cloudFile.getVideoDuration());
        cloudFileResponse.setVideoResolution(cloudFile.getVideoResolution());
        return cloudFileResponse;
    }

}
