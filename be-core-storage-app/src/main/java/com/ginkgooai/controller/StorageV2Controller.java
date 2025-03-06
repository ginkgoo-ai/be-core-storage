package com.ginkgooai.controller;

import com.ginkgooai.dto.CloudFilesResponse;
import com.ginkgooai.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Tag(name = "File Storage", description = "File storage management")  // Add category tag
@RestController
@RequestMapping("/v2/files")
@RequiredArgsConstructor
public class StorageV2Controller {
    private final StorageService storageService;


    /**
     * Upload files.
     *
     * @return Cloud files.
     */
    @Operation(
            summary = "Upload files",
            description = "Upload files to the storage service and return a unique identifier for the file",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "File upload successful, returns file preview URL"
                    ),

                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CloudFilesResponse> uploadFiles(
            @RequestPart("files") MultipartFile[] files) {
        return ResponseEntity.status(201).body(storageService.uploadFiles(files));
    }

}
