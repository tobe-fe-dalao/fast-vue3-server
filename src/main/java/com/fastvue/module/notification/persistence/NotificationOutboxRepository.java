package com.fastvue.module.notification.persistence;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Cross-tenant infrastructure queue; every payload carries an explicit tenant and receiver. */
@Repository
@RequiredArgsConstructor
public class NotificationOutboxRepository {
    private final JdbcTemplate jdbc;

    public record Message(
            UUID id,
            Long tenantId,
            Long receiverId,
            String type,
            String title,
            String content,
            int attempts) {}

    public void enqueue(Message message) {
        jdbc.update(
                """
                INSERT INTO sys_notification_outbox(id, tenant_id, receiver_id, type, title, content)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                message.id(),
                message.tenantId(),
                message.receiverId(),
                message.type(),
                message.title(),
                message.content());
    }

    public List<UUID> pendingIds(int limit) {
        return jdbc.query(
                """
                SELECT id FROM sys_notification_outbox WHERE status = 'PENDING' AND next_attempt_at <= now()
                ORDER BY next_attempt_at, created_at, id LIMIT ?
                """,
                (rs, row) -> rs.getObject("id", UUID.class),
                limit);
    }

    public Message lockDue(UUID id) {
        return jdbc
                .query(
                        """
                SELECT id, tenant_id, receiver_id, type, title, content, attempts FROM sys_notification_outbox
                WHERE id = ? AND status = 'PENDING' AND next_attempt_at <= now() FOR UPDATE SKIP LOCKED
                """,
                        (rs, row) ->
                                new Message(
                                        rs.getObject("id", UUID.class),
                                        rs.getLong("tenant_id"),
                                        rs.getLong("receiver_id"),
                                        rs.getString("type"),
                                        rs.getString("title"),
                                        rs.getString("content"),
                                        rs.getInt("attempts")),
                        id)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public void delivered(UUID id) {
        jdbc.update(
                """
                UPDATE sys_notification_outbox SET status = 'DELIVERED', delivered_at = now(),
                attempts = attempts + 1, last_error = NULL WHERE id = ?
                """,
                id);
    }

    public void failed(Message message, int maxAttempts, String errorClass) {
        int attempts = message.attempts() + 1;
        long delaySeconds = Math.min(3600L, 5L * (1L << Math.min(attempts - 1, 10)));
        jdbc.update(
                """
                UPDATE sys_notification_outbox SET attempts = attempts + 1, last_error = ?,
                status = ?, next_attempt_at = now() + (? * interval '1 second')
                WHERE id = ? AND status = 'PENDING' AND attempts = ?
                """,
                errorClass,
                attempts >= maxAttempts ? "FAILED" : "PENDING",
                delaySeconds,
                message.id(),
                message.attempts());
    }
}
