package com.ginkgooai.core.storage.service;

import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

/**
 * @author: david
 * @date: 20:49 2025/2/9
 */


public interface StorageService {
    String uploadFile(MultipartFile file);

    URL generatePresignedUrl(String fileName);
}
