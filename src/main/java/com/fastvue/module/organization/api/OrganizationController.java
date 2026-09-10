package com.fastvue.module.organization.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.module.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService service;

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('department:list')")
    public ApiResponse<OrganizationModels.OrganizationVO> current() {
        return ApiResponse.success(service.current());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('department:manage')")
    public ApiResponse<OrganizationModels.OrganizationVO> update(@PathVariable Long id,
            @Valid @RequestBody OrganizationModels.UpdateRequest request) {
        return ApiResponse.success(service.update(id, request));
    }
}
