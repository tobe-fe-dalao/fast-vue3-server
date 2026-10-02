package com.fastvue.module.tenant.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_tenant")
public class TenantEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private String status;
    private String plan;
    private OffsetDateTime expiredAt;
    private OffsetDateTime createdAt;
}
