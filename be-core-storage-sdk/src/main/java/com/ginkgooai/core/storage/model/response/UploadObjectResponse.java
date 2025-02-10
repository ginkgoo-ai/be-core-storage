package com.ginkgooai.core.storage.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: david
 * @date: 21:06 2025/2/9
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadObjectResponse {

    private String url;

    private String downloadUrl;

}
