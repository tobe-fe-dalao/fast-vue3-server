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

    public void projectCreated() { projectCreated.increment(); }
    public void taskCreated() { taskCreated.increment(); }
    public void approvalSubmitted() { approvalSubmitted.increment(); }
    public void approvalRejected() { approvalRejected.increment(); }
}
