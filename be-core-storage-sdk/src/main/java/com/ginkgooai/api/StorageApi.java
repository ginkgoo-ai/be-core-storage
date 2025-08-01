package com.ginkgooai.api;

import com.ginkgooai.core.common.config.CustomErrorDecoder;
import com.ginkgooai.core.common.config.FeignMultipartConfig;
import com.ginkgooai.model.request.PresignedUrlRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.net.URL;

/**
 * @author: david
 * @date: 17:57 2025/2/10
 */
@FeignClient(name = "core-storage",
        configuration = {CustomErrorDecoder.class, FeignMultipartConfig.class},
        url = "${STORAGE_API_URL:}")
@Component
public interface StorageApi {

    @PostMapping("/files")
    ResponseEntity<String> upload(@RequestPart MultipartFile file);

    @GetMapping("/files/{fileName}/presigned-url")
    ResponseEntity<URL> generatePresignedUrl(@PathVariable String fileName);

	@GetMapping("/files/fileId}/presigned-url-by-id")
	ResponseEntity<URL> generatePresignedUrlByFileId(@PathVariable String fileId) throws FileNotFoundException;

    @PostMapping("/files/presigned-url")
    ResponseEntity<URL> generatePresignedUrlByOriginalUrl(@RequestBody PresignedUrlRequest request);

    @PostMapping("/{fileId}")
    void downloadFile(@PathVariable String fileId, HttpServletResponse response);

    @GetMapping("/{fileId}/private-url")
    String getPrivateUrl(@PathVariable String fileId) throws FileNotFoundException;
}
