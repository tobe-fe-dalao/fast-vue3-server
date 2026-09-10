package com.fastvue.module.approval;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.metrics.BusinessMetrics;
import com.fastvue.module.approval.persistence.*;
import com.fastvue.module.approval.service.ApprovalService;
import com.fastvue.module.organization.persistence.DepartmentMapper;
import com.fastvue.module.user.persistence.UserMapper;
import com.fastvue.security.LoginUser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowTest {
    @Mock ApprovalRequestMapper requests; @Mock ApprovalStepMapper steps; @Mock ApprovalActionMapper actions;
    @Mock DepartmentMapper departments; @Mock UserMapper users; @Mock ApplicationEventPublisher publisher;
    @Mock BusinessMetrics metrics;

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void approvedRequestCannotReturnToPendingOrBeCancelled() {
        ApprovalService service = new ApprovalService(requests, steps, actions, departments, users, publisher, metrics);
        ApprovalRequestEntity request = new ApprovalRequestEntity();
        request.setId(1L); request.setApplicantId(8L); request.setStatus("APPROVED");
        when(requests.selectById(1L)).thenReturn(request);
        LoginUser user = new LoginUser(8L, 1L, "applicant", "x", true, List.of(), List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ILLEGAL_STATE_TRANSITION);
    }

    @Test void departmentManagerThenTenantAdministratorCompleteTwoStepApproval() {
        ApprovalService service = new ApprovalService(requests, steps, actions, departments, users, publisher, metrics);
        ApprovalRequestEntity request = new ApprovalRequestEntity();
        request.setId(10L); request.setTenantId(1L); request.setApplicantId(8L);
        request.setTitle("Purchase request"); request.setStatus("PENDING"); request.setCurrentStep(1); request.setVersion(0);

        ApprovalStepEntity managerStep = new ApprovalStepEntity();
        managerStep.setId(11L); managerStep.setApproverType("DEPARTMENT_MANAGER");
        managerStep.setApproverId(2L); managerStep.setStepOrder(1); managerStep.setStatus("PENDING");
        ApprovalStepEntity adminStep = new ApprovalStepEntity();
        adminStep.setId(12L); adminStep.setApproverType("ADMIN");
        adminStep.setStepOrder(2); adminStep.setStatus("WAITING");

        when(requests.selectById(10L)).thenReturn(request);
        when(requests.updateById(request)).thenReturn(1);
        when(steps.selectOne(any())).thenReturn(managerStep, adminStep, adminStep, null);
        when(steps.selectList(any())).thenReturn(List.of(managerStep, adminStep));

        authenticate(new LoginUser(2L, 1L, "manager", "x", true, List.of("employee"), List.of()));
        assertThat(service.approve(10L, "manager approved").status()).isEqualTo("PENDING");
        assertThat(request.getCurrentStep()).isEqualTo(2);
        assertThat(adminStep.getStatus()).isEqualTo("PENDING");

        authenticate(new LoginUser(3L, 1L, "tenant-admin", "x", true,
                List.of("tenant-admin"), List.of("approval:action"), List.of()));
        assertThat(service.approve(10L, "admin approved").status()).isEqualTo("APPROVED");
        verify(requests, org.mockito.Mockito.times(2)).updateById(request);
        verify(actions, org.mockito.Mockito.times(2)).insert(any(ApprovalActionEntity.class));
    }

    private void authenticate(LoginUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
