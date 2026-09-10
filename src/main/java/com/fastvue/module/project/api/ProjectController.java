package com.fastvue.module.project.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.infrastructure.idempotency.IdempotencyService;
import com.fastvue.module.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final IdempotencyService idempotencyService;

    @GetMapping
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<List<ProjectModels.ProjectVO>> list() { return ApiResponse.success(projectService.list()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<ProjectModels.ProjectVO> get(@PathVariable Long id) { return ApiResponse.success(projectService.get(id)); }

    @GetMapping("/{id}/activities")
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<List<ProjectModels.ActivityVO>> activities(@PathVariable Long id) {
        return ApiResponse.success(projectService.activities(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('project:create')")
    public ApiResponse<ProjectModels.ProjectVO> create(@RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody ProjectModels.CreateRequest request) {
        return ApiResponse.success(idempotencyService.execute("project-create", key, () -> projectService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('project:update')")
    public ApiResponse<ProjectModels.ProjectVO> update(@PathVariable Long id,
            @Valid @RequestBody ProjectModels.UpdateRequest request) {
        return ApiResponse.success(projectService.update(id, request));
    }

    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('project:update')")
    public ApiResponse<ProjectModels.ProjectVO> archive(@PathVariable Long id, @RequestParam Integer version) {
        return ApiResponse.success(projectService.archive(id, version));
    }

    @PutMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('project:update')")
    public ApiResponse<Void> member(@PathVariable Long id, @PathVariable Long userId) {
        projectService.addMember(id, userId); return ApiResponse.success();
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('project:update')")
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId); return ApiResponse.success();
    }
}
