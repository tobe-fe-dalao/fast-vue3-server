package com.fastvue.config;

import java.util.TimeZone;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Jackson 配置。
 *
 * <p>统一时间序列化为 ISO-8601 字符串（UTC），避免输出 Java 8 时间对象的数组形式。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.defaultTimeZone(TimeZone.getTimeZone("UTC"));
        };
    }
}
