package com.fastvue.module.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.organization.api.DepartmentModels.SaveRequest;
import com.fastvue.module.organization.persistence.DepartmentEntity;
import com.fastvue.module.organization.persistence.DepartmentMapper;
import com.fastvue.module.organization.persistence.OrganizationMapper;
import com.fastvue.module.organization.service.DepartmentService;
import com.fastvue.module.user.persistence.UserEntity;
import com.fastvue.module.user.persistence.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {
    @Mock DepartmentMapper departments;
    @Mock OrganizationMapper organizations;
    @Mock UserMapper users;
    DepartmentService service;
    DepartmentEntity destination;

    @BeforeEach
    void setUp() {
        service = new DepartmentService(departments, organizations, users);
        TenantContext.set(1L, false);
        destination = new DepartmentEntity();
        destination.setId(20L);
        destination.setTenantId(1L);
        destination.setOrganizationId(2L);
        destination.setLeaderId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void movingAnotherDepartmentsLeaderIsRejectedBeforeMembershipChanges() {
        when(departments.selectById(20L)).thenReturn(destination);
        when(users.selectById(7L)).thenReturn(new UserEntity());
        when(departments.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.assignMember(20L, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(departments, never()).removeUserMembership(any());
        verify(departments, never()).addMember(any(), any(), any());
    }

    @Test
    void ordinaryMemberCanMoveBetweenDepartments() {
        when(departments.selectById(20L)).thenReturn(destination);
        when(users.selectById(7L)).thenReturn(new UserEntity());

        service.assignMember(20L, 7L);

        var order = inOrder(departments);
        order.verify(departments).removeUserMembership(7L);
        order.verify(departments).addMember(1L, 20L, 7L);
        assertThat(TenantContext.tenantId()).isEqualTo(1L);
    }

    @Test
    void missingMemberCannotBeAssigned() {
        when(departments.selectById(20L)).thenReturn(destination);

        assertThatThrownBy(() -> service.assignMember(20L, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(departments, never()).removeUserMembership(any());
    }

    @Test
    void updatingLeaderCannotMoveAnotherDepartmentsLeader() {
        when(departments.selectById(20L)).thenReturn(destination);
        when(users.selectById(7L)).thenReturn(new UserEntity());
        when(departments.selectCount(any())).thenReturn(0L, 1L);

        assertThatThrownBy(
                        () ->
                                service.update(
                                        20L,
                                        new SaveRequest(
                                                null, "Engineering", "ENG", 7L, 0, "active")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(departments, never()).updateById(any(DepartmentEntity.class));
        verify(departments, never()).removeUserMembership(any());
    }

    @Test
    void retainingCurrentLeaderDoesNotRewriteMembership() {
        when(departments.selectById(20L)).thenReturn(destination);
        when(users.selectById(9L)).thenReturn(new UserEntity());

        service.update(20L, new SaveRequest(null, "Engineering", "ENG", 9L, 0, "active"));

        verify(departments).updateById(destination);
        verify(departments, never()).removeUserMembership(any());
        verify(departments, never()).addMember(any(), any(), any());
    }

    @Test
    void removingCurrentLeaderRequiresReplacementFirst() {
        when(departments.selectById(20L)).thenReturn(destination);

        assertThatThrownBy(() -> service.removeMember(20L, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(departments, never()).removeMember(any(), any());
    }

    @Test
    void creatingDepartmentCannotStealAnExistingLeader() {
        when(users.selectById(7L)).thenReturn(new UserEntity());
        when(departments.selectCount(any())).thenReturn(0L, 1L);
        assertThatThrownBy(
                        () -> service.create(new SaveRequest(null, "New", "NEW", 7L, 0, "active")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(departments, never()).insert(any(DepartmentEntity.class));
        verify(departments, never()).removeUserMembership(any());
    }

    @Test
    void leaderCanBeAssignedWithinTheirExistingDepartment() {
        when(departments.selectById(20L)).thenReturn(destination);
        when(users.selectById(9L)).thenReturn(new UserEntity());
        service.assignMember(20L, 9L);
        verify(departments).addMember(1L, 20L, 9L);
    }

    @Test
    void missingDepartmentNeverTakesALockOrWrites() {
        assertThatThrownBy(
                        () ->
                                service.update(
                                        99L,
                                        new SaveRequest(null, "Name", "code", null, null, null)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(organizations);
    }

    @Test
    void validationRereadsDepartmentAfterAcquiringTheOrganizationLock() {
        var latest = new DepartmentEntity();
        latest.setId(20L);
        latest.setTenantId(1L);
        latest.setOrganizationId(2L);
        latest.setLeaderId(7L);
        when(departments.selectById(20L)).thenReturn(destination, latest);
        assertThatThrownBy(() -> service.removeMember(20L, 7L))
                .isInstanceOf(BusinessException.class);
        var order = inOrder(departments, organizations);
        order.verify(departments).selectById(20L);
        order.verify(organizations).lockByTenantId(1L);
        order.verify(departments).selectById(20L);
        verify(departments, never()).removeMember(any(), any());
    }

    @Test
    void creatingWithMissingOrganizationDoesNotInsert() {
        assertThatThrownBy(
                        () ->
                                service.create(
                                        new SaveRequest(null, "Name", "code", null, null, null)))
                .hasMessageContaining("组织不存在");
        verify(departments, never()).insert(any(DepartmentEntity.class));
    }

    @Test
    void cyclesAndCrossOrganizationMovesAreRejected() {
        when(departments.selectById(20L)).thenReturn(destination);
        assertThatThrownBy(
                        () ->
                                service.update(
                                        20L,
                                        new SaveRequest(20L, "Name", "code", null, null, null)))
                .hasMessageContaining("自己");
        var child = new DepartmentEntity();
        child.setId(21L);
        child.setParentId(20L);
        child.setOrganizationId(2L);
        when(departments.selectById(21L)).thenReturn(child);
        assertThatThrownBy(
                        () ->
                                service.update(
                                        20L,
                                        new SaveRequest(21L, "Name", "code", null, null, null)))
                .hasMessageContaining("循环");
        child.setParentId(null);
        child.setOrganizationId(3L);
        assertThatThrownBy(
                        () ->
                                service.update(
                                        20L,
                                        new SaveRequest(21L, "Name", "code", null, null, null)))
                .hasMessageContaining("跨组织");
        verify(departments, never()).updateById(any(DepartmentEntity.class));
    }

    @Test
    void missingLeaderAndDuplicateCodeAreRejected() {
        when(departments.selectById(20L)).thenReturn(destination);
        assertThatThrownBy(
                        () ->
                                service.update(
                                        20L,
                                        new SaveRequest(null, "Name", "code", 77L, null, null)))
                .hasMessageContaining("负责人不存在");
        when(departments.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(
                        () ->
                                service.update(
                                        20L,
                                        new SaveRequest(null, "Name", "code", null, null, null)))
                .hasMessageContaining("编码已存在");
    }

    @Test
    void nonLeaderMembershipCanBeRemovedAndOnlyEmptyLeafDepartmentsDeleted() {
        when(departments.selectById(20L)).thenReturn(destination);
        service.removeMember(20L, 7L);
        verify(departments).removeMember(20L, 7L);
        when(departments.selectCount(any())).thenReturn(1L, 0L, 0L);
        assertThatThrownBy(() -> service.delete(20L)).hasMessageContaining("子部门");
        when(departments.countMembers(20L)).thenReturn(1L, 0L);
        assertThatThrownBy(() -> service.delete(20L)).hasMessageContaining("成员");
        service.delete(20L);
        verify(departments).deleteById(20L);
    }

    @Test
    void treeAndMemberReadsHandleRootOrderingAndEmptyMemberships() {
        destination.setParentId(null);
        destination.setSort(2);
        var first = new DepartmentEntity();
        first.setId(21L);
        first.setParentId(null);
        first.setSort(1);
        var child = new DepartmentEntity();
        child.setId(22L);
        child.setParentId(20L);
        child.setSort(0);
        when(departments.selectList(any()))
                .thenReturn(java.util.List.of(destination, first, child));
        var tree = service.tree();
        assertThat(tree)
                .extracting(com.fastvue.module.organization.api.DepartmentModels.DepartmentVO::id)
                .containsExactly(21L, 20L);
        assertThat(tree.get(1).children()).hasSize(1);
        when(departments.selectById(20L)).thenReturn(destination);
        assertThat(service.members(20L)).isEmpty();
        when(departments.selectMemberIds(20L)).thenReturn(java.util.List.of(7L));
        var user = new UserEntity();
        user.setId(7L);
        user.setUsername("member");
        when(users.selectBatchIds(java.util.List.of(7L))).thenReturn(java.util.List.of(user));
        assertThat(service.members(20L))
                .extracting(com.fastvue.module.organization.api.DepartmentModels.MemberVO::username)
                .containsExactly("member");
    }
}
