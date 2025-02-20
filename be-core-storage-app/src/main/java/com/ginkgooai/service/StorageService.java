package com.ginkgooai.service;

import com.ginkgooai.model.request.PresignedUrlRequest;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Set;

/**
 * @author: david
 * @date: 20:49 2025/2/9
 */


public interface StorageService {

    Long MAX_FILE_SIZE = 100 * 1024 * 1024L;
    Set<String> ALLOWED_FILE_TYPES = Set.of("image/jpeg", "image/png", "image/gif");



    String uploadFile(MultipartFile file);

    URL generatePresignedUrl(String fileName);

    URL generatePresignedUrlByOrigninalUrl(PresignedUrlRequest request);
}
