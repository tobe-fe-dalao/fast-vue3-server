package com.fastvue.module.organization.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OrganizationModels {
    private OrganizationModels() {}

    public record OrganizationVO(Long id, Long tenantId, String name, String code, String status) {}
    public record UpdateRequest(@Size(max = 128) String name,
                                @Pattern(regexp = "active|disabled") String status) {}
}
