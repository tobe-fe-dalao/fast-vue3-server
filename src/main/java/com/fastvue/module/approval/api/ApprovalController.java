package com.fastvue.module.approval.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.infrastructure.idempotency.IdempotencyService;
import com.fastvue.module.approval.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalService service;
    private final IdempotencyService idempotency;

    @GetMapping
    public ApiResponse<List<ApprovalModels.ApprovalVO>> list() { return ApiResponse.success(service.list()); }

    @PostMapping
    @PreAuthorize("hasAuthority('approval:create')")
    public ApiResponse<ApprovalModels.ApprovalVO> create(@Valid @RequestBody ApprovalModels.CreateRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('approval:create')")
    public ApiResponse<ApprovalModels.ApprovalVO> submit(@PathVariable Long id,
            @RequestHeader("Idempotency-Key") String key) {
        return ApiResponse.success(idempotency.execute("approval-submit-" + id, key, () -> service.submit(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('approval:action')")
    public ApiResponse<ApprovalModels.ApprovalVO> approve(@PathVariable Long id,
            @Valid @RequestBody ApprovalModels.ActionRequest request) {
        return ApiResponse.success(service.approve(id, request.comment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('approval:action')")
    public ApiResponse<ApprovalModels.ApprovalVO> reject(@PathVariable Long id,
            @Valid @RequestBody ApprovalModels.ActionRequest request) {
        return ApiResponse.success(service.reject(id, request.comment()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('approval:create')")
    public ApiResponse<ApprovalModels.ApprovalVO> cancel(@PathVariable Long id) {
        return ApiResponse.success(service.cancel(id));
    }
}
