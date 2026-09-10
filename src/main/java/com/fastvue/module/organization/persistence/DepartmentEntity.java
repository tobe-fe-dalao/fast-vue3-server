package com.fastvue.module.organization.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("org_department")
public class DepartmentEntity extends BaseEntity {
    private Long tenantId;
    private Long organizationId;
    private Long parentId;
    private String name;
    private String code;
    private Long leaderId;
    private Integer sort;
    private String status;
}
