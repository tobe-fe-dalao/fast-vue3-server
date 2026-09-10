package com.fastvue.config;

import com.fastvue.module.user.persistence.UserEntity;
import com.fastvue.module.user.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import com.fastvue.infrastructure.tenant.TenantContext;

/**
 * 默认管理员密码初始化。
 *
 * <p>Flyway 迁移中已插入 admin 用户（占位密码）。应用启动时：
 * <ul>
 *   <li>若配置了 {@code ADMIN_PASSWORD} 环境变量，则以该值（BCrypt 后）覆盖 admin 密码；</li>
 *   <li>仅在 dev 环境且未配置时，使用明确标注仅供开发使用的默认密码 {@code admin123}。</li>
 * </ul>
 * 生产环境必须通过环境变量提供密码，避免使用硬编码弱口令。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.default-dev-password:}")
    private String defaultDevPassword;

    @Override
    public void run(ApplicationArguments args) {
        String configured = resolvePassword();
        if (configured == null) {
            return;
        }

        boolean updated = TenantContext.runAs(1L, false, () -> {
            UserEntity admin = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserEntity>()
                            .eq(UserEntity::getUsername, "admin"));
            if (admin == null) {
                return false;
            }
            admin.setPassword(passwordEncoder.encode(configured));
            userMapper.updateById(admin);
            return true;
        });
        if (!updated) {
            log.warn("未找到 admin 用户，跳过密码初始化");
            return;
        }
        log.info("admin 用户密码已按配置初始化（明文来源：{}）",
                adminPassword == null || adminPassword.isBlank() ? "dev 默认值" : "ADMIN_PASSWORD 环境变量");
    }

    private String resolvePassword() {
        if (adminPassword != null && !adminPassword.isBlank()) {
            return adminPassword;
        }
        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")
                && defaultDevPassword != null && !defaultDevPassword.isBlank()) {
            log.warn("未配置 ADMIN_PASSWORD，使用仅供开发使用的默认管理员密码");
            return defaultDevPassword;
        }
        return null;
    }
}
