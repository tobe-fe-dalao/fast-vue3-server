package com.fastvue.module.audit.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@TableName("sys_operation_log")
public class OperationLogEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String requestId;
    private Long tenantId;
    private Long userId;
    private String method;
    private String path;
    private String ip;
    private String userAgent;
    private Long duration;
    private Integer status;
    private OffsetDateTime createdAt;
}
