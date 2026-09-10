package com.fastvue.module.content.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fastvue.infrastructure.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 内容分类实体。
 */
@Getter
@Setter
@TableName("content_category")
public class CategoryEntity extends BaseEntity {

    private Long tenantId;
    private String name;
    private String slug;
    private String description;
    private String status;
}
