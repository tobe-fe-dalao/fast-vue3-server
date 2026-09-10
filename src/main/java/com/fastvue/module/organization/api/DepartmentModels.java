package com.fastvue.module.organization.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class DepartmentModels {
    private DepartmentModels() {}

    public record SaveRequest(
            Long parentId,
            @NotBlank @Size(max = 128) String name,
            @NotBlank @Size(max = 64) String code,
            Long leaderId,
            Integer sort,
            @Pattern(regexp = "active|disabled") String status) {
    }

    public record DepartmentVO(Long id, Long parentId, String name, String code, Long leaderId,
                               Integer sort, String status, List<DepartmentVO> children) {
    }

    public record MemberVO(Long id, String username, String nickname, String status) {
    }
}
