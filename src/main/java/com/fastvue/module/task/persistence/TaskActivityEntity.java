package com.fastvue.module.task.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@TableName("biz_task_activity")
public class TaskActivityEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long taskId;
    private Long actorId;
    private String action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private OffsetDateTime createdAt;
}
