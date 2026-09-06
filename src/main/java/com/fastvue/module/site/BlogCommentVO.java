package com.fastvue.module.site;

import java.time.OffsetDateTime;

/** Public comment projection without internal user identifiers. */
public record BlogCommentVO(
        Long id,
        Long articleId,
        String username,
        String content,
        OffsetDateTime createdAt) {
}
