package com.fastvue.module.site.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/** Persisted comment submitted from a public blog detail page. */
@Getter
@Setter
@TableName("site_blog_comment")
public class BlogCommentEntity extends BaseEntity {
    private Long tenantId;
    private Long articleId;
    private Long userId;
    private String username;
    private String content;
}
