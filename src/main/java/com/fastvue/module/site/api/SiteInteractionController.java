package com.fastvue.module.site.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.module.site.service.SiteInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Site interactions with explicit public-read/authenticated-write boundaries. */
@Tag(name = "站点交互")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SiteInteractionController {

    private final SiteInteractionService service;

    @Operation(summary = "公开读取博客评论")
    @GetMapping("/public/blog/{articleId}/comments")
    public ApiResponse<List<BlogCommentVO>> comments(@PathVariable Long articleId) {
        return ApiResponse.success(service.comments(articleId));
    }

    @Operation(summary = "登录用户发表评论")
    @PostMapping("/blog/{articleId}/comments")
    public ApiResponse<BlogCommentVO> createComment(
            @PathVariable Long articleId,
            @Valid @RequestBody CreateBlogCommentRequest request) {
        return ApiResponse.success(service.createComment(articleId, request));
    }

    @Operation(summary = "登录用户创建支付订单")
    @PostMapping("/payments/checkout")
    public ApiResponse<PaymentOrderVO> checkout(@Valid @RequestBody CreateCheckoutRequest request) {
        return ApiResponse.success(service.checkout(request));
    }
}
