package com.fastvue.common.response;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import java.util.List;

/** Compatibility endpoints must not silently truncate unbounded arrays. */
public final class QueryLimits {
    private QueryLimits() {}

    public static final int LEGACY_MAX = 1000;
    public static final String LEGACY_SQL_LIMIT = "LIMIT 1001";

    public static <T> List<T> requireBounded(List<T> rows) {
        if (rows.size() > LEGACY_MAX) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "结果超过 1000 条，请使用对应的 /page 分页接口");
        }
        return rows;
    }
}
