package com.fastvue.module.site.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Create-checkout request. */
public record CreateCheckoutRequest(
        @NotNull(message = "套餐 ID 不能为空")
        @Positive(message = "套餐 ID 必须大于 0")
        Long planId,

        @NotNull(message = "支付方式不能为空")
        @Pattern(regexp = "alipay|wechat|card", message = "不支持的支付方式")
        String channel) {
}
