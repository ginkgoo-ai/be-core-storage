package com.ginkgooai.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoMetadata {
    private String thumbnailFileId;
    private String thumbnailUrl;
    private Long duration;
    private String resolution;
    private Long size;
}