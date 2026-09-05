package com.fastvue.module.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统用户实体。
 */
@Getter
@Setter
@TableName("sys_user")
public class UserEntity extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String status;
}
