package com.fastvue.module.notification.api;

import java.time.OffsetDateTime;

public record NotificationVO(Long id, String type, String title, String content,
                             Long receiverId, Boolean read, OffsetDateTime createdAt) {}
