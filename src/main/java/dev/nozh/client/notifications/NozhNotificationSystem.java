package dev.nozh.client.notifications;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Non-intrusive notification system for important events.
 * Shows toast-style notifications for:
 * - Major optimizations applied
 * - Performance warnings
 * - Recovery from degradation
 * 
 * INTEGRATION: Client-side UI
 * CONTRACT: Thread-safe, minimal allocation
 */
public final class NozhNotificationSystem {

    private static final long DEFAULT_DURATION_MS = 3000;
    private static final int MAX_NOTIFICATIONS = 5;

    /**
     * Notification type with visual styling.
     */
    public enum NotificationType {
        SUCCESS,    // Green, short duration
        INFO,       // Blue, medium duration
        WARNING,    // Yellow, longer duration
        CRITICAL    // Red, persistent until dismissed
    }

    /**
     * Notification record.
     */
    public record Notification(
        int id,
        String title,
        String message,
        NotificationType type,
        long createdAt,
        long expiresAt,
        boolean dismissable
    ) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        public long remainingMs() {
            return Math.max(0, expiresAt - System.currentTimeMillis());
        }
    }

    private final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * Show a notification.
     */
    public int show(String title, String message, NotificationType type) {
        long duration = getDurationForType(type);
        return show(title, message, type, duration);
    }

    /**
     * Show notification with custom duration.
     */
    public int show(String title, String message, NotificationType type, long durationMs) {
        long now = System.currentTimeMillis();
        int id = nextId.getAndIncrement();
        
        Notification notification = new Notification(
            id,
            title,
            message,
            type,
            now,
            type == NotificationType.CRITICAL ? Long.MAX_VALUE : now + durationMs,
            type != NotificationType.CRITICAL
        );

        // Cleanup old notifications before adding
        cleanupExpired();

        // Limit total notifications
        if (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }

        notifications.add(notification);
        return id;
    }

    /**
     * Show optimization applied notification.
     */
    public int showOptimizationApplied(CapabilityId capability, CapabilityValue value, double gainMs) {
        String valueStr = formatValue(value);
        String title = "Optimization Applied";
        String message = String.format("%s → %s (Expected: +%.1fms)", 
            capability.name(), valueStr, gainMs);
        return show(title, message, NotificationType.SUCCESS);
    }

    /**
     * Show performance warning.
     */
    public int showPerformanceWarning(String reason) {
        return show("Performance Warning", reason, NotificationType.WARNING, 5000);
    }

    /**
     * Show recovery notification.
     */
    public int showRecovery(String details) {
        return show("Performance Recovered", details, NotificationType.INFO);
    }

    /**
     * Show critical alert.
     */
    public int showCritical(String title, String message) {
        return show(title, message, NotificationType.CRITICAL);
    }

    /**
     * Dismiss a notification by ID.
     */
    public boolean dismiss(int notificationId) {
        return notifications.removeIf(n -> n.id == notificationId && n.dismissable);
    }

    /**
     * Dismiss all dismissable notifications.
     */
    public void dismissAll() {
        notifications.removeIf(Notification::dismissable);
    }

    /**
     * Get all active (non-expired) notifications.
     */
    public List<Notification> getActiveNotifications() {
        cleanupExpired();
        return new ArrayList<>(notifications);
    }

    /**
     * Clear all notifications.
     */
    public void clear() {
        notifications.clear();
    }

    /**
     * Get notification count.
     */
    public int getCount() {
        cleanupExpired();
        return notifications.size();
    }

    private void cleanupExpired() {
        notifications.removeIf(Notification::isExpired);
    }

    private long getDurationForType(NotificationType type) {
        return switch (type) {
            case SUCCESS -> 2500;
            case INFO -> 3500;
            case WARNING -> 5000;
            case CRITICAL -> Long.MAX_VALUE;
        };
    }

    private String formatValue(CapabilityValue value) {
        // Java 17 compatible: using instanceof instead of pattern matching switch
        if (value == null) {
            return "null";
        }
        if (value instanceof CapabilityValue.IntValue) {
            return String.valueOf(((CapabilityValue.IntValue) value).value());
        }
        if (value instanceof CapabilityValue.BoolValue) {
            return ((CapabilityValue.BoolValue) value).value() ? "ON" : "OFF";
        }
        if (value instanceof CapabilityValue.EnumValue) {
            return ((CapabilityValue.EnumValue) value).name();
        }
        if (value instanceof CapabilityValue.FloatValue) {
            return String.format("%.2f", ((CapabilityValue.FloatValue) value).value());
        }
        return value.toString();
    }

    /**
     * Check if any critical notifications exist.
     */
    public boolean hasCritical() {
        return notifications.stream().anyMatch(n -> n.type == NotificationType.CRITICAL);
    }

    /**
     * Get summary of notification counts by type.
     */
    public String getSummary() {
        cleanupExpired();
        long success = notifications.stream().filter(n -> n.type == NotificationType.SUCCESS).count();
        long info = notifications.stream().filter(n -> n.type == NotificationType.INFO).count();
        long warning = notifications.stream().filter(n -> n.type == NotificationType.WARNING).count();
        long critical = notifications.stream().filter(n -> n.type == NotificationType.CRITICAL).count();
        
        return String.format("Notifications: %d total (Success: %d, Info: %d, Warning: %d, Critical: %d)",
            notifications.size(), success, info, warning, critical);
    }
}
