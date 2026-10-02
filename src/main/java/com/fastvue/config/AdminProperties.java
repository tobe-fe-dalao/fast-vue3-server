package com.fastvue.config;

import com.fastvue.common.validation.BcryptPassword;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.admin")
public record AdminProperties(
        @BcryptPassword String password, @BcryptPassword String defaultDevPassword) {}
