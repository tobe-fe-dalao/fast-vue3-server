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
    private final com.fastvue.module.notification.persistence.NotificationOutboxRepository outbox;
    private final NotificationOutboxDispatcher dispatcher;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void taskAssigned(TaskAssignedEvent event) {
        deliver(
                event.tenantId(),
                "TASK_ASSIGNED",
                "新任务指派",
                "任务「" + event.taskTitle() + "」已指派给你",
                event.assigneeId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void approvalSubmitted(ApprovalSubmittedEvent event) {
        deliver(
                event.tenantId(),
                "APPROVAL_PENDING",
                "待处理审批",
                "审批「" + event.title() + "」等待你的处理",
                event.receiverId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void approvalFinished(ApprovalFinishedEvent event) {
        String type = "APPROVED".equals(event.result()) ? "APPROVAL_APPROVED" : "APPROVAL_REJECTED";
        deliver(
                event.tenantId(),
                type,
                "审批结果",
                "审批「" + event.title() + "」结果：" + event.result(),
                event.applicantId());
    }

    private void deliver(
            Long tenantId, String type, String title, String content, Long receiverId) {
        if (receiverId == null) return;
        var id = java.util.UUID.randomUUID();
        outbox.enqueue(
                new com.fastvue.module.notification.persistence.NotificationOutboxRepository
                        .Message(id, tenantId, receiverId, type, title, content, 0));
        // Preserve prompt delivery after successful commits; polling recovers crashes and failures.
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                dispatcher.tryDeliver(id);
                            }
                        });
    }
}
