package com.ginkgooai.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.ginkgooai.core.common.exception.GinkgooRunTimeException;
import com.ginkgooai.core.common.exception.enums.CustomErrorEnum;
import com.ginkgooai.domain.CloudFile;
import com.ginkgooai.domain.VideoMetadata;
import com.ginkgooai.dto.CloudFileResponse;
import com.ginkgooai.dto.CloudFilesResponse;
import com.ginkgooai.dto.PDFHighlightRequest;
import com.ginkgooai.dto.SaveSeparatelyRequest;
import com.ginkgooai.model.request.PresignedUrlRequest;
import com.ginkgooai.repository.CloudFileRepository;
import com.ginkgooai.utils.PDFHighlighter;
import com.ginkgooai.utils.VideoMetadataExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static jodd.net.MimeTypes.getMimeType;

/**
 * @author: david
 * @date: 23:13 2025/2/8
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class R2Service implements StorageService {

    @Value("${CLOUDFLARE_R2_BUCKET_NAME}")
    private String bucketName;

    @Value("${EXPIRATION_TIME:}")
    private String expirationTime;

    @Value("${CLOUDFLARE_R2_ENDPOINTS:}")
    private String endpoints;

    @Value(("${AUTH_CLIENT:}"))
    private String domain;

    private final AmazonS3 s3Client;

    private final CloudFileRepository cloudFileRepository;

    // 上传文件
    @Override
    public CloudFileResponse uploadFile(MultipartFile file) {

        try {
            String storageName = generateUniqueFileName(Objects.requireNonNull(file.getOriginalFilename()));
            String storagePath = String.format("%s/%s/%s", endpoints, bucketName, storageName);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());


            CloudFile cloudFile = CloudFile.fromFile(file, bucketName, storageName, storagePath , isVideoFile(file.getContentType()) ? extractMetadata(file) : null);
            s3Client.putObject(new PutObjectRequest(bucketName, storageName, file.getInputStream(), metadata));
            log.info("uploadFile path : {}", storagePath);

            return CloudFileResponse.fromCloudFile(cloudFileRepository.save(cloudFile), getPrivateUrlByPath(cloudFile.getStoragePath()), getPrivateUrlByPath(cloudFile.getVideoThumbnailUrl()));

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new GinkgooRunTimeException(CustomErrorEnum.UPLOADING_FILE_EXCEPTION);
        }
    }


    @Override
    public CloudFilesResponse uploadFiles(MultipartFile[] files) {

        List<CompletableFuture<CloudFileResponse>> futures = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE) {
                //todo 文件过大
            }
            // 异步处理文件上传
            futures.add(CompletableFuture.completedFuture(uploadFile(file)));
        }

        // 收集所有结果
        List<CloudFileResponse> cloudFiles = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return CloudFilesResponse.builder().cloudFiles(cloudFiles).build();

    }

    @Override
    public List<CloudFileResponse> getFileDetails(List<String> fileIds) {
        List<CloudFile> file = cloudFileRepository.findAllById(fileIds);
        
        return file.stream().map(t -> CloudFileResponse.fromCloudFile(t, getPrivateUrlByPath(t.getStoragePath()), getPrivateUrlByPath(t.getVideoThumbnailUrl()))).toList();
    }

    @Override
    public CloudFileResponse saveSeparately(SaveSeparatelyRequest saveSeparatelyRequest) {
        try {
            // Validate request parameters
            if (saveSeparatelyRequest.getThirdPartUrl() == null || saveSeparatelyRequest.getThirdPartUrl().isEmpty()) {
                throw new IllegalArgumentException("Third party URL cannot be null or empty");
            }

            log.info("Starting to download file from third party URL: {}", saveSeparatelyRequest.getThirdPartUrl());

            // Create HTTP connection to download file from third party URL
            URL url = new URL(saveSeparatelyRequest.getThirdPartUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method
            connection.setRequestMethod(ObjectUtils.isEmpty(saveSeparatelyRequest.getMethod()) ? "GET" : saveSeparatelyRequest.getMethod());
            
            // Set cookie if provided
            if (saveSeparatelyRequest.getCookie() != null && !saveSeparatelyRequest.getCookie().isEmpty()) {
                connection.setRequestProperty("Cookie", saveSeparatelyRequest.getCookie());
                log.debug("Set cookie header: {}", saveSeparatelyRequest.getCookie());
            }

            if (saveSeparatelyRequest.getCsrfToken() != null && !saveSeparatelyRequest.getCsrfToken().isEmpty()) {
                connection.setRequestProperty("CsrfToken", saveSeparatelyRequest.getCsrfToken());
                log.debug("Set cookie header: {}", saveSeparatelyRequest.getCsrfToken());
            }
            
            // Set common headers to mimic browser request
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setRequestProperty("Accept", "*/*");
            
            // Set connection timeout
            connection.setConnectTimeout(30000); // 30 seconds
            connection.setReadTimeout(60000);    // 60 seconds
            
            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed to download file from third party URL. Response code: " + responseCode);
            }
            
            // Get content type and length
            String contentType = connection.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream"; // Default content type
            }
            
            long contentLength = connection.getContentLengthLong();
            log.info("File content type: {}, content length: {}", contentType, contentLength);
            
            // Extract filename from URL or generate one
            String extension = getExtensionFromContentType(contentType);
            String originalFileName = "downloaded_file" + extension;

            // Generate unique storage name
            String storageName = generateUniqueFileName(originalFileName);
            String storagePath = String.format("%s/%s/%s", endpoints, bucketName, storageName);
            
            // Create ObjectMetadata for S3 upload
            ObjectMetadata metadata = new ObjectMetadata();
            if (contentLength > 0) {
                metadata.setContentLength(contentLength);
            }
            metadata.setContentType(contentType);
            
            // Download and upload to R2 storage
            try (InputStream inputStream = connection.getInputStream()) {
                // Upload to S3/R2
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, storageName, inputStream, metadata);
                s3Client.putObject(putObjectRequest);
                
                log.info("Successfully uploaded file to R2 storage. Path: {}", storagePath);
                
                // Create CloudFile entity
                CloudFile cloudFile = CloudFile.builder()
                        .originalName(originalFileName)
                        .storageName(storageName)
                        .fileType(contentType)
                        .fileSize(contentLength)
                        .storagePath(storagePath)
                        .bucketName(bucketName)
                        .isDeleted(false)
                        .build();

                
                // Save to database
                CloudFile savedCloudFile = cloudFileRepository.save(cloudFile);
                
                // Return response
                return CloudFileResponse.fromCloudFile(
                    savedCloudFile,
                    getPrivateUrlByPath(savedCloudFile.getStoragePath()),
                    null
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to save file separately from URL: {}", saveSeparatelyRequest.getThirdPartUrl(), e);
            throw new GinkgooRunTimeException(CustomErrorEnum.UPLOADING_FILE_EXCEPTION);
        }
    }

    /**
     * Extract filename from URL
     */
    private String extractFileNameFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String path = url.getPath();
            if (path != null && !path.isEmpty()) {
                String fileName = path.substring(path.lastIndexOf('/') + 1);
                // Remove query parameters if any
                int queryIndex = fileName.indexOf('?');
                if (queryIndex != -1) {
                    fileName = fileName.substring(0, queryIndex);
                }
                return fileName.isEmpty() ? null : fileName;
            }
        } catch (Exception e) {
            log.warn("Failed to extract filename from URL: {}", urlString, e);
        }
        return null;
    }

    /**
     * Get file extension from content type
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        
        // Common content type to extension mappings
        Map<String, String> contentTypeToExtension = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "video/mp4", ".mp4",
            "video/avi", ".avi",
            "video/mov", ".mov",
            "video/wmv", ".wmv",
            "application/pdf", ".pdf",
            "text/plain", ".txt"
        );
        
        return contentTypeToExtension.getOrDefault(contentType.toLowerCase(), "");
    }

    // 获取文件的预签名 URL
    @Override
    public URL generatePresignedUrl(String fileName) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + Long.parseLong(expirationTime));
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileName)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            return s3Client.generatePresignedUrl(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new GinkgooRunTimeException(CustomErrorEnum.OBTAINING_DOWNLOAD_LINK_EXCEPTION);
        }
    }

    @Override
    public URL generatePresignedUrlByOrigninalUrl(PresignedUrlRequest request) {
        try {
            String filePath = extractFilePathFromUrl(request.getOriginalUrl());

            Date expiration = new Date(System.currentTimeMillis() + Long.parseLong(expirationTime));
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, filePath)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            return s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        } catch (Exception e) {
			throw new GinkgooRunTimeException(CustomErrorEnum.OBTAINING_DOWNLOAD_LINK_EXCEPTION);
		}
	}

	@Override
	public URL generatePresignedUrlByFileId(String fileId) throws FileNotFoundException {
		try {
			CloudFile file = cloudFileRepository.findById(fileId)
				.orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

			Date expiration = new Date(System.currentTimeMillis() + Long.parseLong(expirationTime));
			GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, file.getStorageName())
				.withMethod(HttpMethod.GET)
				.withExpiration(expiration);
			return s3Client.generatePresignedUrl(request);
		}
		catch (FileNotFoundException e) {
			log.error("File not found with ID: {}", fileId, e);
			throw e;
		}
		catch (Exception e) {
			log.error("Error generating presigned URL for file ID: {}", fileId, e);
            throw new GinkgooRunTimeException(CustomErrorEnum.OBTAINING_DOWNLOAD_LINK_EXCEPTION);
        }
    }

    @Override
    public String generateSignedUrl(String fileId) throws FileNotFoundException, URISyntaxException {
        CloudFile file = cloudFileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        return generatePresignedUrl(file.getStorageName()).toURI().toString();
    }

    private String extractFilePathFromUrl(String fileUrl) {
        String[] urlParts = fileUrl.split("/", 4);
        if (urlParts.length > 3) {
            return urlParts[3];
        }
        throw new IllegalArgumentException("Invalid file URL");
    }

    private String generateUniqueFileName(String originalName) {
        String extension = originalName.substring(originalName.lastIndexOf("."));
        return UUID.randomUUID() + extension;
    }

    public String getPrivateUrl(String fileId) throws FileNotFoundException {
        CloudFile file = cloudFileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        return file.getStoragePath().replace(endpoints + "/" + bucketName, domain + "/api/storage/v1/files/blob");
    }



    private String getPrivateUrlByPath(String storagePath) {
        if (storagePath == null) {
            return null;
        }

        return storagePath.replace(endpoints + "/" + bucketName, domain + "/api/storage/v1/files/blob");
    }

    @Override
    public void downloadFile(String fileId, OutputStream out) throws FileNotFoundException {
        CloudFile file = cloudFileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        GetObjectRequest request = new GetObjectRequest(
                bucketName,
                file.getStorageName()
        );

        try (S3Object s3Object = s3Client.getObject(request);
             S3ObjectInputStream inputStream = s3Object.getObjectContent()) {

            IOUtils.copyLarge(inputStream, out);
            out.flush();

        } catch (IOException e) {
            throw new GinkgooRunTimeException(CustomErrorEnum.OBTAINING_DOWNLOAD_EXCEPTION);
        }
    }

    @Override
    public void downloadBlob(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.info("Request uri :{}" , requestURI);
        String url = URLDecoder.decode(requestURI, StandardCharsets.UTF_8);
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        // 新增MIME类型检测
        String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        String contentType = getMimeType(fileExtension);
        
        GetObjectRequest getObjectRequest = new GetObjectRequest(
                bucketName,
                fileName
        );

        try (S3Object s3Object = s3Client.getObject(getObjectRequest);
             S3ObjectInputStream inputStream = s3Object.getObjectContent()) {

            // 设置动态响应头
            response.setContentType(contentType);
            response.setHeader("Accept-Ranges", "bytes");

            IOUtils.copyLarge(inputStream, response.getOutputStream());
            response.getOutputStream().flush();

            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new GinkgooRunTimeException(CustomErrorEnum.OBTAINING_DOWNLOAD_EXCEPTION);
        }
    }

    @Override
    public CloudFile uploadThumbnailToStorage(Path thumbnailFile, String thumbnailName) {
        File file = thumbnailFile.toFile();
        String thumbnailPath = String.format("%s/%s/%s", endpoints, bucketName, thumbnailName);

        s3Client.putObject(new PutObjectRequest(
                bucketName,
                thumbnailName,
                file
        ));

        return cloudFileRepository.save(CloudFile.fromFile(file, bucketName, thumbnailName, thumbnailPath));
    }


    private boolean isVideoFile(String contentType) {
        return contentType != null && contentType.startsWith("video/");
    }


    public VideoMetadata extractMetadata(MultipartFile videoFile) throws IOException {
        Path tempFile = Files.createTempFile("video", videoFile.getOriginalFilename());

        try (InputStream inputStream = videoFile.getInputStream();
             OutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            double videoDuration = VideoMetadataExtractor.getVideoDuration(tempFile.toFile());
            String videoResolution = VideoMetadataExtractor.getVideoResolution(tempFile.toFile());

            CloudFile cloudFile = generateThumbnail(videoFile, videoDuration);
            return VideoMetadata.builder()
                    .thumbnailFileId(cloudFile.getId())
                    .thumbnailUrl(cloudFile.getStoragePath())
                    .duration(Math.round(videoDuration * 1000))
                    .resolution(videoResolution)
                    .size(videoFile.getSize())
                    .build();
        }

    }

    public CloudFile generateThumbnail(MultipartFile file, double videoDuration) throws IOException {
        Path tempThumbnailFile = null;
        Path tempFile = Files.createTempFile("video", "temp" + file.getOriginalFilename());
        file.transferTo(tempFile);
        try {
            tempThumbnailFile = VideoMetadataExtractor.generateThumbnailAtPosition(tempFile.toFile() , 1);

            String thumbnailName = VideoMetadataExtractor.generateUniqueThumbnailName(Objects.requireNonNull(file.getOriginalFilename()));

            return uploadThumbnailToStorage(tempThumbnailFile, thumbnailName);

        } catch (Exception e) {
            log.error("Failed to generate thumbnail.", e);
            return null;
        } finally {
            VideoMetadataExtractor.deleteTempFile(tempThumbnailFile);

        }
    }

    @Override
    public void processPDFHighlight(PDFHighlightRequest request, HttpServletResponse response) throws FileNotFoundException, IOException {
        try {
            // Validate request parameters
            if (request.getFileId() == null || request.getFileId().isEmpty()) {
                throw new IllegalArgumentException("File ID cannot be null or empty");
            }

            log.info("Processing PDF highlight for file ID: {}", request.getFileId());

            // Get file information from database
            CloudFile cloudFile = cloudFileRepository.findById(request.getFileId())
                    .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + request.getFileId()));

            // Validate that it's a PDF file
            if (!isPDFFile(cloudFile.getFileType())) {
                throw new IllegalArgumentException("File is not a PDF. File type: " + cloudFile.getFileType());
            }

            // Check if highlight data is empty - if so, return original file
            boolean needsHighlighting = request.getHighlightData() != null && !request.getHighlightData().isEmpty();

            if (!needsHighlighting) {
                // Return original PDF file without highlighting
                log.info("No highlight data provided, returning original PDF for file ID: {}", request.getFileId());
                
                // Set response headers for original PDF
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + 
                    URLEncoder.encode(cloudFile.getOriginalName(), StandardCharsets.UTF_8));

                // Stream original PDF directly from R2 storage
                GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, cloudFile.getStorageName());
                try (S3Object s3Object = s3Client.getObject(getObjectRequest);
                     S3ObjectInputStream inputStream = s3Object.getObjectContent()) {

                    // Set Content-Length if available
                    ObjectMetadata metadata = s3Object.getObjectMetadata();
                    if (metadata.getContentLength() > 0) {
                        response.setContentLengthLong(metadata.getContentLength());
                    }

                    // Stream the original PDF to response
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        response.getOutputStream().write(buffer, 0, bytesRead);
                    }
                    response.getOutputStream().flush();
                }

                log.info("Successfully returned original PDF for file ID: {}", request.getFileId());
                return;
            }

            // Download PDF from R2 storage to a temporary file for highlighting
            Path tempInputFile = Files.createTempFile("pdf_input_", ".pdf");
            Path tempOutputFile = Files.createTempFile("pdf_highlighted_", ".pdf");

            try {
                // Download PDF from R2 to temporary file
                downloadPDFFromR2(cloudFile.getStorageName(), tempInputFile);

                // Process PDF highlighting - convert FastJSON JSONArray to string for PDFHighlighter
                String highlightDataString = request.getHighlightData().toJSONString();
                PDFHighlighter.highlightAnswers(tempInputFile.toFile(), tempOutputFile.toFile(), highlightDataString);

                // Set response headers for PDF blob
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + 
                    URLEncoder.encode(cloudFile.getOriginalName().replace(".pdf", "_highlighted.pdf"), StandardCharsets.UTF_8));
                
                // Get file size for Content-Length header
                long fileSize = Files.size(tempOutputFile);
                response.setContentLengthLong(fileSize);

                // Stream the highlighted PDF to response
                try (InputStream inputStream = Files.newInputStream(tempOutputFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        response.getOutputStream().write(buffer, 0, bytesRead);
                    }
                    response.getOutputStream().flush();
                }

                log.info("Successfully processed PDF highlight for file ID: {}, output size: {} bytes", 
                    request.getFileId(), fileSize);

            } finally {
                // Clean up temporary files
                try {
                    Files.deleteIfExists(tempInputFile);
                    Files.deleteIfExists(tempOutputFile);
                } catch (IOException e) {
                    log.warn("Failed to clean up temporary files", e);
                }
            }

        } catch (FileNotFoundException e) {
            log.error("File not found: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            throw new IOException("Invalid request: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to process PDF highlight for file ID: {}", request.getFileId(), e);
            throw new IOException("Failed to process PDF highlight: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the file is a PDF
     */
    private boolean isPDFFile(String contentType) {
        return contentType != null && (
            contentType.equals("application/pdf") || 
            contentType.toLowerCase().contains("pdf")
        );
    }

    /**
     * Download PDF file from R2 storage to a temporary file
     */
    private void downloadPDFFromR2(String storageName, Path outputPath) throws IOException {
        GetObjectRequest request = new GetObjectRequest(bucketName, storageName);

        try (S3Object s3Object = s3Client.getObject(request);
             S3ObjectInputStream inputStream = s3Object.getObjectContent();
             OutputStream outputStream = Files.newOutputStream(outputPath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            log.debug("Downloaded PDF from R2 storage to temporary file: {}", outputPath);

        } catch (Exception e) {
            log.error("Failed to download PDF from R2 storage: {}", storageName, e);
            throw new IOException("Failed to download PDF from storage: " + e.getMessage(), e);
        }
    }
}
