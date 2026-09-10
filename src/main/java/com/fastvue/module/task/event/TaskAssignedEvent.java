package com.fastvue.module.task.event;

public record TaskAssignedEvent(Long tenantId, Long taskId, Long assigneeId, String taskTitle) {
}
