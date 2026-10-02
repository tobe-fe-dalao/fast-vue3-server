package com.fastvue.module.notification.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.common.response.PageRequest;
import com.fastvue.common.response.PageResponse;
import com.fastvue.module.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    public ApiResponse<List<NotificationVO>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<NotificationVO>> page(
            @Valid @ModelAttribute PageRequest request) {
        return ApiResponse.success(service.page(request));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unread() {
        return ApiResponse.success(Map.of("count", service.unreadCount()));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> read(@PathVariable Long id) {
        service.markRead(id);
        return ApiResponse.success();
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> all() {
        service.markAllRead();
        return ApiResponse.success();
    }
}
