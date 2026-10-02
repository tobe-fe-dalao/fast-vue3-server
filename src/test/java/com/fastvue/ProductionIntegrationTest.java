package com.fastvue;

import static org.assertj.core.api.Assertions.*;

import com.fastvue.config.AdminInitializer;
import com.fastvue.infrastructure.idempotency.IdempotencyService;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.project.api.ProjectModels;
import com.fastvue.module.project.service.ProjectService;
import com.fastvue.module.task.api.TaskModels;
import com.fastvue.module.task.service.TaskService;
import com.fastvue.security.LoginUser;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
@ActiveProfiles("prod")
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.jwt.secret=test-only-production-profile-key-at-least-thirty-two-bytes",
            "app.admin.password=production-test-password",
            "app.notifications.delivery-enabled=false",
            "app.cors.allowed-origin-patterns=https://app.example.com",
            "app.storage.local-root=${java.io.tmpdir}/fastvue-production-tests"
        })
class ProductionIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired StringRedisTemplate redis;
    @Autowired IdempotencyService idempotency;
    @Autowired AdminInitializer initializer;
    @Autowired ProjectService projects;
    @Autowired TaskService tasks;
    @Autowired com.fastvue.module.organization.service.DepartmentService departments;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        // This Redis container belongs only to this test class.
        try (var connection = redis.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void productionStartsMigratesAndExposesSeparateProbes() {
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM flyway_schema_history WHERE success",
                                Integer.class))
                .isEqualTo(13);
        for (String path :
                List.of(
                        "/actuator/health",
                        "/actuator/health/liveness",
                        "/actuator/health/readiness")) {
            var response = http.getForEntity(path, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("UP").doesNotContain("password");
        }
    }

    @Test
    void authenticationRotationAuthorizationAndCorrelationWorkOverHttp() {
        var tokens = login();
        String access = (String) tokens.get("accessToken");
        String refresh = (String) tokens.get("refreshToken");
        var headers = new HttpHeaders();
        headers.setBearerAuth(access);
        headers.set("X-Request-Id", "production-regression");
        var me =
                http.exchange(
                        "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getHeaders().getFirst("X-Request-Id")).isEqualTo("production-regression");
        assertThat(me.getBody()).contains("admin").doesNotContain("password");
        headers.setBearerAuth(refresh);
        assertThat(
                        http.exchange(
                                        "/api/v1/auth/me",
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(
                        http.postForEntity(
                                        "/api/v1/auth/refresh",
                                        Map.of("refreshToken", refresh),
                                        Map.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(
                        http.postForEntity(
                                        "/api/v1/auth/refresh",
                                        Map.of("refreshToken", refresh),
                                        Map.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        var anonymous = http.getForEntity("/actuator/prometheus", String.class);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(anonymous.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        headers.setBearerAuth(access);
        assertThat(
                        http.exchange(
                                        "/actuator/prometheus",
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void demoAndSwaggerAreDisabledAndCheckoutFailsClosed() {
        var headers = new HttpHeaders();
        headers.setBearerAuth((String) login().get("accessToken"));
        assertThat(
                        http.exchange(
                                        "/api/v1/dashboard/stats",
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        http.exchange(
                                        "/api-docs-ui/index.html",
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        var response =
                http.exchange(
                        "/api/v1/payments/checkout",
                        HttpMethod.POST,
                        new HttpEntity<>(Map.of("planId", 2, "channel", "card"), headers),
                        String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM site_payment_order", Integer.class))
                .isZero();
    }

    @Test
    void loginRateLimitIsAtomicAndHasExpiry() {
        for (int i = 0; i < 10; i++) {
            assertThat(
                            http.postForEntity(
                                            "/api/v1/auth/login",
                                            Map.of("username", "missing", "password", "bad"),
                                            String.class)
                                    .getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        var corsHeaders = new HttpHeaders();
        corsHeaders.setOrigin("https://app.example.com");
        var response =
                http.exchange(
                        "/api/v1/auth/login",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                Map.of("username", "missing", "password", "bad"), corsHeaders),
                        String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("https://app.example.com");
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("60");
        assertThat(response.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        var keys = redis.keys("rate:login:*");
        assertThat(keys).hasSize(1);
        assertThat(redis.getExpire(keys.iterator().next())).isBetween(1L, 60L);
    }

    @Test
    void restartingNeverOverwritesInitializedAdministrator() {
        String before =
                jdbc.queryForObject("SELECT password FROM sys_user WHERE id=1", String.class);
        initializer.run(new DefaultApplicationArguments());
        assertThat(jdbc.queryForObject("SELECT password FROM sys_user WHERE id=1", String.class))
                .isEqualTo(before);
    }

    @Test
    void commandFailureRollsBackBusinessAndClaimAllowingRetry() {
        authenticate();
        String key = UUID.randomUUID().toString();
        assertThatThrownBy(
                        () ->
                                idempotency.execute(
                                        "rollback",
                                        key,
                                        () -> {
                                            jdbc.update(
                                                    "INSERT INTO sys_permission(code,name) VALUES (?,?)",
                                                    key,
                                                    "rollback");
                                            throw new IllegalStateException("business failure");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_permission WHERE code=?",
                                Integer.class,
                                key))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_idempotency WHERE request_key=?",
                                Integer.class,
                                key))
                .isZero();
        assertThat(idempotency.execute("rollback", key, () -> "retried")).isEqualTo("retried");
        assertThatThrownBy(() -> idempotency.execute("rollback", key, () -> "duplicate"))
                .hasMessageContaining("幂等");
    }

    @Test
    void concurrentIdempotentCommandsCommitOnlyOnce() throws Exception {
        String key = UUID.randomUUID().toString();
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> command =
                    () -> {
                        authenticate();
                        ready.countDown();
                        start.await(10, TimeUnit.SECONDS);
                        try {
                            idempotency.execute(
                                    "concurrent",
                                    key,
                                    () -> {
                                        jdbc.update(
                                                "INSERT INTO sys_permission(code,name) VALUES (?,?)",
                                                key,
                                                "once");
                                        return null;
                                    });
                            return true;
                        } catch (com.fastvue.common.exception.BusinessException ex) {
                            return false;
                        } finally {
                            TenantContext.clear();
                            SecurityContextHolder.clearContext();
                        }
                    };
            Future<Boolean> a = executor.submit(command), b = executor.submit(command);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_permission WHERE code=?",
                                Integer.class,
                                key))
                .isEqualTo(1);
    }

    @Test
    void taskCreationWaitsForArchiveAndRejectsAfterCommit() throws Exception {
        authenticate();
        var project =
                projects.create(
                        new ProjectModels.CreateRequest(
                                "Race", UUID.randomUUID().toString(), null, null, null));
        CountDownLatch archived = new CountDownLatch(1),
                release = new CountDownLatch(1),
                attempted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var archive =
                    executor.submit(
                            () -> {
                                authenticate();
                                try {
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        projects.archive(project.id(), 0);
                                                        archived.countDown();
                                                        try {
                                                            if (!release.await(
                                                                    10, TimeUnit.SECONDS))
                                                                throw new IllegalStateException(
                                                                        "timeout");
                                                        } catch (InterruptedException ex) {
                                                            Thread.currentThread().interrupt();
                                                            throw new IllegalStateException(ex);
                                                        }
                                                    });
                                } finally {
                                    TenantContext.clear();
                                    SecurityContextHolder.clearContext();
                                }
                            });
            assertThat(archived.await(10, TimeUnit.SECONDS)).isTrue();
            var create =
                    executor.submit(
                            () -> {
                                authenticate();
                                attempted.countDown();
                                try {
                                    return tasks.create(
                                            project.id(),
                                            new TaskModels.CreateRequest(
                                                    "Late", null, null, null, null));
                                } finally {
                                    TenantContext.clear();
                                    SecurityContextHolder.clearContext();
                                }
                            });
            assertThat(attempted.await(10, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            archive.get(15, TimeUnit.SECONDS);
            assertThatThrownBy(() -> create.get(15, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(com.fastvue.common.exception.BusinessException.class);
        } finally {
            release.countDown();
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM biz_task WHERE project_id=?",
                                Integer.class,
                                project.id()))
                .isZero();
    }

    @Test
    void databaseRejectsCrossTenantRelationsEvenForRawSql() {
        Long tenant =
                jdbc.queryForObject(
                        "INSERT INTO sys_tenant(name,code) VALUES ('Other',?) RETURNING id",
                        Long.class,
                        UUID.randomUUID().toString());
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES (?,1,1)",
                                        tenant))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Autowired com.fastvue.module.content.persistence.ArticleMapper articles;
    @Autowired com.fastvue.module.user.persistence.UserMapper users;

    @Test
    void articleJsonRoundTripsAndAuditTimestampChangesOnUpdate() {
        authenticate();
        var article = new com.fastvue.module.content.persistence.ArticleEntity();
        article.setTenantId(1L);
        article.setTitle("Roundtrip");
        article.setStatus("draft");
        article.setContent(List.of("first", "second"));
        article.setTags(List.of("java"));
        articles.insert(article);
        var loaded = articles.selectById(article.getId());
        assertThat(loaded.getContent()).containsExactly("first", "second");
        assertThat(loaded.getTags()).containsExactly("java");
        var old = java.time.OffsetDateTime.parse("2020-01-01T00:00:00Z");
        loaded.setUpdatedAt(old);
        loaded.setTitle("Updated");
        articles.updateById(loaded);
        assertThat(articles.selectById(article.getId()).getUpdatedAt()).isAfter(old);
    }

    @Test
    void pagedEndpointsValidateBoundsAndKeepExistingArrays() {
        var headers = new HttpHeaders();
        headers.setBearerAuth((String) login().get("accessToken"));
        for (String path :
                List.of(
                        "/api/v1/projects/page",
                        "/api/v1/approvals/page",
                        "/api/v1/notifications/page")) {
            var page =
                    http.exchange(
                            path + "?pageSize=2",
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            String.class);
            assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(page.getBody()).contains("\"items\"", "\"total\"", "\"pageSize\":2");
            assertThat(
                            http.exchange(
                                            path + "?pageSize=101",
                                            HttpMethod.GET,
                                            new HttpEntity<>(headers),
                                            String.class)
                                    .getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(
                        http.exchange(
                                        "/api/v1/projects",
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers),
                                        String.class)
                                .getBody())
                .contains("\"data\":[");
        assertThat(
                        http.getForEntity(
                                        "/api/v1/public/blog/1/comments/page?page=0", String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Autowired com.fastvue.module.approval.service.ApprovalService approvals;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.fastvue.module.notification.service.NotificationService notifications;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.fastvue.module.notification.persistence.NotificationOutboxRepository outbox;

    @Autowired
    com.fastvue.module.notification.service.NotificationOutboxDispatcher notificationDispatcher;

    @Autowired org.springframework.context.ApplicationEventPublisher events;
    @Autowired org.springframework.security.web.FilterChainProxy securityChain;

    @Test
    void pagingRetainsResourceScopeAndLegacyListsRejectOversizedResults() {
        authenticate();
        var project =
                projects.create(
                        new ProjectModels.CreateRequest(
                                "Scoped", UUID.randomUUID().toString(), null, null, null));
        Long actor =
                jdbc.queryForObject(
                        "INSERT INTO sys_user(tenant_id,username,password,status) VALUES (1,?, 'unused','active') RETURNING id",
                        Long.class,
                        UUID.randomUUID().toString());
        var user = new LoginUser(actor, 1L, "paging-user", "unused", true, List.of(), List.of());
        TenantContext.set(1L, false);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        assertThat(projects.page(new com.fastvue.common.response.PageRequest(1L, 10L)).items())
                .extracting(ProjectModels.ProjectVO::id)
                .doesNotContain(project.id());
        assertThat(approvals.page(new com.fastvue.common.response.PageRequest(1L, 10L)).items())
                .isEmpty();
        jdbc.update(
                "INSERT INTO sys_notification(tenant_id,receiver_id,type,title) SELECT 1,?,'TEST','paging' FROM generate_series(1,1001)",
                actor);
        assertThat(notifications.page(new com.fastvue.common.response.PageRequest(2L, 10L)).total())
                .isEqualTo(1001);
        assertThatThrownBy(notifications::list)
                .isInstanceOf(com.fastvue.common.exception.BusinessException.class)
                .hasMessageContaining("1000");
    }

    @Test
    void bcryptByteLimitReturnsFriendlyValidationError() {
        var response =
                http.postForEntity(
                        "/api/v1/auth/register",
                        Map.of(
                                "username",
                                "byte-limit",
                                "email",
                                "test@example.com",
                                "password",
                                "密".repeat(25)),
                        String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("72").doesNotContain("Exception");
    }

    @Autowired com.fastvue.module.user.service.UserService userService;

    @Test
    void tenantAdministratorCannotAssignRolesWithPermissionsTheyDoNotHold() {
        Long target =
                jdbc.queryForObject(
                        "INSERT INTO sys_user(tenant_id,username,password,status) VALUES (1,?, 'unused','active') RETURNING id",
                        Long.class,
                        UUID.randomUUID().toString());
        Long role =
                jdbc.queryForObject(
                        "INSERT INTO sys_role(tenant_id,name,code) VALUES (1,'Restricted',?) RETURNING id",
                        Long.class,
                        UUID.randomUUID().toString());
        Long permission =
                jdbc.queryForObject(
                        "SELECT id FROM sys_permission WHERE code='role:create'", Long.class);
        jdbc.update(
                "INSERT INTO sys_role_permission(tenant_id,role_id,permission_id) VALUES (1,?,?)",
                role,
                permission);
        TenantContext.set(1L, false);
        var user =
                new LoginUser(
                        target,
                        1L,
                        "limited-admin",
                        "unused",
                        true,
                        List.of("tenant-admin"),
                        List.of("user:update"),
                        List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        assertThatThrownBy(
                        () ->
                                userService.update(
                                        target,
                                        new com.fastvue.module.user.api.UpdateUserRequest(
                                                null, null, null, null, null, List.of(role))))
                .isInstanceOf(com.fastvue.common.exception.BusinessException.class)
                .hasMessageContaining("不能分配");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_user_role WHERE user_id=?",
                                Integer.class,
                                target))
                .isZero();
    }

    @Test
    void archivedProjectRejectsTaskEditsWithoutChangingDatabaseOrActivity() {
        authenticate();
        var project =
                projects.create(
                        new ProjectModels.CreateRequest(
                                "Archived edit regression",
                                UUID.randomUUID().toString(),
                                null,
                                null,
                                null));
        var task =
                tasks.create(
                        project.id(),
                        new TaskModels.CreateRequest("Original title", null, null, null, null));
        projects.archive(project.id(), project.version());
        Long activitiesBefore =
                jdbc.queryForObject(
                        "SELECT count(*) FROM biz_task_activity WHERE task_id=?",
                        Long.class,
                        task.id());

        assertThatThrownBy(
                        () ->
                                tasks.update(
                                        task.id(),
                                        new TaskModels.UpdateRequest(
                                                "Changed title",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                task.version())))
                .isInstanceOf(com.fastvue.common.exception.BusinessException.class)
                .extracting(
                        ex -> ((com.fastvue.common.exception.BusinessException) ex).getErrorCode())
                .isEqualTo(com.fastvue.common.exception.ErrorCode.ILLEGAL_STATE_TRANSITION);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT title FROM biz_task WHERE id=?", String.class, task.id()))
                .isEqualTo("Original title");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM biz_task_activity WHERE task_id=?",
                                Long.class,
                                task.id()))
                .isEqualTo(activitiesBefore);
    }

    @Test
    void leaderCannotBeMovedIndirectlyAndMembershipSurvivesRejectedCommands() {
        authenticate();
        Long leaderId =
                jdbc.queryForObject(
                        "INSERT INTO sys_user(tenant_id,username,password,status) VALUES (1,?, 'unused','active') RETURNING id",
                        Long.class,
                        UUID.randomUUID().toString());
        var original =
                departments.create(
                        new com.fastvue.module.organization.api.DepartmentModels.SaveRequest(
                                null,
                                "Original department",
                                UUID.randomUUID().toString(),
                                leaderId,
                                0,
                                "active"));
        var destination =
                departments.create(
                        new com.fastvue.module.organization.api.DepartmentModels.SaveRequest(
                                null,
                                "Destination department",
                                UUID.randomUUID().toString(),
                                null,
                                0,
                                "active"));

        assertThatThrownBy(() -> departments.assignMember(destination.id(), leaderId))
                .isInstanceOf(com.fastvue.common.exception.BusinessException.class);
        assertThatThrownBy(
                        () ->
                                departments.update(
                                        destination.id(),
                                        new com.fastvue.module.organization.api.DepartmentModels
                                                .SaveRequest(
                                                null,
                                                "Destination department",
                                                destination.code(),
                                                leaderId,
                                                0,
                                                "active")))
                .isInstanceOf(com.fastvue.common.exception.BusinessException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT department_id FROM org_department_member WHERE user_id=?",
                                Long.class,
                                leaderId))
                .isEqualTo(original.id());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT leader_id FROM org_department WHERE id=?",
                                Long.class,
                                original.id()))
                .isEqualTo(leaderId);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT leader_id FROM org_department WHERE id=?",
                                Long.class,
                                destination.id()))
                .isNull();
        // Keeping the leader in the same department must remain a valid edit.
        departments.update(
                original.id(),
                new com.fastvue.module.organization.api.DepartmentModels.SaveRequest(
                        null, "Renamed original", original.code(), leaderId, 0, "active"));
    }

    @Test
    void departmentParentAndLeaderCanBeClearedInPersistedRows() {
        authenticate();
        Long leader = newUser();
        var parent = newDepartment(null, null);
        var child = newDepartment(parent.id(), leader);
        departments.update(
                child.id(),
                new com.fastvue.module.organization.api.DepartmentModels.SaveRequest(
                        null, "Moved to root", child.code(), null, 0, "active"));
        var row =
                jdbc.queryForMap(
                        "SELECT parent_id, leader_id FROM org_department WHERE id=?", child.id());
        assertThat(row.get("parent_id")).isNull();
        assertThat(row.get("leader_id")).isNull();
        // Clearing leadership does not implicitly remove the former leader's membership.
        assertThat(
                        jdbc.queryForObject(
                                "SELECT department_id FROM org_department_member WHERE user_id=?",
                                Long.class,
                                leader))
                .isEqualTo(child.id());
    }

    @Test
    void simultaneousInverseDepartmentMovesCannotCreateACycle() throws Exception {
        authenticate();
        var a = newDepartment(null, null);
        var b = newDepartment(null, null);
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var started = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first =
                    pool.submit(
                            () -> {
                                TenantContext.set(1L, false);
                                try {
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        jdbc.queryForObject(
                                                                "SELECT id FROM org_organization WHERE tenant_id=1 FOR UPDATE",
                                                                Long.class);
                                                        locked.countDown();
                                                        awaitLatch(release);
                                                        departments.update(
                                                                a.id(),
                                                                new com.fastvue.module.organization
                                                                        .api.DepartmentModels
                                                                        .SaveRequest(
                                                                        b.id(), a.name(), a.code(),
                                                                        null, 0, "active"));
                                                    });
                                } finally {
                                    TenantContext.clear();
                                }
                            });
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            var second =
                    pool.submit(
                            () -> {
                                TenantContext.set(1L, false);
                                started.countDown();
                                try {
                                    departments.update(
                                            b.id(),
                                            new com.fastvue.module.organization.api.DepartmentModels
                                                    .SaveRequest(
                                                    a.id(), b.name(), b.code(), null, 0, "active"));
                                    return "accepted";
                                } catch (com.fastvue.common.exception.BusinessException expected) {
                                    return "rejected";
                                } finally {
                                    TenantContext.clear();
                                }
                            });
            try {
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
            } finally {
                release.countDown();
            }
            first.get(5, TimeUnit.SECONDS);
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("rejected");
        } finally {
            release.countDown();
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT parent_id FROM org_department WHERE id=?",
                                Long.class,
                                a.id()))
                .isEqualTo(b.id());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT parent_id FROM org_department WHERE id=?",
                                Long.class,
                                b.id()))
                .isNull();
    }

    @Test
    void concurrentLeaderClaimsLeaveOneLeaderAndMatchingMembership() throws Exception {
        authenticate();
        Long leader = newUser();
        var a = newDepartment(null, null);
        var b = newDepartment(null, null);
        var start = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var results = new ArrayList<Future<Boolean>>();
            for (var department : List.of(a, b))
                results.add(
                        pool.submit(
                                () -> {
                                    TenantContext.set(1L, false);
                                    try {
                                        start.await(5, TimeUnit.SECONDS);
                                        departments.update(
                                                department.id(),
                                                new com.fastvue.module.organization.api
                                                        .DepartmentModels.SaveRequest(
                                                        null,
                                                        department.name(),
                                                        department.code(),
                                                        leader,
                                                        0,
                                                        "active"));
                                        return true;
                                    } catch (
                                            com.fastvue.common.exception.BusinessException
                                                    expected) {
                                        return false;
                                    } finally {
                                        TenantContext.clear();
                                    }
                                }));
            assertThat(
                            List.of(
                                    results.get(0).get(5, TimeUnit.SECONDS),
                                    results.get(1).get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        Long owner =
                jdbc.queryForObject(
                        "SELECT id FROM org_department WHERE leader_id=? AND NOT deleted",
                        Long.class,
                        leader);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT department_id FROM org_department_member WHERE user_id=?",
                                Long.class,
                                leader))
                .isEqualTo(owner);
    }

    @Test
    void concurrentCategoryNameInsertsCannotBypassPreflightAndDeletedNamesCanBeReused()
            throws Exception {
        String name = "category-" + UUID.randomUUID();
        var start = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var calls = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 2; i++)
                calls.add(
                        pool.submit(
                                () -> {
                                    try {
                                        return new TransactionTemplate(transactionManager)
                                                .execute(
                                                        status -> {
                                                            assertThat(
                                                                            jdbc.queryForObject(
                                                                                    "SELECT count(*) FROM content_category WHERE tenant_id=1 AND name=? AND NOT deleted",
                                                                                    Long.class,
                                                                                    name))
                                                                    .isZero();
                                                            awaitBarrier(start);
                                                            jdbc.update(
                                                                    "INSERT INTO content_category(tenant_id,name,slug) VALUES (1,?,?)",
                                                                    name,
                                                                    UUID.randomUUID().toString());
                                                            return true;
                                                        });
                                    } catch (
                                            org.springframework.dao.DataIntegrityViolationException
                                                    expected) {
                                        return false;
                                    }
                                }));
            assertThat(
                            List.of(
                                    calls.get(0).get(5, TimeUnit.SECONDS),
                                    calls.get(1).get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        jdbc.update("UPDATE content_category SET deleted=true WHERE tenant_id=1 AND name=?", name);
        jdbc.update(
                "INSERT INTO content_category(tenant_id,name,slug) VALUES (1,?,?)",
                name,
                UUID.randomUUID().toString());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM content_category WHERE tenant_id=1 AND name=? AND NOT deleted",
                                Long.class,
                                name))
                .isEqualTo(1);
    }

    @Test
    void configuredDatabaseBudgetsApplyAndCancelledQueriesReleaseTheConnection() {
        assertThat(jdbc.queryForObject("SHOW statement_timeout", String.class)).isEqualTo("15s");
        assertThat(jdbc.queryForObject("SHOW lock_timeout", String.class)).isEqualTo("5s");
        assertThat(
                        ((org.springframework.transaction.support
                                                .AbstractPlatformTransactionManager)
                                        transactionManager)
                                .getDefaultTimeout())
                .isEqualTo(20);
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(transactionManager)
                                        .executeWithoutResult(
                                                status -> {
                                                    jdbc.execute(
                                                            "SET LOCAL statement_timeout = '100ms'");
                                                    jdbc.execute("SELECT pg_sleep(2)");
                                                }))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
    }

    @Test
    void lockWaitsAreBoundedWithoutPoisoningFollowingTransactions() throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var owner =
                    pool.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        jdbc.queryForObject(
                                                                "SELECT id FROM org_organization WHERE tenant_id=1 FOR UPDATE",
                                                                Long.class);
                                                        locked.countDown();
                                                        awaitLatch(release);
                                                    }));
            try {
                assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(
                                () ->
                                        new TransactionTemplate(transactionManager)
                                                .executeWithoutResult(
                                                        status -> {
                                                            jdbc.execute(
                                                                    "SET LOCAL lock_timeout = '100ms'");
                                                            jdbc.queryForObject(
                                                                    "SELECT id FROM org_organization WHERE tenant_id=1 FOR UPDATE",
                                                                    Long.class);
                                                        }))
                        .isInstanceOf(org.springframework.dao.DataAccessException.class);
            } finally {
                release.countDown();
            }
            owner.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
        }
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
    }

    @Test
    void rateLimitRunsAfterCorsAndBeforeJwtDatabaseAuthentication() {
        var filters = securityChain.getFilterChains().getFirst().getFilters();
        int cors = filterIndex(filters, org.springframework.web.filter.CorsFilter.class);
        int limit = filterIndex(filters, com.fastvue.security.RateLimitFilter.class);
        int jwt = filterIndex(filters, com.fastvue.security.JwtAuthenticationFilter.class);
        assertThat(cors).isLessThan(limit);
        assertThat(limit).isLessThan(jwt);
        redis.opsForValue().set("rate:api:127.0.0.1", "300", java.time.Duration.ofMinutes(1));
        var headers = new HttpHeaders();
        headers.setBearerAuth("invalid-token");
        headers.setOrigin("https://app.example.com");
        var response =
                http.exchange(
                        "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("https://app.example.com");
    }

    @Test
    void missingMultipartFileIsAClientErrorOverHttp() {
        var headers = new HttpHeaders();
        headers.setBearerAuth((String) login().get("accessToken"));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var body = new org.springframework.util.LinkedMultiValueMap<String, Object>();
        body.add("description", "no file part");
        var response =
                http.postForEntity("/api/v1/files", new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void notificationsCommitWithBusinessAndRollbackLeavesNoOutboxRow() {
        authenticate();
        String title = "commit-" + UUID.randomUUID();
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                events.publishEvent(
                                        new com.fastvue.module.task.event.TaskAssignedEvent(
                                                1L, 100L, 1L, title)));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_notification WHERE content LIKE ?",
                                Long.class,
                                "%" + title + "%"))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM sys_notification_outbox WHERE content LIKE ?",
                                String.class,
                                "%" + title + "%"))
                .isEqualTo("DELIVERED");
        String rolledBack = "rollback-" + UUID.randomUUID();
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(transactionManager)
                                        .executeWithoutResult(
                                                status -> {
                                                    events.publishEvent(
                                                            new com.fastvue.module.task.event
                                                                    .TaskAssignedEvent(
                                                                    1L, 101L, 1L, rolledBack));
                                                    throw new IllegalStateException("rollback");
                                                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_notification_outbox WHERE content LIKE ?",
                                Long.class,
                                "%" + rolledBack + "%"))
                .isZero();
    }

    @Test
    void outboxFailureRollsBackBusinessInsteadOfLosingTheEvent() {
        authenticate();
        String code = UUID.randomUUID().toString();
        org.mockito.Mockito.doThrow(
                        new org.springframework.dao.DataAccessResourceFailureException("test"))
                .when(outbox)
                .enqueue(org.mockito.ArgumentMatchers.any());
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(transactionManager)
                                        .executeWithoutResult(
                                                status -> {
                                                    jdbc.update(
                                                            "INSERT INTO content_category(tenant_id,name,slug) VALUES (1,?,?)",
                                                            code,
                                                            code);
                                                    events.publishEvent(
                                                            new com.fastvue.module.task.event
                                                                    .TaskAssignedEvent(
                                                                    1L, 101L, 1L, code));
                                                }))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM content_category WHERE slug=?",
                                Long.class,
                                code))
                .isZero();
    }

    @Test
    void notificationFailureRemainsDurableAndRetriesIdempotently() {
        authenticate();
        String title = "retry-" + UUID.randomUUID();
        org.mockito.Mockito.doThrow(new IllegalStateException("failure must not reach API"))
                .when(notificationTarget())
                .deliver(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                events.publishEvent(
                                        new com.fastvue.module.task.event.TaskAssignedEvent(
                                                1L, 102L, 1L, title)));
        UUID id =
                jdbc.queryForObject(
                        "SELECT id FROM sys_notification_outbox WHERE content LIKE ?",
                        UUID.class,
                        "%" + title + "%");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT attempts FROM sys_notification_outbox WHERE id=?",
                                Integer.class,
                                id))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT last_error FROM sys_notification_outbox WHERE id=?",
                                String.class,
                                id))
                .isEqualTo("IllegalStateException");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_notification WHERE content LIKE ?",
                                Long.class,
                                "%" + title + "%"))
                .isZero();
        org.mockito.Mockito.doCallRealMethod()
                .when(notificationTarget())
                .deliver(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        jdbc.update("UPDATE sys_notification_outbox SET next_attempt_at=now() WHERE id=?", id);
        notificationDispatcher.dispatchPending();
        notificationDispatcher.tryDeliver(id);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM sys_notification_outbox WHERE id=?",
                                String.class,
                                id))
                .isEqualTo("DELIVERED");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_notification WHERE content LIKE ?",
                                Long.class,
                                "%" + title + "%"))
                .isEqualTo(1);
    }

    @Test
    void acknowledgementFailureRollsBackNotificationInsertAndCanRecoverAfterRestart() {
        authenticate();
        var message = pendingNotification();
        org.mockito.Mockito.doThrow(new IllegalStateException("ack fault"))
                .when(outbox)
                .delivered(message.id());
        notificationDispatcher.tryDeliver(message.id());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_notification WHERE title=?",
                                Long.class,
                                message.title()))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM sys_notification_outbox WHERE id=?",
                                String.class,
                                message.id()))
                .isEqualTo("PENDING");
        org.mockito.Mockito.doCallRealMethod().when(outbox).delivered(message.id());
        jdbc.update(
                "UPDATE sys_notification_outbox SET next_attempt_at=now() WHERE id=?",
                message.id());
        // Reconstructed worker has no in-memory event state, as after process restart.
        var restarted =
                new com.fastvue.module.notification.service.NotificationOutboxDispatcher(
                        outbox,
                        notifications,
                        new com.fastvue.config.NotificationDeliveryProperties(20, 10),
                        transactionManager);
        restarted.dispatchPending();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_notification WHERE title=?",
                                Long.class,
                                message.title()))
                .isEqualTo(1);
    }

    @Test
    void exhaustedNotificationsRemainInspectableAndCanBeRequeued() {
        authenticate();
        var message = pendingNotification();
        org.mockito.Mockito.doThrow(new IllegalStateException("permanent"))
                .when(notificationTarget())
                .deliver(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        var worker =
                new com.fastvue.module.notification.service.NotificationOutboxDispatcher(
                        outbox,
                        notifications,
                        new com.fastvue.config.NotificationDeliveryProperties(20, 2),
                        transactionManager);
        worker.tryDeliver(message.id());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT next_attempt_at > now() FROM sys_notification_outbox WHERE id=?",
                                Boolean.class,
                                message.id()))
                .isTrue();
        jdbc.update(
                "UPDATE sys_notification_outbox SET next_attempt_at=now() WHERE id=?",
                message.id());
        worker.tryDeliver(message.id());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM sys_notification_outbox WHERE id=?",
                                String.class,
                                message.id()))
                .isEqualTo("FAILED");
        assertThat(outbox.pendingIds(100)).doesNotContain(message.id());
        org.mockito.Mockito.doCallRealMethod()
                .when(notificationTarget())
                .deliver(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        jdbc.update(
                "UPDATE sys_notification_outbox SET status='PENDING', attempts=0, next_attempt_at=now() WHERE id=? AND status='FAILED'",
                message.id());
        worker.tryDeliver(message.id());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_notification WHERE title=?",
                                Long.class,
                                message.title()))
                .isEqualTo(1);
    }

    @Test
    void competingNotificationWorkersDeliverOneRowExactlyOnce() throws Exception {
        authenticate();
        var message = pendingNotification();
        var start = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var one =
                    pool.submit(
                            () -> {
                                awaitBarrier(start);
                                notificationDispatcher.tryDeliver(message.id());
                            });
            var two =
                    pool.submit(
                            () -> {
                                awaitBarrier(start);
                                notificationDispatcher.tryDeliver(message.id());
                            });
            one.get(5, TimeUnit.SECONDS);
            two.get(5, TimeUnit.SECONDS);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM sys_notification WHERE title=?",
                                Long.class,
                                message.title()))
                .isEqualTo(1);
    }

    @Test
    void multipartAllowsTheFullFileBudgetAndRejectsOneExtraByte() {
        var headers = new HttpHeaders();
        headers.setBearerAuth((String) login().get("accessToken"));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        byte[] bytes = new byte[10 * 1024 * 1024];
        Arrays.fill(bytes, (byte) 'a');
        var resource =
                new org.springframework.core.io.ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        return "budget.txt";
                    }
                };
        var body = new org.springframework.util.LinkedMultiValueMap<String, Object>();
        body.add("file", resource);
        var response =
                http.postForEntity("/api/v1/files", new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            String json = response.getBody();
            assertThat(json).contains("10485760");
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String key = mapper.readTree(json).path("data").path("key").asText();
            var deleteHeaders = new HttpHeaders();
            deleteHeaders.setBearerAuth(headers.getFirst(HttpHeaders.AUTHORIZATION).substring(7));
            assertThat(
                            http.exchange(
                                            "/api/v1/files/" + key,
                                            HttpMethod.DELETE,
                                            new HttpEntity<>(deleteHeaders),
                                            String.class)
                                    .getStatusCode())
                    .isEqualTo(HttpStatus.OK);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
        var oversized =
                new org.springframework.core.io.ByteArrayResource(new byte[bytes.length + 1]) {
                    @Override
                    public String getFilename() {
                        return "too-large.txt";
                    }
                };
        body.set("file", oversized);
        assertThat(
                        http.postForEntity(
                                        "/api/v1/files",
                                        new HttpEntity<>(body, headers),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void backgroundDeliveryUsesPayloadTenantAndCategoryNamesRemainTenantScoped() {
        authenticate();
        Long otherTenant =
                jdbc.queryForObject(
                        "INSERT INTO sys_tenant(name,code) VALUES ('Other tenant',?) RETURNING id",
                        Long.class,
                        UUID.randomUUID().toString());
        Long receiver =
                jdbc.queryForObject(
                        "INSERT INTO sys_user(tenant_id,username,password,status) VALUES (?,?,'unused','active') RETURNING id",
                        Long.class,
                        otherTenant,
                        UUID.randomUUID().toString());
        var message =
                new com.fastvue.module.notification.persistence.NotificationOutboxRepository
                        .Message(
                        UUID.randomUUID(),
                        otherTenant,
                        receiver,
                        "TEST",
                        "tenant-" + UUID.randomUUID(),
                        "content",
                        0);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> outbox.enqueue(message));
        notificationDispatcher.tryDeliver(message.id());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT tenant_id FROM sys_notification WHERE title=?",
                                Long.class,
                                message.title()))
                .isEqualTo(otherTenant);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT receiver_id FROM sys_notification WHERE title=?",
                                Long.class,
                                message.title()))
                .isEqualTo(receiver);
        assertThat(TenantContext.tenantId()).isEqualTo(1L);
        assertThat(TenantContext.isSuperAdmin()).isTrue();
        String name = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO content_category(tenant_id,name,slug) VALUES (1,?,?),(?,?,?)",
                name,
                name,
                otherTenant,
                name,
                name);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM content_category WHERE name=? AND NOT deleted",
                                Long.class,
                                name))
                .isEqualTo(2);
    }

    private com.fastvue.module.notification.service.NotificationService notificationTarget() {
        return org.springframework.test.util.AopTestUtils.getUltimateTargetObject(notifications);
    }

    private com.fastvue.module.notification.persistence.NotificationOutboxRepository.Message
            pendingNotification() {
        var message =
                new com.fastvue.module.notification.persistence.NotificationOutboxRepository
                        .Message(
                        UUID.randomUUID(),
                        1L,
                        1L,
                        "TEST",
                        "outbox-" + UUID.randomUUID(),
                        "test",
                        0);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> outbox.enqueue(message));
        return message;
    }

    private Long newUser() {
        return jdbc.queryForObject(
                "INSERT INTO sys_user(tenant_id,username,password,status) VALUES (1,?,'unused','active') RETURNING id",
                Long.class,
                UUID.randomUUID().toString());
    }

    private com.fastvue.module.organization.api.DepartmentModels.DepartmentVO newDepartment(
            Long parent, Long leader) {
        return departments.create(
                new com.fastvue.module.organization.api.DepartmentModels.SaveRequest(
                        parent, "Department", UUID.randomUUID().toString(), leader, 0, "active"));
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("latch timeout");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int filterIndex(List<jakarta.servlet.Filter> filters, Class<?> type) {
        for (int i = 0; i < filters.size(); i++) if (type.isInstance(filters.get(i))) return i;
        throw new AssertionError("Missing filter " + type.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> login() {
        var response =
                http.postForEntity(
                        "/api/v1/auth/login",
                        Map.of("username", "admin", "password", "production-test-password"),
                        Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (Map<String, Object>) response.getBody().get("data");
    }

    private void authenticate() {
        TenantContext.set(1L, true);
        var user = new LoginUser(1L, 1L, "admin", "unused", true, List.of("admin"), List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
