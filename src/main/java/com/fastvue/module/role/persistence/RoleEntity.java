package com.fastvue.module.role.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统角色实体。
 */
@Getter
@Setter
@TableName("sys_role")
public class RoleEntity extends BaseEntity {

    private Long tenantId;
    private String code;
    private String name;
    private String description;
}
