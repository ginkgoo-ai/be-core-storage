package com.ginkgooai.api;

import com.ginkgooai.core.common.config.CustomErrorDecoder;
import com.ginkgooai.core.common.config.FeignMultipartConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/storage/objects")
    String upload(@RequestPart MultipartFile file);

    @GetMapping("/{fileName}")
    URL generatePresignedUrl(@PathVariable String fileName);
}
