package com.ginkgooai.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.ginkgooai.core.common.exception.GinkgooRunTimeException;
import com.ginkgooai.core.common.exception.enums.CustomErrorEnum;
import com.ginkgooai.model.request.PresignedUrlRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Date;

/**
 * @author: david
 * @date: 23:13 2025/2/8
 */

@Service
@Slf4j
public class R2Service implements StorageService {

    @Value("${CLOUDFLARE_R2_BUCKET_NAME}")
    private String bucketName;

    @Value("${EXPIRATION_TIME:}")
    private String expirationTime;

    private final AmazonS3 s3Client;

    public R2Service(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    // 上传文件
    @Override
    public String uploadFile(MultipartFile file) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            s3Client.putObject(new PutObjectRequest(bucketName, file.getOriginalFilename(), file.getInputStream(), metadata));
            return generatePresignedUrl(file.getOriginalFilename()).getFile();
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

    private String extractFilePathFromUrl(String fileUrl) {
        // 从 URL 中提取文件路径（这里假设 URL 中的路径部分为文件路径）
        String[] urlParts = fileUrl.split("/", 4);
        if (urlParts.length > 3) {
            return urlParts[3];  // 返回文件路径
        }
        throw new IllegalArgumentException("Invalid file URL");
    }
}
