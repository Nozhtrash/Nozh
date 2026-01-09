package dev.nozh.core.safety;

import dev.nozh.NozhConstants;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.telemetry.TelemetryManager;

/**
 * Crash Loop Guard - Protects against repeated crashes by enabling safe mode.
 * Thread-safe implementation.
 * 
 * Logic:
 * 1. On startup: load state, increment bootAttempts, save immediately
 * 2. After N ticks of stable operation: mark stable, reset bootAttempts
 * 3. If bootAttempts >= 3 and not stable: activate safe mode
 * 4. Safe mode = profiler only, NO actions applied
 */
public final class CrashLoopGuard {

    private static final Object LOCK = new Object();
    private static volatile int ticksSinceStart = 0;
    private static volatile boolean initialized = false;
    private static volatile TelemetryManager telemetryManager = null;

    private CrashLoopGuard() {
        // Utility class
    }

    /**
     * Called once during mod initialization.
     * Loads state, increments boot attempts, checks for safe mode.
     * Thread-safe.
     */
    public static void onStartup() {
        synchronized (LOCK) {
            if (initialized) {
                NozhConstants.LOGGER.warn("CrashLoopGuard.onStartup() called multiple times, ignoring");
                return;
            }
            initialized = true;

            // Load existing state first
            StateManager.load();
            NozhState state = StateManager.getState();

            if (state == null) {
                NozhConstants.LOGGER.error("State is null after load, cannot initialize CrashLoopGuard");
                return;
            }

            state.cleanupExpiredQuarantines(System.currentTimeMillis());

            // Increment boot attempts immediately
            state.incrementBootAttempts();
            StateManager.saveImmediately();

            NozhConstants.LOGGER.info("Boot attempt #{}", state.bootAttempts);

            // Check if we should enter safe mode due to crash loop
            CrashRecoveryDecision decision = evaluateCrashRecovery(state, System.currentTimeMillis());
            if (decision.action() != CrashRecoveryAction.NONE) {
                StateManager.saveImmediately();
                if (decision.action() == CrashRecoveryAction.QUARANTINED_CAPABILITY) {
                    NozhConstants.LOGGER.warn(
                            "Crash loop detected, quarantining capability {} until {}",
                            decision.capabilityId(),
                            decision.retryAtMillis());
                } else if (decision.action() == CrashRecoveryAction.SAFE_MODE) {
                    NozhConstants.LOGGER.warn("NOZH entering SAFE MODE after {} failed boots", state.bootAttempts);
                }
                recordCrashRecoveryTelemetry(decision, state.lastFailureContext);
            }

            // Sync config force flag
            state.syncConfigForce(ConfigManager.getConfig().safeModeForce);
            if (state.safeModeCauses.contains(dev.nozh.core.safety.SafeModeCause.CONFIG_FORCE)) {
                NozhConstants.LOGGER.info("Safe mode forced by config");
            }
        }
    }

    /**
     * Called every client tick. Marks session as stable after N ticks.
     * Thread-safe.
     */
    public static void onClientTick() {
        synchronized (LOCK) {
            if (!initialized) {
                return;
            }

            NozhState state = StateManager.getState();
            if (state == null) {
                return;
            }

            // Already stable, nothing to do
            if (state.sessionStable) {
                return;
            }

            ticksSinceStart++;

            // Check if we've been stable long enough
            if (ticksSinceStart >= NozhConstants.TICKS_BEFORE_STABLE) {
                state.markStable();
                StateManager.saveImmediately();
                NozhConstants.LOGGER.info("Session marked stable after {} ticks", ticksSinceStart);

                // If we were in safe mode due to crashes (not forced), we stay in safe mode
                // User must manually reset with /nozh safemode reset
            }
        }
    }

    /**
     * Called on clean shutdown (if possible).
     * Thread-safe.
     */
    public static void onShutdown() {
        synchronized (LOCK) {
            if (!initialized) {
                return;
            }

            NozhState state = StateManager.getState();
            if (state == null) {
                return;
            }

            state.resetBootAttempts();
            StateManager.saveImmediately();
            NozhConstants.LOGGER.debug("Clean shutdown recorded");
        }
    }

    /**
     * Capture failure context to aid crash-loop recovery.
     */
    public static void recordFailureContext(CrashFailureContext context) {
        if (context == null) {
            return;
        }
        synchronized (LOCK) {
            StateManager.recordFailureContext(context);
        }
        recordCrashContextTelemetry(context);
    }

    /**
     * Check if we're currently in safe mode (any reason).
     * Thread-safe.
     */
    public static boolean isInSafeMode() {
        NozhState state = StateManager.getState();
        if (state == null) {
            return false;
        }
        return state.isSafeModeActive();
    }

    /**
     * Get human-readable reason for safe mode.
     * Thread-safe.
     */
    public static String getSafeModeReason() {
        NozhState state = StateManager.getState();
        if (state == null) {
            return "off";
        }
        return state.getSafeModeReason();
    }

    /**
     * Manually reset safe mode (from command).
     * Thread-safe.
     */
    public static void resetSafeMode() {
        synchronized (LOCK) {
            NozhState state = StateManager.getState();
            if (state == null) {
                NozhConstants.LOGGER.warn("Cannot reset safe mode: state is null");
                return;
            }

            state.deactivateSafeMode();
            state.resetBootAttempts();
            StateManager.saveImmediately();
            NozhConstants.LOGGER.info("Safe mode reset by user");
        }
    }

    /**
     * Manually enable safe mode (from command).
     * Thread-safe.
     */
    public static void enableSafeMode() {
        synchronized (LOCK) {
            NozhState state = StateManager.getState();
            if (state == null) {
                NozhConstants.LOGGER.warn("Cannot enable safe mode: state is null");
                return;
            }

            state.activateSafeModeUser();
            StateManager.saveImmediately();
            NozhConstants.LOGGER.info("Safe mode enabled by user");
        }
    }

    /**
     * Get current boot attempt count.
     * Thread-safe.
     */
    public static int getBootAttempts() {
        NozhState state = StateManager.getState();
        return (state != null) ? state.bootAttempts : 0;
    }

    /**
     * Check if session is stable.
     * Thread-safe.
     */
    public static boolean isSessionStable() {
        NozhState state = StateManager.getState();
        return (state != null) && state.sessionStable;
    }

    /**
     * Check if current state indicates a crash loop.
     */
    public static boolean isCrashLoopDetected() {
        NozhState state = StateManager.getState();
        return shouldEnterSafeMode(state);
    }

    /**
     * Check if a capability is quarantined due to crash recovery.
     */
    public static boolean isCapabilityQuarantined(dev.nozh.core.capability.CapabilityId capabilityId) {
        NozhState state = StateManager.getState();
        if (state == null) {
            return false;
        }
        return state.isCapabilityQuarantined(capabilityId, System.currentTimeMillis());
    }

    /**
     * Provide a telemetry manager to emit crash events.
     */
    public static void setTelemetryManager(TelemetryManager manager) {
        telemetryManager = manager;
    }

    /**
     * Get audit-friendly metrics for safe mode and crash-loop checks.
     */
    public static CrashLoopAuditMetrics getAuditMetrics() {
        synchronized (LOCK) {
            NozhState state = StateManager.getState();
            if (state == null) {
                return CrashLoopAuditMetrics.empty(ticksSinceStart, initialized);
            }
            return CrashLoopAuditMetrics.fromState(state, ticksSinceStart, initialized, shouldEnterSafeMode(state));
        }
    }

    private static boolean shouldEnterSafeMode(NozhState state) {
        if (state == null) {
            return false;
        }
        // Enter safe mode if we've had too many failed boots
        return state.bootAttempts >= NozhConstants.MAX_BOOT_ATTEMPTS_BEFORE_SAFE_MODE
                && !state.sessionStable;
    }

    /**
     * Evaluate and apply crash-loop recovery actions.
     */
    public static CrashRecoveryDecision evaluateCrashRecovery(NozhState state, long nowMillis) {
        if (!shouldEnterSafeMode(state)) {
            return CrashRecoveryDecision.none();
        }

        if (state == null) {
            return CrashRecoveryDecision.none();
        }

        state.cleanupExpiredQuarantines(nowMillis);

        CrashFailureContext context = state.lastFailureContext;
        if (context != null) {
            var capability = context.resolveCapabilityId();
            if (capability.isPresent()) {
                dev.nozh.core.capability.CapabilityId capabilityId = capability.get();
                if (!state.isCapabilityQuarantined(capabilityId, nowMillis)) {
                    long retryAt = nowMillis + NozhConstants.CRASH_RECOVERY_QUARANTINE_MILLIS;
                    state.quarantineCapability(capabilityId, retryAt);
                    return CrashRecoveryDecision.quarantined(capabilityId.name(), retryAt, context.errorMessage());
                }
            }
        }

        state.activateSafeModeCrashLoop();
        return CrashRecoveryDecision.safeMode(context != null ? context.errorMessage() : "");
    }

    private static void recordCrashContextTelemetry(CrashFailureContext context) {
        TelemetryManager manager = telemetryManager;
        if (manager != null) {
            manager.recordCrashContext(context);
        }
    }

    private static void recordCrashRecoveryTelemetry(CrashRecoveryDecision decision, CrashFailureContext context) {
        TelemetryManager manager = telemetryManager;
        if (manager != null) {
            manager.recordCrashRecovery(decision, context);
        }
    }
}
