package com.fastvue.module.notification.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_notification")
public class NotificationEntity extends BaseEntity {
    private Long tenantId;
    private String type;
    private String title;
    private String content;
    private Long receiverId;
    private Boolean read;
}
