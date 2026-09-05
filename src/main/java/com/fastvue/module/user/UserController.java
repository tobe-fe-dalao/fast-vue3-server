package com.fastvue.module.user;

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
 * 用户管理接口。
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    @PreAuthorize("hasAuthority('user:list')")
    public ApiResponse<PageResponse<UserVO>> page(@Valid UserQueryRequest query) {
        Page<UserVO> page = userService.page(query);
        return ApiResponse.success(PageResponse.of(page));
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:list')")
    public ApiResponse<UserVO> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ApiResponse<UserVO> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public ApiResponse<UserVO> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }
}
