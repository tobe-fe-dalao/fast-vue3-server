package com.fastvue.module.menu;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.menu.api.*;
import com.fastvue.module.menu.persistence.*;
import com.fastvue.module.menu.service.*;
import com.fastvue.module.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {
    @Mock MenuMapper mapper;
    @Mock UserService users;
    MenuService service;

    @BeforeEach
    void setup() {
        TenantContext.set(1L, true);
        service =
                new MenuService(
                        mapper,
                        org.mapstruct.factory.Mappers.getMapper(MenuConverter.class),
                        users);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    MenuEntity menu(long id, long parent, int sort) {
        var value = new MenuEntity();
        value.setId(id);
        value.setParentId(parent);
        value.setName("Menu" + id);
        value.setSort(sort);
        return value;
    }

    MenuRequest request(Long parent) {
        return new MenuRequest(
                parent, "Menu", "/menu", "component", "icon", null, null, "menu:list", "menu");
    }

    @Test
    void tenantAdministratorCannotMutateGlobalMenus() {
        TenantContext.set(2L, false);
        assertThatThrownBy(() -> service.create(request(null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.update(1L, request(null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void descendantAndSelfParentAreRejected() {
        when(mapper.selectById(1L)).thenReturn(menu(1, 0, 0));
        when(mapper.selectById(2L)).thenReturn(menu(2, 1, 0));
        assertThatThrownBy(() -> service.update(1L, request(2L))).hasMessageContaining("子节点");
        assertThatThrownBy(() -> service.update(1L, request(1L))).hasMessageContaining("自己");
        verify(mapper, never()).updateById(any(MenuEntity.class));
    }

    @Test
    void missingResourcesAndParentsAreRejected() {
        assertThatThrownBy(() -> service.getById(9L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.update(9L, request(null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(request(9L))).hasMessageContaining("父菜单");
    }

    @Test
    void parentsWithChildrenCannotBeDeleted() {
        when(mapper.selectById(1L)).thenReturn(menu(1, 0, 0));
        when(mapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.delete(1L)).hasMessageContaining("子菜单");
        verify(mapper, never()).deleteById(any(Long.class));
    }

    @Test
    void rootCreateUpdateAndLeafDeleteAreSupported() {
        when(mapper.selectById(1L)).thenReturn(menu(1, 0, 0));
        assertThat(service.create(request(null)).parentId()).isZero();
        assertThat(service.update(1L, request(null)).name()).isEqualTo("Menu");
        service.delete(1L);
        verify(mapper).deleteById(1L);
    }

    @Test
    void treeNestsChildrenAndSortsRoots() {
        when(mapper.selectList(any()))
                .thenReturn(List.of(menu(1, 0, 10), menu(2, 0, 1), menu(3, 1, 0)));
        var result = service.tree();
        assertThat(result).extracting(MenuVO::id).containsExactly(2L, 1L);
        assertThat(result.get(1).children()).extracting(MenuVO::id).containsExactly(3L);
    }

    @Test
    void userWithoutMenuIdsNeedsNoDatabaseQuery() {
        when(users.listMenuIds(8L)).thenReturn(List.of());
        assertThat(service.treeForUser(8L)).isEmpty();
        assertThat(service.treeByIds(null)).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    void authorizedIdsBuildAFilteredTree() {
        when(users.listMenuIds(8L)).thenReturn(List.of(1L, 3L));
        when(mapper.selectList(any())).thenReturn(List.of(menu(1, 0, 0), menu(3, 1, 0)));
        assertThat(service.treeForUser(8L).getFirst().children())
                .extracting(MenuVO::id)
                .containsExactly(3L);
    }
}
