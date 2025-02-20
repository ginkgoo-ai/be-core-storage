package com.ginkgooai.model.request;

import lombok.*;

/**
 * @author: david
 * @date: 15:12 2025/2/20
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {
    @NonNull
    private String originalUrl;
}
