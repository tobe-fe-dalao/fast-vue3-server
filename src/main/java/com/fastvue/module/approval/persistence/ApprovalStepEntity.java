package com.fastvue.module.approval.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@TableName("wf_approval_step")
public class ApprovalStepEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long approvalRequestId;
    private Integer stepOrder;
    private String approverType;
    private Long approverId;
    private String status;
    private OffsetDateTime actedAt;
}
