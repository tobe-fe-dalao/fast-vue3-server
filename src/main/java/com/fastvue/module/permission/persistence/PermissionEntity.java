package com.fastvue.module.permission.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统权限实体。
 */
@Getter
@Setter
@TableName("sys_permission")
public class PermissionEntity extends BaseEntity {

    private String code;
    private String name;
    private String description;
}
