package com.fastvue.module.approval.event;

public record ApprovalSubmittedEvent(Long tenantId, Long approvalId, Long receiverId, String title) {}
