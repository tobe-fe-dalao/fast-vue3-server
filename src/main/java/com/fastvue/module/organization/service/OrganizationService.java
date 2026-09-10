package com.fastvue.module.organization.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.organization.api.OrganizationModels.OrganizationVO;
import com.fastvue.module.organization.api.OrganizationModels.UpdateRequest;
import com.fastvue.module.organization.persistence.OrganizationEntity;
import com.fastvue.module.organization.persistence.OrganizationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationMapper mapper;

    public OrganizationVO current() {
        OrganizationEntity value = mapper.selectOne(new LambdaQueryWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getTenantId, TenantContext.tenantId()));
        if (value == null) throw new BusinessException(ErrorCode.NOT_FOUND, "组织不存在");
        return toVO(value);
    }

    @Transactional
    public OrganizationVO update(Long id, UpdateRequest request) {
        OrganizationEntity value = mapper.selectById(id);
        if (value == null) throw new BusinessException(ErrorCode.NOT_FOUND, "组织不存在");
        if (request.name() != null) value.setName(request.name());
        if (request.status() != null) value.setStatus(request.status());
        mapper.updateById(value);
        return toVO(value);
    }

    private OrganizationVO toVO(OrganizationEntity value) {
        return new OrganizationVO(value.getId(), value.getTenantId(), value.getName(), value.getCode(), value.getStatus());
    }
}
