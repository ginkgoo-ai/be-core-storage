package com.ginkgooai.dto;

import com.ginkgooai.domain.CloudFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Cloud file data transfer object")
public class CloudFilesResponse {

    List<CloudFileResponse> cloudFiles;

}
