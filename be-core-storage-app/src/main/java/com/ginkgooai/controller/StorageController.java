package com.ginkgooai.controller;

import com.ginkgooai.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

/**
 * @author: david
 * @date: 20:48 2025/2/9
 */

@RestController
@RequestMapping("/storage/objects")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    /**
     * upload  file.
     *
     * @return A pre-signed URL that can be used to access the file.
     */
    @PostMapping
    public String upload(@RequestPart MultipartFile file) {
        return storageService.uploadFile(file);
    }


    /**
     * Generates a pre-signed URL for the specified file.
     *
     * @param fileName The name of the file for which to generate the pre-signed URL.
     * @return A pre-signed URL that can be used to access the file.
     */
    @GetMapping("/{fileName}")
    public URL generatePresignedUrl(@PathVariable String fileName){
        return storageService.generatePresignedUrl(fileName);
    }

}
