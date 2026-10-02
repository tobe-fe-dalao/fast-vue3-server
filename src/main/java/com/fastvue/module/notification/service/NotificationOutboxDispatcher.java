package com.fastvue.module.notification.service;

import com.fastvue.config.NotificationDeliveryProperties;
import com.fastvue.module.notification.persistence.NotificationOutboxRepository;
import com.fastvue.module.notification.persistence.NotificationOutboxRepository.Message;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class NotificationOutboxDispatcher {
    private final NotificationOutboxRepository outbox;
    private final NotificationService notifications;
    private final NotificationDeliveryProperties properties;
    private final TransactionTemplate transaction;

    public NotificationOutboxDispatcher(
            NotificationOutboxRepository outbox,
            NotificationService notifications,
            NotificationDeliveryProperties properties,
            PlatformTransactionManager manager) {
        this.outbox = outbox;
        this.notifications = notifications;
        this.properties = properties;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void dispatchPending() {
        for (UUID id : outbox.pendingIds(properties.batchSize())) tryDeliver(id);
    }

    public void tryDeliver(UUID id) {
        var claimed = new java.util.concurrent.atomic.AtomicReference<Message>();
        try {
            transaction.executeWithoutResult(
                    status -> {
                        Message message = outbox.lockDue(id);
                        if (message == null) return;
                        claimed.set(message);
                        // Notification insert and acknowledgement share a transaction and row lock.
                        // A crash rolls back both; concurrent workers skip the locked row.
                        notifications.deliver(
                                message.tenantId(),
                                message.type(),
                                message.title(),
                                message.content(),
                                message.receiverId());
                        outbox.delivered(id);
                    });
        } catch (RuntimeException ex) {
            log.error(
                    "Notification delivery failed outboxId={} error={}",
                    id,
                    ex.getClass().getSimpleName());
            if (claimed.get() != null) {
                try {
                    transaction.executeWithoutResult(
                            status ->
                                    outbox.failed(
                                            claimed.get(),
                                            properties.maxAttempts(),
                                            ex.getClass().getSimpleName()));
                } catch (RuntimeException recovery) {
                    // The durable pending row remains available when infrastructure recovers.
                    log.error(
                            "Notification retry bookkeeping failed outboxId={} error={}",
                            id,
                            recovery.getClass().getSimpleName());
                }
            }
        }
    }
}
