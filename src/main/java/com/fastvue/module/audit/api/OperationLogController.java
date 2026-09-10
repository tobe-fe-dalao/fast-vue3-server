package com.fastvue.module.audit.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.module.audit.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit/operations")
@RequiredArgsConstructor
public class OperationLogController {
    private final OperationLogService service;

    @GetMapping
    @PreAuthorize("hasAuthority('audit:view')")
    public ApiResponse<List<OperationLogVO>> list() { return ApiResponse.success(service.list()); }
}
