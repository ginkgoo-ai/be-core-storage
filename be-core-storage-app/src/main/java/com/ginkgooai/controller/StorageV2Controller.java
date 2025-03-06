package com.ginkgooai.controller;

import com.ginkgooai.dto.CloudFilesResponse;
import com.ginkgooai.service.StorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@Tag(name = "File Storage", description = "File storage management")  // Add category tag
@RestController
@RequestMapping("/v2/files")
@RequiredArgsConstructor
public class StorageV2Controller {
    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CloudFilesResponse> uploadFiles(
            @RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.status(201).body(storageService.uploadFiles(files));
    }

}
