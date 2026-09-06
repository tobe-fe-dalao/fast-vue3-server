package com.fastvue.module.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章数据访问。
 */
@Mapper
public interface ArticleMapper extends BaseMapper<ArticleEntity> {
}
