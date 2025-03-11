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
import com.ginkgooai.model.request.PresignedUrlRequest;
import com.ginkgooai.repository.CloudFileRepository;
import com.ginkgooai.utils.VideoMetadataExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    @Value(("${SLATE_URI:}"))
    private String domain;

    private final AmazonS3 s3Client;

    private final CloudFileRepository cloudFileRepository;

    // 上传文件
    @Override
    public CloudFile uploadFile(MultipartFile file) {

        try {
            String storageName = generateUniqueFileName(Objects.requireNonNull(file.getOriginalFilename()));
            String storagePath = String.format("%s/%s/%s", endpoints, bucketName, storageName);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());


            CloudFile cloudFile = CloudFile.fromFile(file, bucketName, storageName, storagePath , isVideoFile(file.getContentType()) ? extractMetadata(file) : null);
            s3Client.putObject(new PutObjectRequest(bucketName, storageName, file.getInputStream(), metadata));
            log.info("uploadFile path : {}", storagePath);

            // 保存元数据
            CloudFile cloudFile = new CloudFile();
            cloudFile.setOriginalName(file.getOriginalFilename());
            cloudFile.setStorageName(storageName);
            cloudFile.setFileType(file.getContentType());
            cloudFile.setFileSize(file.getSize());
            cloudFile.setStoragePath(storagePath);
            // todo
//            cloudFile.setUploaderId(uploaderId);
            cloudFile.setBucketName(bucketName);

            return CloudFileResponse.fromCloudFile(cloudFileRepository.save(cloudFile), getPrivateUrlByPath(cloudFile.getStoragePath()));

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

        return file.getStoragePath().replace(endpoints + "/" + bucketName, domain + "/api/storage/files/blob");
    }



    private String getPrivateUrlByPath(String storagePath) throws FileNotFoundException {

        return storagePath.replace(endpoints + "/" + bucketName, domain + "/api/storage/files/blob");
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

        GetObjectRequest getObjectRequest = new GetObjectRequest(
                bucketName,
                fileName
        );

        try (S3Object s3Object = s3Client.getObject(getObjectRequest);
             S3ObjectInputStream inputStream = s3Object.getObjectContent()) {

            // 4. 将内容写入输出流
            IOUtils.copyLarge(inputStream, response.getOutputStream());
            response.getOutputStream().flush();

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
            tempThumbnailFile = VideoMetadataExtractor.generateThumbnailAtPosition(tempFile.toFile() , videoDuration / 2);

            String thumbnailName = VideoMetadataExtractor.generateUniqueThumbnailName(Objects.requireNonNull(file.getOriginalFilename()));

            return uploadThumbnailToStorage(tempThumbnailFile, thumbnailName);

        } catch (Exception e) {
            log.error("Failed to generate thumbnail.", e);
            return null;
        } finally {
            VideoMetadataExtractor.deleteTempFile(tempThumbnailFile);

        }
    }
}
