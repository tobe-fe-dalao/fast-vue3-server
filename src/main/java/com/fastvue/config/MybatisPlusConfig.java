package com.fastvue.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.fastvue.infrastructure.tenant.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>注册分页插件。复杂查询直接写 SQL 或使用简单 Wrapper，不追求纯 Wrapper 链。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler() {
            private static final java.util.Set<String> TENANT_TABLES = java.util.Set.of(
                    "sys_user", "sys_role", "sys_user_role", "sys_role_permission", "sys_role_menu",
                    "content_category", "content_article", "site_blog_comment", "site_payment_order",
                    "org_organization", "org_department", "org_department_member", "biz_project", "biz_project_member",
                    "biz_project_activity",
                    "biz_task", "biz_task_comment", "biz_task_activity", "wf_approval_request",
                    "wf_approval_step", "wf_approval_action", "sys_notification");

            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContext.tenantId();
                return new LongValue(tenantId == null ? -1L : tenantId);
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return TenantContext.isSuperAdmin()
                        || !TENANT_TABLES.contains(tableName.toLowerCase(java.util.Locale.ROOT));
            }
        }));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
