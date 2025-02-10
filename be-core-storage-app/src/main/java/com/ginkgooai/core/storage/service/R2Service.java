package com.ginkgooai.core.storage.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
            return file.getOriginalFilename();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Error uploading file to R2", e);
        }
    }


    // 获取文件的预签名 URL
    @Override
    public URL generatePresignedUrl(String fileName) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + Long.parseLong(expirationTime));  // 1 小时有效
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileName)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            return s3Client.generatePresignedUrl(request);
        } catch (Exception e) {
            throw new RuntimeException("Error generating presigned URL", e);
        }
    }
}
