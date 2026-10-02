package com.fastvue.config;

import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.user.persistence.UserEntity;
import com.fastvue.module.user.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 默认管理员密码初始化。
 *
 * <p>Flyway 迁移中已插入 admin 用户（占位密码）。应用启动时：
 * <ul>
 *   <li>若配置了 {@code ADMIN_PASSWORD} 环境变量，则仅将尚未初始化的占位密码替换为 BCrypt 密码；</li>
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

    private final AdminProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        String configured = resolvePassword();
        if (configured == null) {
            return;
        }

        boolean updated =
                TenantContext.runAs(
                        1L,
                        false,
                        () -> {
                            UserEntity admin =
                                    userMapper.selectOne(
                                            new com.baomidou.mybatisplus.core.conditions.query
                                                                    .LambdaQueryWrapper<
                                                            UserEntity>()
                                                    .eq(UserEntity::getUsername, "admin"));
                            if (admin == null
                                    || !"$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa"
                                            .equals(admin.getPassword())) {
                                return false;
                            }
                            return userMapper.update(
                                            null,
                                            new com.baomidou.mybatisplus.core.conditions.update
                                                                    .LambdaUpdateWrapper<
                                                            UserEntity>()
                                                    .eq(UserEntity::getId, admin.getId())
                                                    .eq(
                                                            UserEntity::getPassword,
                                                            admin.getPassword())
                                                    .set(
                                                            UserEntity::getPassword,
                                                            passwordEncoder.encode(configured)))
                                    == 1;
                        });
        if (!updated) {
            log.debug("管理员已初始化或不存在，保留当前密码");
            return;
        }
        log.info(
                "admin 用户密码已按配置初始化（明文来源：{}）",
                properties.password() == null || properties.password().isBlank()
                        ? "dev 默认值"
                        : "ADMIN_PASSWORD 环境变量");
    }

    private String resolvePassword() {
        if (properties.password() != null && !properties.password().isBlank()) {
            return properties.password();
        }
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev"))
                && properties.defaultDevPassword() != null
                && !properties.defaultDevPassword().isBlank()) {
            log.warn("未配置 ADMIN_PASSWORD，使用仅供开发使用的默认管理员密码");
            return properties.defaultDevPassword();
        }
        return null;
    }
}
