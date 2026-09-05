package com.fastvue.module.role;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.ApiResponse;
import com.fastvue.common.PageResponse;
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

/**
 * 角色管理接口。
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping
    @PreAuthorize("hasAuthority('role:list')")
    public ApiResponse<PageResponse<RoleVO>> page(@Valid RoleQueryRequest query) {
        Page<RoleVO> page = roleService.page(query);
        return ApiResponse.success(PageResponse.of(page));
    }

    @Operation(summary = "查询角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:list')")
    public ApiResponse<RoleVO> getById(@PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public ApiResponse<RoleVO> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.success(roleService.create(request));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public ApiResponse<RoleVO> update(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.success(roleService.update(id, request));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }
}
