package com.ginkgooai.core.storage.controller;

import com.ginkgooai.core.storage.service.StorageService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

/**
 * @author: david
 * @date: 20:48 2025/2/9
 */

@RestController
@RequestMapping("/storage/objects")
public class StorageController {

    @Resource
    private StorageService storageService;

    @PostMapping
    public String upload(@RequestPart MultipartFile file) {
        return storageService.uploadFile(file);
    }

    @GetMapping("/{fileName}")
    public URL generatePresignedUrl(@PathVariable String fileName){
        return storageService.generatePresignedUrl(fileName);
    }
}
