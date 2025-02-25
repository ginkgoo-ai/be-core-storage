package com.ginkgooai.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.ginkgooai.core.common.exception.GinkgooRunTimeException;
import com.ginkgooai.core.common.exception.enums.CustomErrorEnum;
import com.ginkgooai.domain.CloudFile;
import com.ginkgooai.model.request.PresignedUrlRequest;
import com.ginkgooai.repository.CloudFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

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

    private final AmazonS3 s3Client;

    private final CloudFileRepository cloudFileRepository;

    // 上传文件
    @Override
    public CloudFile uploadFile(MultipartFile file) {

        try {
            String storageName = generateUniqueFileName(file.getOriginalFilename());
            String storagePath = String.format("%s/%s/%s", endpoints, bucketName, storageName);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
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
            return cloudFileRepository.save(cloudFile);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new GinkgooRunTimeException(CustomErrorEnum.UPLOADING_FILE_EXCEPTION);
        }
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

    // 获取文件的预签名 URL
    @Override
    public URL generatePresignedUrlByOrigninalUrl(PresignedUrlRequest request) {
        try {
            String filePath = extractFilePathFromUrl(request.getOriginalUrl());

            Date expiration = new Date(System.currentTimeMillis() + Long.parseLong(expirationTime));
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, filePath)
                    .withMethod(HttpMethod.GET)  // 使用 GET 方法读取文件
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
        // 从 URL 中提取文件路径（这里假设 URL 中的路径部分为文件路径）
        String[] urlParts = fileUrl.split("/", 4);
        if (urlParts.length > 3) {
            return urlParts[3];  // 返回文件路径
        }
        throw new IllegalArgumentException("Invalid file URL");
    }

    private String generateUniqueFileName(String originalName) {
        String extension = originalName.substring(originalName.lastIndexOf("."));
        return UUID.randomUUID() + extension;
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

            // 4. 将内容写入输出流
            IOUtils.copyLarge(inputStream, out);
            out.flush();

        } catch (IOException e) {
            throw new GinkgooRunTimeException(CustomErrorEnum.OBTAINING_DOWNLOAD_EXCEPTION);
        }
    }
}
