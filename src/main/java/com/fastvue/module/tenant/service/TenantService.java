package com.fastvue.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.tenant.api.TenantVO;
import com.fastvue.module.tenant.api.TenantModels.CreateRequest;
import com.fastvue.module.tenant.api.TenantModels.UpdateRequest;
import com.fastvue.module.tenant.persistence.TenantEntity;
import com.fastvue.module.tenant.persistence.TenantMapper;
import com.fastvue.module.organization.persistence.OrganizationEntity;
import com.fastvue.module.organization.persistence.OrganizationMapper;
import com.fastvue.module.role.persistence.RoleEntity;
import com.fastvue.module.role.persistence.RoleMapper;
import com.fastvue.module.permission.persistence.PermissionEntity;
import com.fastvue.module.permission.persistence.PermissionMapper;
import com.fastvue.module.menu.persistence.MenuEntity;
import com.fastvue.module.menu.persistence.MenuMapper;
import com.fastvue.module.user.persistence.UserEntity;
import com.fastvue.module.user.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class TenantService {
    private final TenantMapper tenantMapper;
    private final OrganizationMapper organizationMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final MenuMapper menuMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public TenantEntity requireActive(String code) {
        String effectiveCode = code == null || code.isBlank() ? "default" : code.trim();
        TenantEntity tenant = tenantMapper.selectOne(new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getCode, effectiveCode));
        return validateActive(tenant);
    }

    public TenantEntity requireActive(Long id) {
        return validateActive(tenantMapper.selectById(id));
    }

    private TenantEntity validateActive(TenantEntity tenant) {
        if (tenant == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        if (!"active".equals(tenant.getStatus())
                || tenant.getExpiredAt() != null && tenant.getExpiredAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.TENANT_DISABLED);
        }
        return tenant;
    }

    public <T> T inTenant(String code, Supplier<T> action) {
        TenantEntity tenant = requireActive(code);
        return TenantContext.runAs(tenant.getId(), false, action);
    }

    public List<TenantVO> list() {
        requireSuperAdmin();
        return tenantMapper.selectList(new LambdaQueryWrapper<TenantEntity>().orderByAsc(TenantEntity::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional
    public TenantVO create(CreateRequest request) {
        requireSuperAdmin();
        if (tenantMapper.selectCount(new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getCode, request.code())) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "租户编码已存在");
        }
        TenantEntity tenant = new TenantEntity();
        tenant.setName(request.name()); tenant.setCode(request.code()); tenant.setStatus("active");
        tenant.setPlan(request.plan()); tenant.setExpiredAt(request.expiredAt()); tenant.setCreatedAt(OffsetDateTime.now());
        tenantMapper.insert(tenant);

        TenantContext.runAs(tenant.getId(), false, () -> provision(tenant, request));
        return toVO(tenant);
    }

    @Transactional
    public TenantVO update(Long id, UpdateRequest request) {
        requireSuperAdmin();
        TenantEntity tenant = tenantMapper.selectById(id);
        if (tenant == null) throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        if (request.name() != null) tenant.setName(request.name());
        if (request.status() != null) tenant.setStatus(request.status());
        if (request.plan() != null) tenant.setPlan(request.plan());
        if (request.expiredAt() != null) tenant.setExpiredAt(request.expiredAt());
        tenantMapper.updateById(tenant);
        return toVO(tenant);
    }

    private Void provision(TenantEntity tenant, CreateRequest request) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setTenantId(tenant.getId()); organization.setName(tenant.getName());
        organization.setCode(tenant.getCode().toUpperCase(java.util.Locale.ROOT)); organization.setStatus("active");
        organizationMapper.insert(organization);

        RoleEntity role = new RoleEntity();
        role.setTenantId(tenant.getId()); role.setCode("tenant-admin"); role.setName("Tenant Administrator");
        role.setDescription("Administrator within one tenant boundary");
        roleMapper.insert(role);

        List<Long> permissionIds = permissionMapper.selectList(new LambdaQueryWrapper<PermissionEntity>()
                        .ne(PermissionEntity::getCode, "tenant:list"))
                .stream().map(PermissionEntity::getId).toList();
        if (!permissionIds.isEmpty()) roleMapper.insertRolePermissions(tenant.getId(), role.getId(), permissionIds);
        List<Long> menuIds = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>())
                .stream().map(MenuEntity::getId).toList();
        if (!menuIds.isEmpty()) roleMapper.insertRoleMenus(tenant.getId(), role.getId(), menuIds);

        UserEntity admin = new UserEntity();
        admin.setTenantId(tenant.getId()); admin.setUsername(request.adminUsername());
        admin.setPassword(passwordEncoder.encode(request.adminPassword())); admin.setNickname("Tenant Administrator");
        admin.setEmail(request.adminEmail()); admin.setStatus("active");
        userMapper.insert(admin);
        userMapper.insertUserRoles(tenant.getId(), admin.getId(), List.of(role.getId()));
        return null;
    }

    private void requireSuperAdmin() {
        if (!TenantContext.isSuperAdmin()) throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private TenantVO toVO(TenantEntity value) {
        return new TenantVO(value.getId(), value.getName(), value.getCode(), value.getStatus(),
                value.getPlan(), value.getExpiredAt(), value.getCreatedAt());
    }
}
