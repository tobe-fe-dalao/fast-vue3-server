package com.fastvue.module.user.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统用户实体。
 */
@Getter
@Setter
@TableName("sys_user")
public class UserEntity extends BaseEntity {

    private Long tenantId;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String status;
}
