package com.fastvue.module.content.service;

import com.fastvue.module.content.api.ArticleVO;
import com.fastvue.module.content.api.CategoryVO;
import com.fastvue.module.content.persistence.ArticleEntity;
import com.fastvue.module.content.persistence.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * 内容实体 <-> VO 转换器。
 */
@Mapper(componentModel = SPRING)
public interface ContentConverter {

    /** 分类名称需由 Service 关联查询后单独设置，此处忽略 */
    @Mapping(target = "category", ignore = true)
    ArticleVO toArticleVO(ArticleEntity entity);

    CategoryVO toCategoryVO(CategoryEntity entity);
}
