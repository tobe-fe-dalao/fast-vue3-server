package com.fastvue.common.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Shared paging input; individual modules still own their query and result models. */
public record PageRequest(@Min(1) @Max(10000) Long page, @Min(1) @Max(100) Long pageSize) {
    public PageRequest {
        if (page == null) page = 1L;
        if (pageSize == null) pageSize = 20L;
    }
}
