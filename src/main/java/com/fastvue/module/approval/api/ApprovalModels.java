package com.fastvue.module.approval.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public final class ApprovalModels {
    private ApprovalModels() {}

    public record CreateRequest(@NotBlank @Size(max=64) String type, @NotBlank @Size(max=255) String title,
            @Size(max=128) String businessKey, Long departmentId, @Size(max=10000) String payload) {}

    public record ActionRequest(@Size(max=1000) String comment) {}

    public record StepVO(Long id, Integer stepOrder, String approverType, Long approverId,
                         String status, OffsetDateTime actedAt) {}

    public record ApprovalVO(Long id, String type, String title, String businessKey, Long applicantId,
            Long departmentId, String status, Integer currentStep, String payload, Integer version,
            List<StepVO> steps, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
}
