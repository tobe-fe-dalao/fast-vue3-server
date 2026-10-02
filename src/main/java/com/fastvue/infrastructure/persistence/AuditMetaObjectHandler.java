package com.fastvue.infrastructure.persistence;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.fastvue.security.SecurityUtils;
import java.time.OffsetDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 审计字段自动填充。
 *
 * <p>插入时填充 {@code createdAt/updatedAt/createdBy/updatedBy}，更新时刷新
 * {@code updatedAt/updatedBy}。创建人信息来自当前登录用户（若存在）。</p>
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, now);

        Long currentUserId = SecurityUtils.currentUserIdOrNull();
        if (currentUserId != null) {
            strictInsertFill(metaObject, "createdBy", Long.class, currentUserId);
            strictInsertFill(metaObject, "updatedBy", Long.class, currentUserId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updatedAt", OffsetDateTime.now(java.time.ZoneOffset.UTC), metaObject);

        Long currentUserId = SecurityUtils.currentUserIdOrNull();
        if (currentUserId != null) {
            setFieldValByName("updatedBy", currentUserId, metaObject);
        }
    }
}
