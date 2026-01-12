package dev.nozh.core.manual;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * v0.2: Pending suggestion queue with expiry.
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

    public PendingSuggestionQueue() {
        this(DEFAULT_MAX_SIZE, DEFAULT_TTL_MS);
    }

    public PendingSuggestionQueue(int maxSize, long ttlMs) {
        this.maxSize = Math.max(1, maxSize);
        this.ttlMs = Math.max(1L, ttlMs);
    }

    public synchronized void add(String id, String reason) {
        long now = nowMs();
        cleanup(now);

        // Drop oldest if full.
        while (q.size() >= maxSize) {
            q.removeFirst();
        }

        q.addLast(new PendingSuggestion(id, reason, now, now + ttlMs));
    }

    public synchronized PendingSuggestion peek() {
        cleanup(nowMs());
        return q.peekFirst();
    }

    public synchronized PendingSuggestion poll() {
        cleanup(nowMs());
        return q.pollFirst();
    }

    public synchronized int size() {
        cleanup(nowMs());
        return q.size();
    }

    private void cleanup(long now) {
        while (!q.isEmpty() && q.peekFirst().isExpired(now)) {
            q.removeFirst();
        }
    }

    private static long nowMs() {
        return System.currentTimeMillis();
    }
}
