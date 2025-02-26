package com.ginkgooai.controller;

import com.ginkgooai.domain.CloudFile;
import com.ginkgooai.model.request.PresignedUrlRequest;
import com.ginkgooai.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CloudFile> upload(
            @RequestPart(name = "file") MultipartFile file) {
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


    /**
     * Downloads a file by its unique identifier and streams it directly to the HTTP response output.
     *
     * <p>This method retrieves the file content from the storage service using the provided {@code fileId}
     * and writes the binary data to the response output stream. The client will receive the file as a
     * downloadable attachment with proper content headers.</p>
     *
     * @param fileId   The unique identifier of the file to download (e.g., UUID or storage key).
     * @param response The HTTP servlet response object to write the file content to.
     * @throws IOException If an I/O error occurs during file streaming or if the file is not found.
     */
    @Operation(
            summary = "Download file by ID",
            description = "Streams the file content directly to the client as a downloadable attachment.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "File successfully streamed to the client",
                            content = @Content(mediaType = "application/octet-stream")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "File not found with the provided ID"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error during file streaming"
                    )
            }
    )
    @PostMapping("/{fileId}")
    public void downloadFile(@PathVariable String fileId, HttpServletResponse response) throws IOException {
        storageService.downloadFile(fileId, response.getOutputStream());
    }

}
