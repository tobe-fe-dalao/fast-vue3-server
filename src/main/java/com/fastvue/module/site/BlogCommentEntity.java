package com.fastvue.module.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/** Persisted comment submitted from a public blog detail page. */
@Getter
@Setter
@TableName("site_blog_comment")
public class BlogCommentEntity extends BaseEntity {
    private Long articleId;
    private Long userId;
    private String username;
    private String content;
}
