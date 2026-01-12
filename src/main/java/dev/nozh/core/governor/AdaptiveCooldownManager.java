package dev.nozh.core.governor;

import dev.nozh.core.bus.CapabilityId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic cooldown system that adapts based on action effectiveness.
 * Successful actions get shorter cooldowns, failed ones get longer.
 * 
 * INTEGRATION: Governor system
 * CONTRACT: Thread-safe, zero allocation in read paths
 */
public final class AdaptiveCooldownManager {

    private static final long BASE_COOLDOWN_MS = 5000;
    private static final long MIN_COOLDOWN_MS = 1000;
    private static final long MAX_COOLDOWN_MS = 30000;
    private static final double SUCCESS_REDUCTION = 0.85; // Reduce by 15%
    private static final double FAILURE_INCREASE = 1.25;  // Increase by 25%
    private static final double NEUTRAL_ADJUSTMENT = 0.95; // Slight reduction

    private final Map<CapabilityId, CooldownState> cooldowns = new ConcurrentHashMap<>();

    /**
     * Internal cooldown state.
     */
    private static class CooldownState {
        volatile long currentCooldownMs;
        volatile long lastActionTime;
        volatile int successStreak;
        volatile int failureStreak;

        CooldownState() {
            this.currentCooldownMs = BASE_COOLDOWN_MS;
            this.lastActionTime = 0;
            this.successStreak = 0;
            this.failureStreak = 0;
        }
    }

    /**
     * Get current cooldown for a capability.
     * Returns 0 if cooldown has expired.
     */
    public long getCooldownFor(CapabilityId capability) {
        CooldownState state = cooldowns.get(capability);
        if (state == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - state.lastActionTime;
        long remaining = state.currentCooldownMs - elapsed;
        
        return Math.max(0, remaining);
    }

    /**
     * Check if action is ready (cooldown expired).
     */
    public boolean isReady(CapabilityId capability) {
        return getCooldownFor(capability) == 0;
    }

    /**
     * Record successful action.
     * Reduces cooldown, building success streak.
     */
    public void recordSuccess(CapabilityId capability) {
        CooldownState state = getOrCreateState(capability);
        
        state.successStreak++;
        state.failureStreak = 0;
        state.lastActionTime = System.currentTimeMillis();

        // Reduce cooldown on success, more for streaks
        double reduction = SUCCESS_REDUCTION;
        if (state.successStreak >= 3) {
            reduction = 0.75; // More aggressive reduction for hot streak
        }
        
        long newCooldown = (long) (state.currentCooldownMs * reduction);
        state.currentCooldownMs = Math.max(MIN_COOLDOWN_MS, newCooldown);
    }

    /**
     * Record failed action.
     * Increases cooldown, building failure streak.
     */
    public void recordFailure(CapabilityId capability) {
        CooldownState state = getOrCreateState(capability);
        
        state.failureStreak++;
        state.successStreak = 0;
        state.lastActionTime = System.currentTimeMillis();

        // Increase cooldown on failure, more for streaks
        double increase = FAILURE_INCREASE;
        if (state.failureStreak >= 3) {
            increase = 1.5; // Penalize repeated failures
        }
        
        long newCooldown = (long) (state.currentCooldownMs * increase);
        state.currentCooldownMs = Math.min(MAX_COOLDOWN_MS, newCooldown);
    }

    /**
     * Record neutral action (no clear success or failure).
     * Slightly reduces cooldown.
     */
    public void recordNeutral(CapabilityId capability) {
        CooldownState state = getOrCreateState(capability);
        
        state.lastActionTime = System.currentTimeMillis();
        
        // Slight reduction for neutral outcomes
        long newCooldown = (long) (state.currentCooldownMs * NEUTRAL_ADJUSTMENT);
        state.currentCooldownMs = Math.max(MIN_COOLDOWN_MS, newCooldown);
    }

    /**
     * Get all current cooldowns (for display/debugging).
     */
    public Map<CapabilityId, Long> getAllCooldowns() {
        Map<CapabilityId, Long> result = new ConcurrentHashMap<>();
        long now = System.currentTimeMillis();
        
        for (Map.Entry<CapabilityId, CooldownState> entry : cooldowns.entrySet()) {
            CooldownState state = entry.getValue();
            long elapsed = now - state.lastActionTime;
            long remaining = Math.max(0, state.currentCooldownMs - elapsed);
            result.put(entry.getKey(), remaining);
        }
        
        return result;
    }

    /**
     * Get configured cooldown duration (not remaining time).
     */
    public long getConfiguredCooldown(CapabilityId capability) {
        CooldownState state = cooldowns.get(capability);
        return state != null ? state.currentCooldownMs : BASE_COOLDOWN_MS;
    }

    /**
     * Reset cooldown for a capability.
     */
    public void reset(CapabilityId capability) {
        cooldowns.remove(capability);
    }

    /**
     * Reset all cooldowns.
     */
    public void resetAll() {
        cooldowns.clear();
    }

    /**
     * Get statistics for a capability.
     */
    public String getStats(CapabilityId capability) {
        CooldownState state = cooldowns.get(capability);
        if (state == null) {
            return String.format("%s: No data", capability);
        }

        long remaining = getCooldownFor(capability);
        return String.format("%s: %dms configured, %dms remaining, Success: %d, Failures: %d",
            capability,
            state.currentCooldownMs,
            remaining,
            state.successStreak,
            state.failureStreak
        );
    }

    private CooldownState getOrCreateState(CapabilityId capability) {
        return cooldowns.computeIfAbsent(capability, k -> new CooldownState());
    }

    /**
     * Force set cooldown for testing.
     */
    public void setCooldown(CapabilityId capability, long cooldownMs) {
        CooldownState state = getOrCreateState(capability);
        state.currentCooldownMs = Math.max(MIN_COOLDOWN_MS, Math.min(MAX_COOLDOWN_MS, cooldownMs));
        state.lastActionTime = System.currentTimeMillis();
    }
}
