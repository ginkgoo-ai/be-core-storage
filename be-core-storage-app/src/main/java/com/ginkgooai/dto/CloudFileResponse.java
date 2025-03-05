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

}
