package dev.nozh.core.manual;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * v0.2: Pending suggestion queue with expiry and lightweight de-dup.
 */
public final class PendingSuggestionQueue {

    public static final int DEFAULT_MAX_SIZE = 3;
    public static final long DEFAULT_TTL_MS = 60_000L;

    public static final class PendingSuggestion {
        public final String id;
        public final String reason;
        public final long createdAtMs;
        public final long expiresAtMs;

        public PendingSuggestion(String id, String reason, long createdAtMs, long expiresAtMs) {
            this.id = id;
            this.reason = reason;
            this.createdAtMs = createdAtMs;
            this.expiresAtMs = expiresAtMs;
        }

        public boolean isExpired(long nowMs) {
            return nowMs >= expiresAtMs;
        }
    }

    private final int maxSize;
    private final long ttlMs;

    private final Deque<PendingSuggestion> q = new ArrayDeque<>();
    private final Set<String> ids = new HashSet<>();

    public PendingSuggestionQueue() {
        this(DEFAULT_MAX_SIZE, DEFAULT_TTL_MS);
    }

    public PendingSuggestionQueue(int maxSize, long ttlMs) {
        this.maxSize = Math.max(1, maxSize);
        this.ttlMs = Math.max(1L, ttlMs);
    }

    /**
     * Adds a suggestion if it is not already present.
     */
    public synchronized boolean add(String id, String reason) {
        if (id == null || id.isBlank()) return false;
        long now = nowMs();
        cleanup(now);

        if (ids.contains(id)) {
            return false;
        }

        // Drop oldest if full.
        while (q.size() >= maxSize) {
            PendingSuggestion dropped = q.removeFirst();
            ids.remove(dropped.id);
        }

        PendingSuggestion s = new PendingSuggestion(id, reason == null ? "" : reason, now, now + ttlMs);
        q.addLast(s);
        ids.add(id);
        return true;
    }

    public synchronized PendingSuggestion peek() {
        cleanup(nowMs());
        return q.peekFirst();
    }

    public synchronized PendingSuggestion poll() {
        long now = nowMs();
        cleanup(now);
        PendingSuggestion s = q.pollFirst();
        if (s != null) {
            ids.remove(s.id);
        }
        return s;
    }

    public synchronized int size() {
        cleanup(nowMs());
        return q.size();
    }

    public synchronized boolean containsId(String id) {
        cleanup(nowMs());
        return ids.contains(id);
    }

    private void cleanup(long now) {
        while (!q.isEmpty() && q.peekFirst().isExpired(now)) {
            PendingSuggestion expired = q.removeFirst();
            ids.remove(expired.id);
        }
    }

    private static long nowMs() {
        return System.currentTimeMillis();
    }
}
