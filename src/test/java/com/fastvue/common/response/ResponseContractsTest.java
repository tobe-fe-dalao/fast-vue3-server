package com.fastvue.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ResponseContractsTest {
    static ValidatorFactory factory;
    static Validator validator;

    @BeforeAll
    static void validation() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidation() {
        factory.close();
    }

    @Test
    void omittedPaginationUsesDocumentedDefaults() {
        PageRequest request = new PageRequest(null, null);
        assertThat(request.page()).isEqualTo(1L);
        assertThat(request.pageSize()).isEqualTo(20L);
        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"0,20", "-1,20", "10001,20", "1,0", "1,-1", "1,101"})
    void invalidPageNumbersAndUnboundedPageSizesAreRejected(long page, long pageSize) {
        assertThat(validator.validate(new PageRequest(page, pageSize))).isNotEmpty();
    }

    @ParameterizedTest
    @CsvSource({"1,1", "10000,100"})
    void paginationLimitsAreInclusive(long page, long pageSize) {
        assertThat(validator.validate(new PageRequest(page, pageSize))).isEmpty();
    }

    @Test
    void conversionPreservesPaginationMetadataAndRecordOrder() {
        Page<Integer> page = new Page<>(3, 2, 8);
        page.setRecords(List.of(42, 43));
        PageResponse<String> result = PageResponse.map(page, value -> "item-" + value);
        assertThat(result.items()).containsExactly("item-42", "item-43");
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.pageSize()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(8);
    }

    @Test
    void emptyPagePreservesTotalInsteadOfResettingIt() {
        Page<Integer> page = new Page<>(10, 20, 5);
        PageResponse<Integer> response = PageResponse.of(page);
        assertThat(response.items()).isEmpty();
        assertThat(response.page()).isEqualTo(10);
        assertThat(response.total()).isEqualTo(5);
    }

    @Test
    void legacyListLimitAllowsBoundaryAndRejectsTruncation() {
        List<String> boundary = Collections.nCopies(QueryLimits.LEGACY_MAX, "item");
        assertThat(QueryLimits.requireBounded(boundary)).hasSize(1000);
        assertThatThrownBy(() -> QueryLimits.requireBounded(Collections.nCopies(1001, "item")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void errorResponseCannotExposeSuccessfulPayload() {
        ApiResponse<List<String>> response = ApiResponse.error(ErrorCode.CONFLICT, "stale version");
        assertThat(response.code()).isEqualTo(ErrorCode.CONFLICT.code());
        assertThat(response.message()).isEqualTo("stale version");
        assertThat(response.data()).isNull();
    }
}
