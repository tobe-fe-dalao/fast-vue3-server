package com.fastvue.module.task.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class TaskModels {
    private TaskModels() {}

    public record CreateRequest(@NotBlank @Size(max=255) String title, @Size(max=8000) String description,
            Long assigneeId, @Pattern(regexp="LOW|MEDIUM|HIGH|URGENT") String priority, LocalDate dueDate) {}

    public record UpdateRequest(@Size(max=255) String title, @Size(max=8000) String description,
            Long assigneeId, @Pattern(regexp="LOW|MEDIUM|HIGH|URGENT") String priority,
            @Pattern(regexp="TODO|IN_PROGRESS|BLOCKED|DONE|CANCELLED") String status,
            LocalDate dueDate, @NotNull Integer version) {}

    public record CommentRequest(@NotBlank @Size(max=2000) String content) {}

    public record TaskVO(Long id, Long projectId, String title, String description, Long assigneeId,
            Long reporterId, String priority, String status, LocalDate dueDate, Integer version,
            OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    public record CommentVO(Long id, Long taskId, Long authorId, String content, OffsetDateTime createdAt) {}

    public record ActivityVO(Long id, Long taskId, Long actorId, String action, String fieldName,
            String oldValue, String newValue, OffsetDateTime createdAt) {}
}
