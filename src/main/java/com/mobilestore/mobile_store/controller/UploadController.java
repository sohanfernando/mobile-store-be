package com.mobilestore.mobile_store.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mobilestore.mobile_store.dto.response.ApiResponseDto;
import com.mobilestore.mobile_store.storage.FileStorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file, "products");
        return ResponseEntity.ok(ApiResponseDto.success("File uploaded successfully", Map.of("url", url)));
    }
}
