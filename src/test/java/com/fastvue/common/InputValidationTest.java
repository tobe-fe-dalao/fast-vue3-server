package com.fastvue.common;

import static org.assertj.core.api.Assertions.*;

import com.fastvue.module.content.api.*;
import com.fastvue.module.role.api.*;
import com.fastvue.module.user.api.UpdateUserRequest;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.*;
import org.junit.jupiter.api.*;

class InputValidationTest {
    static ValidatorFactory factory;

    @BeforeAll
    static void validator() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void roleIdsRejectNullNegativeAndOversizedCollections() {
        for (var ids :
                List.of(Arrays.asList(1L, null), List.of(0L), Collections.nCopies(1001, 1L))) {
            assertThat(
                            factory.getValidator()
                                    .validate(
                                            new CreateRoleRequest("code", "name", null, ids, null)))
                    .isNotEmpty();
            assertThat(
                            factory.getValidator()
                                    .validate(new UpdateRoleRequest(null, null, null, ids)))
                    .isNotEmpty();
            assertThat(
                            factory.getValidator()
                                    .validate(
                                            new UpdateUserRequest(
                                                    null, null, null, null, null, ids)))
                    .isNotEmpty();
        }
    }

    @Test
    void absentAndEmptyCollectionsRetainPartialUpdateAndClearSemantics() {
        for (List<Long> ids : Arrays.asList(null, List.<Long>of(), List.of(1L, 1L))) {
            assertThat(factory.getValidator().validate(new UpdateRoleRequest(null, null, ids, ids)))
                    .isEmpty();
            assertThat(
                            factory.getValidator()
                                    .validate(
                                            new UpdateUserRequest(
                                                    null, null, null, null, null, ids)))
                    .isEmpty();
        }
    }

    @Test
    void articleContentAndTagsRejectNullElementsAndBoundWork() {
        assertThat(
                        factory.getValidator()
                                .validate(
                                        new CreateArticleRequest(
                                                "title",
                                                null,
                                                null,
                                                Arrays.asList("paragraph", null),
                                                null,
                                                null,
                                                null,
                                                List.of())))
                .isNotEmpty();
        assertThat(
                        factory.getValidator()
                                .validate(
                                        new UpdateArticleRequest(
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                Collections.nCopies(101, "tag"))))
                .isNotEmpty();
        assertThat(
                        factory.getValidator()
                                .validate(
                                        new UpdateArticleRequest(
                                                null,
                                                null,
                                                null,
                                                List.of("x".repeat(10001)),
                                                null,
                                                null,
                                                null,
                                                List.of("x".repeat(65)))))
                .hasSize(2);
        assertThat(
                        factory.getValidator()
                                .validate(
                                        new CreateArticleRequest(
                                                "title",
                                                null,
                                                null,
                                                Collections.nCopies(1001, ""),
                                                null,
                                                null,
                                                null,
                                                null)))
                .isNotEmpty();
    }

    @Test
    void articleOptionalEmptyAndMaximumLengthValuesRemainValid() {
        assertThat(
                        factory.getValidator()
                                .validate(
                                        new CreateArticleRequest(
                                                "title",
                                                null,
                                                null,
                                                List.of("", "x".repeat(10000)),
                                                null,
                                                null,
                                                null,
                                                List.of("x".repeat(64)))))
                .isEmpty();
        assertThat(
                        factory.getValidator()
                                .validate(
                                        new UpdateArticleRequest(
                                                null, null, null, List.of(), null, null, null,
                                                List.of())))
                .isEmpty();
    }
}
