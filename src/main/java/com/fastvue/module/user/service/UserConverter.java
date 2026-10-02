package com.fastvue.module.user.service;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.fastvue.module.user.api.UserVO;
import com.fastvue.module.user.persistence.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 用户实体 <-> VO / 请求 转换器。
 */
@Mapper(componentModel = SPRING)
public interface UserConverter {

    @Mapping(target = "roles", ignore = true)
    UserVO toVO(UserEntity entity);
}
