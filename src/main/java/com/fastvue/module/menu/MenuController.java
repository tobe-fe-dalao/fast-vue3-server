package com.fastvue.module.menu;

import com.fastvue.common.ApiResponse;
import com.fastvue.module.user.UserMapper;
import com.fastvue.security.SecurityUtils;
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
 * 菜单管理接口。
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final UserMapper userMapper;

    @Operation(summary = "查询当前用户有权限访问的菜单树")
    @GetMapping
    public ApiResponse<List<MenuVO>> myMenus() {
        Long userId = SecurityUtils.currentUser().id();
        List<Long> menuIds = userMapper.selectMenuIds(userId);
        return ApiResponse.success(menuService.treeByIds(menuIds));
    }

    @Operation(summary = "查询全部菜单树（管理端）")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('menu:list')")
    public ApiResponse<List<MenuVO>> tree() {
        return ApiResponse.success(menuService.tree());
    }

    @Operation(summary = "查询菜单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:list')")
    public ApiResponse<MenuVO> getById(@PathVariable Long id) {
        return ApiResponse.success(menuService.getById(id));
    }

    @Operation(summary = "创建菜单")
    @PostMapping
    @PreAuthorize("hasAuthority('menu:create')")
    public ApiResponse<MenuVO> create(@Valid @RequestBody MenuRequest request) {
        return ApiResponse.success(menuService.create(request));
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:update')")
    public ApiResponse<MenuVO> update(@PathVariable Long id, @Valid @RequestBody MenuRequest request) {
        return ApiResponse.success(menuService.update(id, request));
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.success();
    }
}
