package com.fastvue.module.permission.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.module.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理接口。
 */
@Tag(name = "权限管理")
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "查询全部权限")
    @GetMapping
    @PreAuthorize("hasAuthority('role:list')")
    public ApiResponse<List<PermissionVO>> list() {
        return ApiResponse.success(permissionService.list());
    }

    @Operation(summary = "创建权限")
    @PostMapping
    @PreAuthorize("hasRole('admin') and hasAuthority('role:create')")
    public ApiResponse<PermissionVO> create(@Valid @RequestBody PermissionRequest request) {
        return ApiResponse.success(permissionService.create(request));
    }

    @Operation(summary = "更新权限")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin') and hasAuthority('role:update')")
    public ApiResponse<PermissionVO> update(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        return ApiResponse.success(permissionService.update(id, request));
    }

    @Operation(summary = "删除权限")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin') and hasAuthority('role:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.success();
    }
}
