package com.fastvue.module.role.service;

import com.fastvue.module.role.api.RoleVO;
import com.fastvue.module.role.persistence.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * 角色实体 <-> VO 转换器。
 */
@Mapper(componentModel = SPRING)
public interface RoleConverter {

    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "menuIds", ignore = true)
    RoleVO toVO(RoleEntity entity);
}
