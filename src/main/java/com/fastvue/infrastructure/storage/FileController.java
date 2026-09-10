package com.fastvue.infrastructure.storage;

import com.fastvue.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {
    private final FileStorage storage;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResponse<FileStorage.StoredFile> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(storage.upload(file));
    }

    @GetMapping("/{tenantId}/{filename}")
    public ResponseEntity<Resource> download(@PathVariable Long tenantId, @PathVariable String filename) {
        Resource resource = storage.load(tenantId + "/" + filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }

    @DeleteMapping("/{tenantId}/{filename}")
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResponse<Void> delete(@PathVariable Long tenantId, @PathVariable String filename) {
        storage.delete(tenantId + "/" + filename);
        return ApiResponse.success();
    }
}
