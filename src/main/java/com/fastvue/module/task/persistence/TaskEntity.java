package com.fastvue.module.task.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@TableName("biz_task")
public class TaskEntity extends BaseEntity {
    private Long tenantId;
    private Long projectId;
    private String title;
    private String description;
    private Long assigneeId;
    private Long reporterId;
    private String priority;
    private String status;
    private LocalDate dueDate;
    @Version
    private Integer version;
}
