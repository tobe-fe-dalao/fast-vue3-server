package com.fastvue;

import com.fastvue.module.user.UserEntity;
import com.fastvue.module.user.UserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户 CRUD 集成测试（Testcontainers PostgreSQL）。
 *
 * <p>依赖 Docker 环境。未安装 Docker 时，Testcontainers 启动会失败并跳过。
 * 这是唯一需要真实数据库的测试，其余测试均为纯内存切片 / 单元测试。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class UserCrudIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("fastvue3")
                    .withUsername("fastvue3")
                    .withPassword("fastvue3");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // 集成测试不依赖 Redis，直接禁用其自动配置所需的连接
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired
    private UserMapper userMapper;

    @BeforeAll
    static void verifyContainerRunning() {
        assertThat(POSTGRES.isRunning()).isTrue();
    }

    @Test
    @DisplayName("Flyway 迁移后存在默认管理员 admin")
    void defaultAdminExists() {
        UserEntity admin = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, "admin"));
        assertThat(admin).isNotNull();
        assertThat(admin.getPassword()).isNotEqualTo("admin"); // 非明文
    }
}
