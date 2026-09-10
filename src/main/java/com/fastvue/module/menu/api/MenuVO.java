package com.fastvue.module.menu.api;

import java.util.List;

/**
 * 菜单树节点。
 */
public record MenuVO(
        Long id,
        Long parentId,
        String name,
        String path,
        String component,
        String icon,
        Integer sort,
        Boolean visible,
        String permission,
        String type,
        List<MenuVO> children) {
}
