package com.fastvue.module.content;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 内容文章实体。
 */
@Getter
@Setter
@TableName("content_article")
public class ArticleEntity extends BaseEntity {

    private String title;
    private String author;
    private String summary;

    /** 正文段落，以 JSON 数组形式存储 */
    @TableField(typeHandler = JsonStringListTypeHandler.class)
    private List<String> content;

    private String cover;
    private Long categoryId;
    private String status;

    /** 标签，以 JSON 数组形式存储 */
    @TableField(typeHandler = JsonStringListTypeHandler.class)
    private List<String> tags;

    /** 发布日期，格式 yyyy-MM-dd */
    private String date;
}
