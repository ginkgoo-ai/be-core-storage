package com.ginkgooai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.alibaba.fastjson.JSONArray;

/**
 * PDF highlight request DTO
 * 
 * @author: david
 * @date: ${DATE}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "PDF highlight request data transfer object")
public class PDFHighlightRequest {

    @Schema(description = "File ID of the PDF document to be highlighted", required = true, example = "abc123-def456-789ghi")
    private String fileId;

    @Schema(description = "Highlight data as FastJSON JSONArray containing question-answer pairs", 
            required = true,
            example = "[{\"question\": \"APPLICANT NAME:\", \"answer\": \"david wang\"}, {\"question\": \"PASSPORT NUMBER:\", \"answer\": \"E12345678\"}]")
    private JSONArray highlightData;
} 