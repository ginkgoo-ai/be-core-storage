package com.ginkgooai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveSeparatelyRequest {
    private String thirdPartUrl;

    private String cookie;
}
