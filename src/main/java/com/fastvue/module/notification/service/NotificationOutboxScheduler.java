package com.fastvue.module.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notifications",
        name = "delivery-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class NotificationOutboxScheduler {
    private final NotificationOutboxDispatcher dispatcher;

    @Scheduled(
            fixedDelayString = "${app.notifications.poll-interval-ms:5000}",
            initialDelayString = "${app.notifications.poll-interval-ms:5000}")
    public void dispatch() {
        try {
            dispatcher.dispatchPending();
        } catch (RuntimeException ex) {
            log.error("Notification queue polling failed ({})", ex.getClass().getSimpleName());
        }
    }
}
