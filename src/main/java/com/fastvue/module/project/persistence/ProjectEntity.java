package com.fastvue.module.project.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@TableName("biz_project")
public class ProjectEntity extends BaseEntity {
    private Long tenantId;
    private String name;
    private String code;
    private String description;
    private Long ownerId;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    @Version
    private Integer version;
}
