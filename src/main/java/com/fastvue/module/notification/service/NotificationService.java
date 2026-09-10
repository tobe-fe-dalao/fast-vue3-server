package com.fastvue.module.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.notification.api.NotificationVO;
import com.fastvue.module.notification.persistence.NotificationEntity;
import com.fastvue.module.notification.persistence.NotificationMapper;
import com.fastvue.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationMapper mapper;

    public List<NotificationVO> list() {
        return mapper.selectList(new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getReceiverId, SecurityUtils.currentUser().id())
                .orderByDesc(NotificationEntity::getCreatedAt)).stream().map(this::toVO).toList();
    }

    public long unreadCount() {
        return mapper.selectCount(new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getReceiverId, SecurityUtils.currentUser().id())
                .eq(NotificationEntity::getRead, false));
    }

    @Transactional
    public void markRead(Long id) {
        NotificationEntity value = mapper.selectById(id);
        if (value == null || !value.getReceiverId().equals(SecurityUtils.currentUser().id())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        value.setRead(true);
        mapper.updateById(value);
    }

    @Transactional
    public void markAllRead() { mapper.markAllRead(SecurityUtils.currentUser().id()); }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(Long tenantId, String type, String title, String content, Long receiverId) {
        if (receiverId == null) return;
        TenantContext.runAs(tenantId, false, () -> {
            NotificationEntity value = new NotificationEntity();
            value.setTenantId(tenantId); value.setType(type); value.setTitle(title);
            value.setContent(content); value.setReceiverId(receiverId); value.setRead(false);
            mapper.insert(value);
        });
    }

    private NotificationVO toVO(NotificationEntity value) {
        return new NotificationVO(value.getId(), value.getType(), value.getTitle(), value.getContent(),
                value.getReceiverId(), value.getRead(), value.getCreatedAt());
    }
}
