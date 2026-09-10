package com.fastvue.module.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.audit.api.OperationLogVO;
import com.fastvue.module.audit.persistence.OperationLogEntity;
import com.fastvue.module.audit.persistence.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogMapper mapper;

    public void save(OperationLogEntity value) { mapper.insert(value); }

    public List<OperationLogVO> list() {
        LambdaQueryWrapper<OperationLogEntity> query = new LambdaQueryWrapper<OperationLogEntity>()
                .orderByDesc(OperationLogEntity::getCreatedAt).last("LIMIT 200");
        if (!TenantContext.isSuperAdmin()) query.eq(OperationLogEntity::getTenantId, TenantContext.tenantId());
        return mapper.selectList(query).stream().map(v -> new OperationLogVO(v.getId(), v.getRequestId(),
                v.getTenantId(), v.getUserId(), v.getMethod(), v.getPath(), v.getIp(), v.getUserAgent(),
                v.getDuration(), v.getStatus(), v.getCreatedAt())).toList();
    }
}
