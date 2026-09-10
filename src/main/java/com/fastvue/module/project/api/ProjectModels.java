package com.fastvue.module.project.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ProjectModels {
    private ProjectModels() {}

    public record CreateRequest(@NotBlank @Size(max=160) String name,
            @NotBlank @Size(max=64) String code, @Size(max=4000) String description,
            LocalDate startDate, LocalDate endDate) {}

    public record UpdateRequest(@Size(max=160) String name, @Size(max=4000) String description,
            LocalDate startDate, LocalDate endDate, @NotNull Integer version) {}

    public record ProjectVO(Long id, String name, String code, String description, Long ownerId,
            String status, LocalDate startDate, LocalDate endDate, Integer version,
            List<Long> memberIds, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    public record ActivityVO(Long id, Long projectId, Long actorId, String action,
                             String detail, OffsetDateTime createdAt) {}
}
