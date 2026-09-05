package com.fastvue.module.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * 用户实体 <-> VO / 请求 转换器。
 */
@Mapper(componentModel = SPRING)
public interface UserConverter {

    @Mapping(target = "roles", ignore = true)
    UserVO toVO(UserEntity entity);
}
