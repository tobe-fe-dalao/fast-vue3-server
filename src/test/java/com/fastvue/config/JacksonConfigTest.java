package com.fastvue.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

class JacksonConfigTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                    .withUserConfiguration(JacksonConfig.class)
                    .withPropertyValues(
                            "spring.jackson.default-property-inclusion=non_null",
                            "spring.jackson.use-jackson2-defaults=true");

    @Test
    void preservesIsoDatesUtcInstantsAndOmittedNullFields() {
        contextRunner.run(
                context -> {
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    var value =
                            new Payload(
                                    LocalDate.of(2026, 10, 31),
                                    LocalDateTime.of(2026, 10, 31, 8, 30),
                                    Instant.parse("2026-10-31T00:30:00Z"),
                                    null);
                    var json = mapper.readTree(mapper.writeValueAsString(value));
                    assertThat(json.get("day").asString()).isEqualTo("2026-10-31");
                    assertThat(json.get("time").asString()).isEqualTo("2026-10-31T08:30:00");
                    assertThat(json.get("instant").asString()).isEqualTo("2026-10-31T00:30:00Z");
                    assertThat(json.has("optional")).isFalse();
                    assertThat(mapper.readValue(json.toString(), Payload.class)).isEqualTo(value);
                });
    }

    @Test
    void acceptsUnknownJsonFieldsForExistingClients() {
        contextRunner.run(
                context -> {
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    var payload =
                            mapper.readValue(
                                    "{\"optional\":\"ok\",\"future\":true}", Payload.class);
                    assertThat(payload.optional()).isEqualTo("ok");
                });
    }

    record Payload(LocalDate day, LocalDateTime time, Instant instant, String optional) {}
}
