package com.fastvue.module.approval.event;

public record ApprovalFinishedEvent(Long tenantId, Long approvalId, Long applicantId, String title, String result) {}
