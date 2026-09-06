package com.fastvue.module.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Checkout order created before handing off to a payment provider. */
@Getter
@Setter
@TableName("site_payment_order")
public class PaymentOrderEntity extends BaseEntity {
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
