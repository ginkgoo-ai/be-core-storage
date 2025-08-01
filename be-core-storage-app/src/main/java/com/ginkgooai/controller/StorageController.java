package com.ginkgooai.controller;

import com.ginkgooai.domain.CloudFile;
import com.ginkgooai.dto.CloudFileResponse;
import com.ginkgooai.dto.PDFHighlightRequest;
import com.ginkgooai.dto.SaveSeparatelyRequest;
import com.ginkgooai.model.request.PresignedUrlRequest;
import com.ginkgooai.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * @author: david
 * @date: 20:48 2025/2/9
 */
@Slf4j
@Tag(name = "File Storage", description = "File storage management")  // Add category tag
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @Value("${MAX_CACHE_AGE:2592000}")
    private String MAX_CACHE_AGE;

    @Value(("${EXPIRES_TIME:2592000000}"))
    private String EXPIRES_TIME;

    /**
     * Upload file.
     *
     * @return Cloud file.
     */
    @Operation(
            summary = "Upload file",
            description = "Upload the file to the storage service and return a unique identifier for the file",
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
    public ResponseEntity<CloudFileResponse> upload(
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
	 * Generates a pre-signed URL for the specified file by its ID.
	 * @param fileId The ID of the file for which to generate the pre-signed URL.
	 * @return A pre-signed URL that can be used to access the file.
	 * @throws FileNotFoundException if the file is not found.
	 */
	@Operation(summary = "Generate pre-signed URL by file ID",
			description = "Generate a time-limited pre-signed URL for temporary access to private files using file ID",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully generated pre-signed URL",
							content = @Content(schema = @Schema(implementation = URL.class))),
					@ApiResponse(responseCode = "404", description = "File not found with the provided ID") })
	@GetMapping("/{fileId}/presigned-url-by-id")
	public ResponseEntity<URL> generatePresignedUrlByFileId(@Parameter(description = "File ID (unique identifier)",
			required = true, example = "123e4567-e89b-12d3-a456-426614174000",
			schema = @Schema(type = "string")) @PathVariable String fileId) throws FileNotFoundException {
		return ResponseEntity.ok(storageService.generatePresignedUrlByFileId(fileId));
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

    @Operation(
            summary = "Get file details by ID",
            description = "Retrieves detailed information about a file using its unique identifier",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "File details retrieved successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CloudFile.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "File not found with the provided ID"
                    )
            }
    )
    @GetMapping("")
    public ResponseEntity<List<CloudFileResponse>> getFileDetails(
            @Parameter(
                    description = "List of file IDs to retrieve (max 500)",
                    required = true
            )
            @Valid @RequestParam @Size(min = 1, max = 100) List<String> fileIds) {
        List<CloudFileResponse> fileDetails = storageService.getFileDetails(fileIds);
        return ResponseEntity.ok(fileDetails);
    }

    @GetMapping("/{fileId}/private-url")
    public String getPrivateUrl(@PathVariable String fileId) throws FileNotFoundException {
        return storageService.getPrivateUrl(fileId);
    }

    /**
     * This method is used to download a file and stream its content directly to the client as a downloadable attachment.
     * It utilizes the Ant-style path pattern "/blob/**" to match requests, providing flexibility in handling various file paths.
     *
     * @param request  The HTTP servlet request object, containing information about the client's request.
     * @param response The HTTP servlet response object, used to write the file content to the client.
     * @throws IOException If an I/O error occurs during file streaming or if the file is not found.
     */
    @Operation(
            summary = "Download file",
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
    @GetMapping("/blob/**")
    @ResponseBody
    public void blob(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setHeader("Cache-Control", "public, max-age=" + MAX_CACHE_AGE);
        response.setHeader("Pragma", "cache");
        response.setDateHeader("Expires", System.currentTimeMillis() + Long.parseLong(EXPIRES_TIME)); 
        
        storageService.downloadBlob(request, response);
    }


    @PostMapping("/third-part")
    public ResponseEntity<CloudFileResponse> saveSeparately(
            @RequestBody SaveSeparatelyRequest saveSeparatelyRequest) {
        return ResponseEntity.status(201).body(storageService.saveSeparately(saveSeparatelyRequest));
    }

    /**
     * Process PDF highlighting and return the highlighted PDF as a blob stream.
     * 
     * @param request The PDF highlight request containing file ID and highlight data
     * @param response The HTTP response to stream the highlighted PDF blob to
     * @throws IOException If an error occurs during PDF processing
     */
    @Operation(
            summary = "Highlight PDF and return as blob",
            description = "Download PDF from storage, apply highlighting based on question-answer pairs, and return the highlighted PDF as a blob stream",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "PDF successfully highlighted and streamed to the client",
                            content = @Content(mediaType = "application/pdf")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request - file is not a PDF or invalid highlight data"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "PDF file not found with the provided ID"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error during PDF processing"
                    )
            }
    )
    @PostMapping("/pdf-highlight")
    @ResponseBody
    public void highlightPDF(
            @Parameter(
                    description = "PDF highlight request data",
                    required = true
            )
            @RequestBody PDFHighlightRequest request, 
            HttpServletResponse response) throws IOException {
        
        storageService.processPDFHighlight(request, response);
    }
}
