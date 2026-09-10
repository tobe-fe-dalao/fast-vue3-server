package com.fastvue.module.organization.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.module.organization.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('department:list')")
    public ApiResponse<List<DepartmentModels.DepartmentVO>> tree() {
        return ApiResponse.success(departmentService.tree());
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAuthority('department:list')")
    public ApiResponse<List<DepartmentModels.MemberVO>> members(@PathVariable Long id) {
        return ApiResponse.success(departmentService.members(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('department:manage')")
    public ApiResponse<DepartmentModels.DepartmentVO> create(@Valid @RequestBody DepartmentModels.SaveRequest request) {
        return ApiResponse.success(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('department:manage')")
    public ApiResponse<DepartmentModels.DepartmentVO> update(@PathVariable Long id,
            @Valid @RequestBody DepartmentModels.SaveRequest request) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    @PutMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('department:manage')")
    public ApiResponse<Void> assign(@PathVariable Long id, @PathVariable Long userId) {
        departmentService.assignMember(id, userId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('department:manage')")
    public ApiResponse<Void> remove(@PathVariable Long id, @PathVariable Long userId) {
        departmentService.removeMember(id, userId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('department:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success();
    }
}
