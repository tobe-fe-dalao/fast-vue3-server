package com.fastvue.module.project.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@TableName("biz_project_activity")
public class ProjectActivityEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    private Long projectId;
    private Long actorId;
    private String action;
    private String detail;
    private OffsetDateTime createdAt;
}
