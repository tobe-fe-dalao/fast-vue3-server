package com.fastvue.module.task.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.common.response.PageRequest;
import com.fastvue.common.response.PageResponse;
import com.fastvue.module.task.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping("/projects/{projectId}/tasks")
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<List<TaskModels.TaskVO>> list(@PathVariable Long projectId) {
        return ApiResponse.success(taskService.list(projectId));
    }

    @GetMapping("/projects/{projectId}/tasks/page")
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<PageResponse<TaskModels.TaskVO>> page(
            @PathVariable Long projectId, @Valid @ModelAttribute PageRequest request) {
        return ApiResponse.success(taskService.page(projectId, request));
    }

    @PostMapping("/projects/{projectId}/tasks")
    @PreAuthorize("hasAuthority('task:create')")
    public ApiResponse<TaskModels.TaskVO> create(
            @PathVariable Long projectId, @Valid @RequestBody TaskModels.CreateRequest request) {
        return ApiResponse.success(taskService.create(projectId, request));
    }

    @GetMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<TaskModels.TaskVO> get(@PathVariable Long id) {
        return ApiResponse.success(taskService.get(id));
    }

    @PutMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('task:update')")
    public ApiResponse<TaskModels.TaskVO> update(
            @PathVariable Long id, @Valid @RequestBody TaskModels.UpdateRequest request) {
        return ApiResponse.success(taskService.update(id, request));
    }

    @PostMapping("/tasks/{id}/comments")
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<TaskModels.CommentVO> comment(
            @PathVariable Long id, @Valid @RequestBody TaskModels.CommentRequest request) {
        return ApiResponse.success(taskService.comment(id, request));
    }

    @GetMapping("/tasks/{id}/comments")
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<List<TaskModels.CommentVO>> comments(@PathVariable Long id) {
        return ApiResponse.success(taskService.comments(id));
    }

    @GetMapping("/tasks/{id}/comments/page")
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<PageResponse<TaskModels.CommentVO>> commentsPage(
            @PathVariable Long id, @Valid @ModelAttribute PageRequest request) {
        return ApiResponse.success(taskService.commentsPage(id, request));
    }

    @GetMapping("/tasks/{id}/activities/page")
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<PageResponse<TaskModels.ActivityVO>> activitiesPage(
            @PathVariable Long id, @Valid @ModelAttribute PageRequest request) {
        return ApiResponse.success(taskService.activitiesPage(id, request));
    }

    @GetMapping("/tasks/{id}/activities")
    @PreAuthorize("hasAuthority('project:list')")
    public ApiResponse<List<TaskModels.ActivityVO>> activities(@PathVariable Long id) {
        return ApiResponse.success(taskService.activities(id));
    }
}
