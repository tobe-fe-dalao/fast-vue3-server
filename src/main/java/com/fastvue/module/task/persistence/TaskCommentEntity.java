package com.fastvue.module.task.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_task_comment")
public class TaskCommentEntity extends BaseEntity {
    private Long tenantId;
    private Long taskId;
    private Long authorId;
    private String content;
}
