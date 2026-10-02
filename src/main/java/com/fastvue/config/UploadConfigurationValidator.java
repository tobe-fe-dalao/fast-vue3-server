package com.fastvue.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UploadConfigurationValidator {
    @Bean
    InitializingBean validateUploadLimits(
            StorageProperties storage, MultipartProperties multipart) {
        return () -> {
            if (storage.maxSizeBytes() != multipart.getMaxFileSize().toBytes()) {
                throw new IllegalStateException(
                        "Storage and servlet file limits must match; configure FILE_MAX_SIZE_BYTES");
            }
            long requestBytes = multipart.getMaxRequestSize().toBytes();
            if (requestBytes < storage.maxSizeBytes()
                    || requestBytes - storage.maxSizeBytes() < 65536) {
                throw new IllegalStateException(
                        "FILE_MAX_REQUEST_SIZE_BYTES must leave at least 64 KiB for multipart overhead");
            }
        };
    }
}
