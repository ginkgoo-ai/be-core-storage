package com.ginkgooai.service;

import com.ginkgooai.domain.CloudFile;
import com.ginkgooai.model.request.PresignedUrlRequest;
import com.ginkgooai.model.response.UploadObjectResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * @author: david
 * @date: 20:49 2025/2/9
 */


public interface StorageService {

    Long MAX_FILE_SIZE = 100 * 1024 * 1024L;

    Set<String> ALLOWED_FILE_TYPES = Set.of("image/jpeg", "image/png", "image/gif");

    static String generateUniqueFileName(String originalFileName) {
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf(".");
        if (dotIndex >= 0) {
            fileExtension = originalFileName.substring(dotIndex);
        }

        return UUID.randomUUID().toString().replace("-", "") + fileExtension;
    }

    CloudFile uploadFile(MultipartFile file);

    URL generatePresignedUrl(String fileName);

    String generateSignedUrl(String fileId) throws FileNotFoundException, URISyntaxException;

    URL generatePresignedUrlByOrigninalUrl(PresignedUrlRequest request);

    void downloadFile(String originUrl, OutputStream out) throws FileNotFoundException;

    CloudFile uploadThumbnailToStorage(Path thumbnailFile, String thumbnailName);
}
