package com.fastvue.module.site.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** Payment order data access. */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrderEntity> {
}
