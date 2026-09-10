package com.fastvue.module.menu.service;

import com.fastvue.module.menu.api.MenuVO;
import com.fastvue.module.menu.persistence.MenuEntity;
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
