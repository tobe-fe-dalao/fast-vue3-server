package com.fastvue.module.approval.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("wf_approval_request")
public class ApprovalRequestEntity extends BaseEntity {
    private Long tenantId;
    private String type;
    private String title;
    private String businessKey;
    private Long applicantId;
    private Long departmentId;
    private String status;
    private Integer currentStep;
    private String payload;
    @Version
    private Integer version;
}
