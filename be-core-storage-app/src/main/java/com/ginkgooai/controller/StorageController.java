package com.ginkgooai.controller;

import com.ginkgooai.model.request.PresignedUrlRequest;
import com.ginkgooai.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

/**
 * @author: david
 * @date: 20:48 2025/2/9
 */
@Tag(name = "File Storage", description = "File storage management")  // Add category tag
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    /**
     * Upload file.
     *
     * @return A pre-signed URL that can be used to access the file.
     */
    @Operation(
            summary = "Upload file",
            description = "Upload the file to the storage service and return a unique identifier for the file",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "File upload successful, returns file preview URL"
                    ),

                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<String> upload(
            @Parameter(
                    description = "The file to be uploaded",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data"))
            @RequestPart MultipartFile file) {
        return ResponseEntity.status(201).body(storageService.uploadFile(file));
    }


    /**
     * Generates a pre-signed URL for the specified file.
     *
     * @param fileName The name of the file for which to generate the pre-signed URL.
     * @return A pre-signed URL that can be used to access the file.
     */
    @Operation(
            summary = "Generate pre-signed URL",
            description = "Generate a time-limited pre-signed URL for temporary access to private files",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully generated pre-signed URL",
                            content = @Content(schema = @Schema(implementation = URL.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "File not found"
                    )
            }
    )

    @GetMapping("/{fileName}/presigned-url")
    public ResponseEntity<URL> generatePresignedUrlByFileName(
            @Parameter(
                    description = "File name (unique identifier)",
                    required = true,
                    example = "file-12345",
                    schema = @Schema(type = "string")
            )
            @PathVariable String fileName){
        return ResponseEntity.ok(storageService.generatePresignedUrl(fileName));
    }

    /**
     * Generates a pre-signed URL for the specified file by its original URL.
     *
     * This method takes a {@link PresignedUrlRequest} object containing the original file URL
     * and uses the {@link StorageService} to generate a pre-signed URL for temporary access to the file.
     *
     * @param request A {@link PresignedUrlRequest} object containing the original file URL.
     * @return A pre-signed URL that can be used to access the file.
     */
    @Operation(
            summary = "Generate pre-signed URL",
            description = "Generate a time-limited pre-signed URL for temporary access to private files",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully generated pre-signed URL",
                            content = @Content(schema = @Schema(implementation = URL.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "File not found"
                    )
            }
    )
    @PostMapping("/presigned-url")
    public ResponseEntity<URL> generatePresignedUrlByOriginalUrl(
            @Parameter(
                    description = "File url (unique identifier)",
                    required = true,
                    example = "url",
                    schema = @Schema(type = "string")
            )
            @RequestBody PresignedUrlRequest request){
        return ResponseEntity.ok(storageService.generatePresignedUrlByOrigninalUrl(request));
    }

}
