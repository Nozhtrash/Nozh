package dev.nozh.core.telemetry;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Timeline of critical system events for debugging and analysis.
 */
public final class EventTimeline {
    private final Deque<Event> events = new ConcurrentLinkedDeque<>();
    private static final int MAX_EVENTS = 1000;
    
    public void recordEvent(EventSeverity severity, EventCategory category, String message, Map<String, String> context) {
        events.addLast(new Event(
            System.currentTimeMillis(),
            severity,
            category,
            message,
            new HashMap<>(context)
        ));
        
        while (events.size() > MAX_EVENTS) {
            events.removeFirst();
        }
    }
    
    public void recordEvent(EventSeverity severity, EventCategory category, String message) {
        recordEvent(severity, category, message, Map.of());
    }
    
    public List<Event> getRecent(int limit) {
        return events.stream()
            .skip(Math.max(0, events.size() - limit))
            .toList();
    }
    
    public List<Event> getByCategory(EventCategory category, int limit) {
        return events.stream()
            .filter(e -> e.category() == category)
            .skip(Math.max(0, (int)events.stream().filter(e -> e.category() == category).count() - limit))
            .toList();
    }
    
    public List<Event> getBySeverity(EventSeverity severity, int limit) {
        return events.stream()
            .filter(e -> e.severity() == severity)
            .skip(Math.max(0, (int)events.stream().filter(e -> e.severity() == severity).count() - limit))
            .toList();
    }
    
    public void reset() {
        events.clear();
    }
    
    public record Event(
        long timestamp,
        EventSeverity severity,
        EventCategory category,
        String message,
        Map<String, String> context
    ) {}
    
    public enum EventSeverity {
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
    
    public enum EventCategory {
        GOVERNOR_DECISION,
        ROLLBACK,
        SAFE_MODE,
        CRASH_LOOP,
        COMPAT_CONFLICT,
        PERFORMANCE_SPIKE,
        CONFIG_CHANGE,
        STATE_MIGRATION,
        PROVIDER_ERROR,
        SCENARIO_CHANGE
    }
}
