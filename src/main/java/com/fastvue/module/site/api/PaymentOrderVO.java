package com.fastvue.module.site.api;

import java.time.OffsetDateTime;

/** Checkout response returned to the site application. */
public record PaymentOrderVO(
        Long id,
        String orderNo,
        Long planId,
        String planName,
        Integer amountCents,
        String currency,
        String channel,
        String status,
        String checkoutUrl,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt) {
}
