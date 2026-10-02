package com.fastvue.module.role;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.role.api.*;
import com.fastvue.module.role.persistence.*;
import com.fastvue.module.role.service.*;
import com.fastvue.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RoleGrantTest {
    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void duplicateGrantsAreNormalizedOnCreateAndUpdate() {
        TenantContext.set(1L, true);
        RoleMapper mapper = mock(RoleMapper.class);
        var role = new RoleEntity();
        role.setId(8L);
        role.setTenantId(1L);
        role.setCode("operator");
        when(mapper.selectById(8L)).thenReturn(role);
        doAnswer(
                        invocation -> {
                            ((RoleEntity) invocation.getArgument(0)).setId(8L);
                            return 1;
                        })
                .when(mapper)
                .insert(any(RoleEntity.class));
        var service =
                new RoleService(
                        mapper, org.mapstruct.factory.Mappers.getMapper(RoleConverter.class));
        service.create(
                new CreateRoleRequest(
                        "operator", "Operator", null, List.of(7L, 7L), List.of(9L, 9L)));
        service.update(8L, new UpdateRoleRequest(null, null, List.of(7L, 7L), List.of(9L, 9L)));
        verify(mapper, times(2)).insertRolePermissions(1L, 8L, List.of(7L));
        verify(mapper, times(2)).insertRoleMenus(1L, 8L, List.of(9L));
    }

    @Test
    void roleEditorCannotGrantAuthorityTheyDoNotHave() {
        TenantContext.set(2L, false);
        var user =
                new LoginUser(
                        5L, 2L, "editor", "x", true, List.of(), List.of("role:update"), List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        RoleMapper mapper = mock(RoleMapper.class);
        var role = new RoleEntity();
        role.setId(8L);
        role.setCode("operator");
        role.setTenantId(2L);
        when(mapper.selectById(8L)).thenReturn(role);
        when(mapper.selectPermissionCodesByIds(List.of(99L))).thenReturn(List.of("user:update"));
        var service = new RoleService(mapper, mock(RoleConverter.class));
        assertThatThrownBy(
                        () ->
                                service.update(
                                        8L, new UpdateRoleRequest(null, null, List.of(99L), null)))
                .hasMessageContaining("不能授予");
        verify(mapper, never()).updateById(any(RoleEntity.class));
        verify(mapper, never()).insertRolePermissions(any(), any(), any());
    }
}
