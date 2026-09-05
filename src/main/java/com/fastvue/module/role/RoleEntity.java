package com.fastvue.module.role;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统角色实体。
 */
@Getter
@Setter
@TableName("sys_role")
public class RoleEntity extends BaseEntity {

    private String code;
    private String name;
    private String description;
}
