package com.fastvue.infrastructure.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    StoredFile upload(MultipartFile file);
    void delete(String key);
    Resource load(String key);
    String getUrl(String key);

    record StoredFile(String key, String originalName, String contentType, long size, String url) {}
}
