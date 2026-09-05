package com.fastvue.module.menu;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * 菜单实体 <-> VO 转换器。
 */
@Mapper(componentModel = SPRING)
public interface MenuConverter {

    @Mapping(target = "children", ignore = true)
    MenuVO toVO(MenuEntity entity);
}
