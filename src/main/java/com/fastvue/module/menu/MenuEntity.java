package com.fastvue.module.menu;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统菜单实体。
 */
@Getter
@Setter
@TableName("sys_menu")
public class MenuEntity extends BaseEntity {

    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Boolean visible;
    private String permission;
    private String type;
}
