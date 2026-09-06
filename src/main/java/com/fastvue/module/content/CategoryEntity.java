package com.fastvue.module.content;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 内容分类实体。
 */
@Getter
@Setter
@TableName("content_category")
public class CategoryEntity extends BaseEntity {

    private String name;
    private String slug;
    private String description;
    private String status;
}
