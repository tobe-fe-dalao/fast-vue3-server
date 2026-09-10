package com.fastvue.module.tenant.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.module.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasRole('admin') and hasAuthority('tenant:list')")
    public ApiResponse<List<TenantVO>> list() {
        return ApiResponse.success(tenantService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('admin') and hasAuthority('tenant:list')")
    public ApiResponse<TenantVO> create(@Valid @RequestBody TenantModels.CreateRequest request) {
        return ApiResponse.success(tenantService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin') and hasAuthority('tenant:list')")
    public ApiResponse<TenantVO> update(@PathVariable Long id,
            @Valid @RequestBody TenantModels.UpdateRequest request) {
        return ApiResponse.success(tenantService.update(id, request));
    }
}
