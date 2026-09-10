package com.fastvue.module.permission.service;

import com.fastvue.module.permission.api.PermissionVO;
import com.fastvue.module.permission.persistence.PermissionEntity;
import org.mapstruct.Mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * 权限实体 <-> VO 转换器。
 */
@Mapper(componentModel = SPRING)
public interface PermissionConverter {

    PermissionVO toVO(PermissionEntity entity);
}
