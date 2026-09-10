package com.fastvue.module.site.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Checkout order created before handing off to a payment provider. */
@Getter
@Setter
@TableName("site_payment_order")
public class PaymentOrderEntity extends BaseEntity {
    private Long tenantId;
    private String orderNo;
    private Long userId;
    private Long planId;
    private String planName;
    private Integer amountCents;
    private String currency;
    private String channel;
    private String status;
    private OffsetDateTime expiresAt;
}
