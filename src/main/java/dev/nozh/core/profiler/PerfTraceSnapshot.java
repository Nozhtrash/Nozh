package dev.nozh.core.profiler;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record PerfTraceSnapshot(
        long windowStartMillis,
        long windowEndMillis,
        List<PerfTraceEvent> events) {

    public static PerfTraceSnapshot empty() {
        return new PerfTraceSnapshot(0L, 0L, List.of());
    }

    public List<PerfTraceEvent> events() {
        return events == null ? List.of() : Collections.unmodifiableList(events);
    }

    public Optional<PerfTraceEvent> latestCriticalEvent() {
        if (events == null || events.isEmpty()) {
            return Optional.empty();
        }
        for (int i = events.size() - 1; i >= 0; i--) {
            PerfTraceEvent event = events.get(i);
            if (event.type() == PerfTraceType.CRITICAL) {
                return Optional.of(event);
            }
        }
        return Optional.empty();
    }
}
