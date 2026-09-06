package com.fastvue.module.role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private RoleConverter roleConverter;

    private RoleService roleService;
    private RoleEntity role;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleMapper, roleConverter);
        role = new RoleEntity();
        role.setId(2L);
        role.setCode("operator");
        role.setName("运营");

        when(roleMapper.selectById(2L)).thenReturn(role);
        when(roleConverter.toVO(role)).thenReturn(
                new RoleVO(2L, "operator", "运营", null, null, null, null, null));
        when(roleMapper.selectPermissionCodes(2L)).thenReturn(List.of("user:list"));
        when(roleMapper.selectMenuIds(2L)).thenReturn(List.of(1L));
    }

    @Test
    @DisplayName("仅更新权限时保留原菜单绑定")
    void updatePermissionsKeepsMenus() {
        roleService.update(2L, new UpdateRoleRequest(null, null, List.of(1L), null));

        verify(roleMapper).deleteRolePermissions(2L);
        verify(roleMapper).insertRolePermissions(2L, List.of(1L));
        verify(roleMapper, never()).deleteRoleMenus(2L);
    }

    @Test
    @DisplayName("仅更新菜单时保留原权限绑定")
    void updateMenusKeepsPermissions() {
        roleService.update(2L, new UpdateRoleRequest(null, null, null, List.of(1L)));

        verify(roleMapper).deleteRoleMenus(2L);
        verify(roleMapper).insertRoleMenus(2L, List.of(1L));
        verify(roleMapper, never()).deleteRolePermissions(2L);
    }
}
