package com.fastvue.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {
    private final Counter projectCreated;
    private final Counter taskCreated;
    private final Counter approvalSubmitted;
    private final Counter approvalRejected;

    public BusinessMetrics(MeterRegistry registry) {
        projectCreated = registry.counter("project_created_total");
        taskCreated = registry.counter("task_created_total");
        approvalSubmitted = registry.counter("approval_submitted_total");
        approvalRejected = registry.counter("approval_rejected_total");
    }

    public void projectCreated() {
        afterCommit(projectCreated);
    }

    public void taskCreated() {
        afterCommit(taskCreated);
    }

    public void approvalSubmitted() {
        afterCommit(approvalSubmitted);
    }

    public void approvalRejected() {
        afterCommit(approvalRejected);
    }

    private void afterCommit(Counter counter) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager
                .isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .registerSynchronization(
                            new org.springframework.transaction.support
                                    .TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    counter.increment();
                                }
                            });
        } else {
            counter.increment();
        }
    }
}
