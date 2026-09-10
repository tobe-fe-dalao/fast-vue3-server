package com.fastvue.module.content.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.module.content.service.CategoryService;
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
 * 分类管理接口。
 */
@Tag(name = "内容管理-分类")
@RestController
@RequestMapping("/api/v1/content/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "查询全部分类")
    @GetMapping
    @PreAuthorize("hasAuthority('content:list')")
    public ApiResponse<List<CategoryVO>> listAll() {
        return ApiResponse.success(categoryService.listAll());
    }

    @Operation(summary = "查询分类详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('content:list')")
    public ApiResponse<CategoryVO> getById(@PathVariable Long id) {
        return ApiResponse.success(categoryService.getById(id));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    @PreAuthorize("hasAuthority('content:create')")
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success(categoryService.create(request));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('content:update')")
    public ApiResponse<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success(categoryService.update(id, request));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('content:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success();
    }
}
