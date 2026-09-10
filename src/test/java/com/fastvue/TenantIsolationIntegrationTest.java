package com.fastvue;

import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.config.AdminInitializer;
import com.fastvue.module.tenant.persistence.TenantEntity;
import com.fastvue.module.tenant.persistence.TenantMapper;
import com.fastvue.module.tenant.api.TenantModels;
import com.fastvue.module.tenant.api.TenantVO;
import com.fastvue.module.tenant.service.TenantService;
import com.fastvue.module.organization.persistence.OrganizationEntity;
import com.fastvue.module.organization.persistence.OrganizationMapper;
import com.fastvue.module.project.persistence.ProjectEntity;
import com.fastvue.module.project.persistence.ProjectMapper;
import com.fastvue.module.role.persistence.RoleEntity;
import com.fastvue.module.role.persistence.RoleMapper;
import com.fastvue.module.task.persistence.TaskEntity;
import com.fastvue.module.task.persistence.TaskMapper;
import com.fastvue.module.user.persistence.UserEntity;
import com.fastvue.module.user.persistence.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class TenantIsolationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fastvue3").withUsername("fastvue3").withPassword("fastvue3");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired TenantMapper tenantMapper;
    @Autowired AdminInitializer adminInitializer;
    @Autowired TenantService tenantService;
    @Autowired OrganizationMapper organizationMapper;
    @Autowired RoleMapper roleMapper;
    @Autowired UserMapper userMapper;
    @Autowired ProjectMapper projectMapper;
    @Autowired TaskMapper taskMapper;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotReadAnotherTenantUserOrProjectById() {
        TenantEntity second = new TenantEntity();
        second.setName("Tenant B"); second.setCode("tenant-b"); second.setStatus("active"); second.setPlan("standard");
        tenantMapper.insert(second);

        UserEntity other = TenantContext.runAs(second.getId(), false, () -> {
            UserEntity user = new UserEntity();
            user.setTenantId(second.getId()); user.setUsername("tenant-b-user"); user.setPassword("hashed"); user.setStatus("active");
            userMapper.insert(user); return user;
        });

        assertThat(TenantContext.runAs(1L, false, () -> userMapper.selectById(other.getId()))).isNull();
        assertThat(TenantContext.runAs(1L, true, () -> userMapper.selectById(other.getId()))).isNotNull();

        ProjectEntity otherProject = TenantContext.runAs(second.getId(), false, () -> {
            ProjectEntity project = new ProjectEntity();
            project.setTenantId(second.getId()); project.setName("Tenant B Project");
            project.setCode("TENANT-B-PROJECT"); project.setOwnerId(other.getId());
            project.setStatus("ACTIVE"); project.setVersion(0);
            projectMapper.insert(project);
            return project;
        });
        assertThat(TenantContext.runAs(1L, false, () -> projectMapper.selectById(otherProject.getId()))).isNull();
        assertThat(TenantContext.runAs(1L, true, () -> projectMapper.selectById(otherProject.getId()))).isNotNull();
    }

    @Test
    void tenantProvisioningCreatesIsolatedOrganizationAdministratorAndRole() {
        TenantModels.CreateRequest request = new TenantModels.CreateRequest(
                "Provisioned Tenant", "provisioned", "enterprise", null,
                "admin", "secure-pass-123", "admin@provisioned.example");

        TenantVO created = TenantContext.runAs(1L, true, () -> tenantService.create(request));

        assertThat(created.code()).isEqualTo("provisioned");
        TenantContext.runAs(created.id(), false, () -> {
            OrganizationEntity organization = organizationMapper.selectOne(new LambdaQueryWrapper<OrganizationEntity>()
                    .eq(OrganizationEntity::getTenantId, created.id()));
            UserEntity administrator = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                    .eq(UserEntity::getUsername, "admin"));
            RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                    .eq(RoleEntity::getCode, "tenant-admin"));

            assertThat(organization).isNotNull();
            assertThat(administrator).isNotNull();
            assertThat(administrator.getId()).isNotEqualTo(1L);
            assertThat(role).isNotNull();
            assertThat(userMapper.selectRoleCodes(administrator.getId())).containsExactly("tenant-admin");
            assertThat(roleMapper.selectPermissionRows(List.of(role.getId()))).isNotEmpty();
            assertThat(roleMapper.selectMenuRows(List.of(role.getId()))).isNotEmpty();
        });
        adminInitializer.run(new DefaultApplicationArguments(new String[0]));
        assertThat(TenantContext.runAs(1L, false, () -> userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, "admin")))).satisfies(systemAdmin -> {
                    assertThat(systemAdmin).isNotNull();
                    assertThat(systemAdmin.getId()).isEqualTo(1L);
                });
    }

    @Test
    void staleTaskWriterIsRejectedByDatabaseOptimisticLock() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantContext.runAs(1L, false, () -> {
            ProjectEntity project = new ProjectEntity();
            project.setTenantId(1L);
            project.setName("Optimistic Lock Project");
            project.setCode("LOCK-" + suffix);
            project.setOwnerId(1L);
            project.setStatus("ACTIVE");
            project.setVersion(0);
            projectMapper.insert(project);

            TaskEntity task = new TaskEntity();
            task.setTenantId(1L);
            task.setProjectId(project.getId());
            task.setTitle("Initial title");
            task.setReporterId(1L);
            task.setPriority("MEDIUM");
            task.setStatus("TODO");
            task.setVersion(0);
            taskMapper.insert(task);

            TaskEntity firstWriter = taskMapper.selectById(task.getId());
            TaskEntity staleWriter = taskMapper.selectById(task.getId());
            firstWriter.setTitle("First writer wins");
            staleWriter.setTitle("Stale writer loses");

            assertThat(taskMapper.updateById(firstWriter)).isEqualTo(1);
            assertThat(taskMapper.updateById(staleWriter)).isZero();
            assertThat(taskMapper.selectById(task.getId()).getTitle()).isEqualTo("First writer wins");
        });
    }

    @Test
    void twoConcurrentTaskUpdatesAllowOnlyOneWinner() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TaskEntity task = TenantContext.runAs(1L, false, () -> {
            ProjectEntity project = new ProjectEntity();
            project.setTenantId(1L); project.setName("Concurrent Project");
            project.setCode("CONCURRENT-" + suffix); project.setOwnerId(1L);
            project.setStatus("ACTIVE"); project.setVersion(0);
            projectMapper.insert(project);
            projectMapper.addMember(1L, project.getId(), 1L, "OWNER");
            assertThat(projectMapper.selectMemberRows(List.of(project.getId()))).hasSize(1);

            TaskEntity created = new TaskEntity();
            created.setTenantId(1L); created.setProjectId(project.getId());
            created.setTitle("Initial title"); created.setReporterId(1L);
            created.setPriority("MEDIUM"); created.setStatus("TODO"); created.setVersion(0);
            taskMapper.insert(created);
            return created;
        });

        CountDownLatch bothLoaded = new CountDownLatch(2);
        CountDownLatch releaseWrites = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            java.util.function.Function<String, Integer> update = title -> TenantContext.runAs(1L, false, () -> {
                TaskEntity writer = taskMapper.selectById(task.getId());
                bothLoaded.countDown();
                try {
                    if (!releaseWrites.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("writers timed out");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                writer.setTitle(title);
                return taskMapper.updateById(writer);
            });
            Future<Integer> first = executor.submit(() -> update.apply("First writer"));
            Future<Integer> second = executor.submit(() -> update.apply("Second writer"));
            assertThat(bothLoaded.await(10, TimeUnit.SECONDS)).isTrue();
            releaseWrites.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS)).isEqualTo(1);
        } finally {
            releaseWrites.countDown();
            executor.shutdownNow();
        }
        assertThat(TenantContext.runAs(1L, false, () -> taskMapper.selectById(task.getId()).getVersion()))
                .isEqualTo(1);
    }
}
