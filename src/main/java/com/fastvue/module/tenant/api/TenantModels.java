package com.fastvue.module.tenant.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public final class TenantModels {
    private TenantModels() {}

    public record CreateRequest(
            @NotBlank @Size(max = 128) String name,
            @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]{1,62}[a-z0-9]") String code,
            @NotBlank @Size(max = 32) String plan,
            OffsetDateTime expiredAt,
            @NotBlank @Size(max = 64) String adminUsername,
            @NotBlank @Size(min = 10, max = 100) String adminPassword,
            @NotBlank @Email @Size(max = 128) String adminEmail) {}

    public record UpdateRequest(
            @Size(max = 128) String name,
            @Pattern(regexp = "active|disabled") String status,
            @Size(max = 32) String plan,
            OffsetDateTime expiredAt) {}
}
