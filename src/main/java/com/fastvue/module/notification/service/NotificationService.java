package com.fastvue.module.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.common.response.PageRequest;
import com.fastvue.common.response.PageResponse;
import com.fastvue.common.response.QueryLimits;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.notification.api.NotificationVO;
import com.fastvue.module.notification.persistence.NotificationEntity;
import com.fastvue.module.notification.persistence.NotificationMapper;
import com.fastvue.security.SecurityUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationMapper mapper;

    public List<NotificationVO> list() {
        return QueryLimits.requireBounded(
                        mapper.selectList(receiverQuery().last(QueryLimits.LEGACY_SQL_LIMIT)))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public PageResponse<NotificationVO> page(PageRequest request) {
        return PageResponse.map(
                mapper.selectPage(new Page<>(request.page(), request.pageSize()), receiverQuery()),
                this::toVO);
    }

    private LambdaQueryWrapper<NotificationEntity> receiverQuery() {
        return new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getReceiverId, SecurityUtils.currentUser().id())
                .orderByDesc(NotificationEntity::getCreatedAt)
                .orderByDesc(NotificationEntity::getId);
    }

    public long unreadCount() {
        return mapper.selectCount(
                new LambdaQueryWrapper<NotificationEntity>()
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
    public void markAllRead() {
        mapper.markAllRead(SecurityUtils.currentUser().id());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(Long tenantId, String type, String title, String content, Long receiverId) {
        writeNotification(tenantId, type, title, content, receiverId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deliver(Long tenantId, String type, String title, String content, Long receiverId) {
        writeNotification(tenantId, type, title, content, receiverId);
    }

    private void writeNotification(
            Long tenantId, String type, String title, String content, Long receiverId) {
        if (receiverId == null) return;
        TenantContext.runAs(
                tenantId,
                false,
                () -> {
                    NotificationEntity value = new NotificationEntity();
                    value.setTenantId(tenantId);
                    value.setType(type);
                    value.setTitle(title);
                    value.setContent(content);
                    value.setReceiverId(receiverId);
                    value.setRead(false);
                    mapper.insert(value);
                });
    }

    private NotificationVO toVO(NotificationEntity value) {
        return new NotificationVO(
                value.getId(),
                value.getType(),
                value.getTitle(),
                value.getContent(),
                value.getReceiverId(),
                value.getRead(),
                value.getCreatedAt());
    }
}
