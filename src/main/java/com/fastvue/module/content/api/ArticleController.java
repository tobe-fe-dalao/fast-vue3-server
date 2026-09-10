package com.fastvue.module.content.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.response.ApiResponse;
import com.fastvue.common.response.PageResponse;
import com.fastvue.module.content.service.ArticleService;
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
 * 文章管理接口。
 */
@Tag(name = "内容管理-文章")
@RestController
@RequestMapping("/api/v1/content/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "分页查询文章")
    @GetMapping
    @PreAuthorize("hasAuthority('content:list')")
    public ApiResponse<PageResponse<ArticleVO>> page(@Valid ArticleQueryRequest query) {
        Page<ArticleVO> page = articleService.page(query);
        return ApiResponse.success(PageResponse.of(page));
    }

    @Operation(summary = "查询文章详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('content:list')")
    public ApiResponse<ArticleVO> getById(@PathVariable Long id) {
        return ApiResponse.success(articleService.getById(id));
    }

    @Operation(summary = "创建文章")
    @PostMapping
    @PreAuthorize("hasAuthority('content:create')")
    public ApiResponse<ArticleVO> create(@Valid @RequestBody CreateArticleRequest request) {
        return ApiResponse.success(articleService.create(request));
    }

    @Operation(summary = "更新文章")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('content:update')")
    public ApiResponse<ArticleVO> update(@PathVariable Long id, @Valid @RequestBody UpdateArticleRequest request) {
        return ApiResponse.success(articleService.update(id, request));
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('content:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ApiResponse.success();
    }
}
