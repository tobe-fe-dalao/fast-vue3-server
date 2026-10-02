package com.fastvue.module.organization.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrganizationMapper extends BaseMapper<OrganizationEntity> {
    // All hierarchy and membership writers serialize on the tenant's organization.
    // Flush the session cache so reads after a contended lock observe the committed state.
    @org.apache.ibatis.annotations.Select(
            "SELECT id FROM org_organization WHERE tenant_id = #{tenantId} FOR UPDATE")
    @org.apache.ibatis.annotations.Options(
            flushCache = org.apache.ibatis.annotations.Options.FlushCachePolicy.TRUE)
    Long lockByTenantId(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId);
}
