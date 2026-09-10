package com.fastvue.module.notification.service;

import com.fastvue.module.approval.event.ApprovalFinishedEvent;
import com.fastvue.module.approval.event.ApprovalSubmittedEvent;
import com.fastvue.module.task.event.TaskAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class BusinessEventNotificationListener {
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void taskAssigned(TaskAssignedEvent event) {
        deliver(event.tenantId(), event.taskId(), "TASK_ASSIGNED", "新任务指派",
                "任务「" + event.taskTitle() + "」已指派给你", event.assigneeId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void approvalSubmitted(ApprovalSubmittedEvent event) {
        deliver(event.tenantId(), event.approvalId(), "APPROVAL_PENDING", "待处理审批",
                "审批「" + event.title() + "」等待你的处理", event.receiverId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void approvalFinished(ApprovalFinishedEvent event) {
        String type = "APPROVED".equals(event.result()) ? "APPROVAL_APPROVED" : "APPROVAL_REJECTED";
        deliver(event.tenantId(), event.approvalId(), type, "审批结果",
                "审批「" + event.title() + "」结果：" + event.result(), event.applicantId());
    }

    private void deliver(Long tenantId, Long aggregateId, String type, String title,
                         String content, Long receiverId) {
        try {
            notifications.create(tenantId, type, title, content, receiverId);
        } catch (RuntimeException ex) {
            // The business transaction has committed; notification failure must not make a
            // successful command appear retryable to the caller.
            log.error("提交后通知失败 tenantId={} aggregateId={} type={} receiverId={}",
                    tenantId, aggregateId, type, receiverId, ex);
        }
    }
}
