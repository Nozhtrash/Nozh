package dev.nozh.core.governor;

import dev.nozh.core.monitoring.ServerPerformanceDetector;
import dev.nozh.core.monitoring.ServerPerformanceDetector.ServerHealth;
import dev.nozh.core.monitoring.NetworkLatencyTracker;
import dev.nozh.core.monitoring.NetworkLatencyTracker.ConnectionQuality;

/**
 * Server Lag Compensator - Adjusts optimization strategy based on server state.
 * 
 * Core functionality:
 * 1. Determine if performance issues are server-side vs client-side
 * 2. Pause/modify optimizations when server is lagging
 * 3. Adjust rendering strategy based on network conditions
 * 4. Provide guidance on what optimizations are appropriate
 * 
 * This prevents the mod from making counterproductive changes when
 * the real issue is server or network related.
 */
public final class ServerLagCompensator {

    /**
     * Lag source classification.
     */
    public enum LagSource {
        CLIENT,         // Client is the bottleneck
        SERVER,         // Server is lagging
        NETWORK,        // Network issues
        BOTH,           // Multiple issues
        UNKNOWN         // Not enough data
    }

    /**
     * Optimization guidance based on current conditions.
     */
    public enum OptimizationGuidance {
        FULL,           // Apply all optimizations normally
        CONSERVATIVE,   // Only apply safe, proven optimizations
        MINIMAL,        // Only critical optimizations
        PAUSE,          // Do not optimize - wait for conditions to improve
        RECOVER         // Conditions improving, can start recovery
    }

    // Thresholds
    private static final double SERVER_LAG_TPS_THRESHOLD = 18.0;
    private static final double NETWORK_LAG_PING_THRESHOLD = 150.0;
    private static final double NETWORK_JITTER_THRESHOLD = 30.0;
    
    // Hysteresis
    private static final int SAMPLES_TO_CONFIRM = 5;
    
    // State tracking
    private LagSource currentLagSource = LagSource.UNKNOWN;
    private int serverLagSampleCount = 0;
    private int networkLagSampleCount = 0;
    private int stableSampleCount = 0;
    
    // Recovery tracking
    private long lastLagDetectedTime = 0;
    private static final long RECOVERY_COOLDOWN_MS = 10_000; // 10 seconds

    private final ServerPerformanceDetector serverDetector;
    private final NetworkLatencyTracker networkTracker;

    public ServerLagCompensator(
            ServerPerformanceDetector serverDetector,
            NetworkLatencyTracker networkTracker) {
        this.serverDetector = serverDetector;
        this.networkTracker = networkTracker;
    }

    /**
     * Analyze current conditions and update lag source classification.
     * Call periodically (e.g., every tick or every second).
     * 
     * @param clientFrametimeMs current client frametime
     * @param targetFrametimeMs target frametime
     * @return current lag source
     */
    public LagSource analyze(double clientFrametimeMs, double targetFrametimeMs) {
        boolean clientLagging = clientFrametimeMs > targetFrametimeMs * 1.2;
        boolean serverLagging = isServerLagging();
        boolean networkLagging = isNetworkLagging();
        
        // Update sample counts with hysteresis
        if (serverLagging) {
            serverLagSampleCount = Math.min(serverLagSampleCount + 1, SAMPLES_TO_CONFIRM * 2);
            stableSampleCount = 0;
            lastLagDetectedTime = System.currentTimeMillis();
        } else {
            serverLagSampleCount = Math.max(0, serverLagSampleCount - 1);
        }
        
        if (networkLagging) {
            networkLagSampleCount = Math.min(networkLagSampleCount + 1, SAMPLES_TO_CONFIRM * 2);
            stableSampleCount = 0;
            lastLagDetectedTime = System.currentTimeMillis();
        } else {
            networkLagSampleCount = Math.max(0, networkLagSampleCount - 1);
        }
        
        if (!serverLagging && !networkLagging && !clientLagging) {
            stableSampleCount++;
        }
        
        // Determine primary lag source
        boolean confirmedServerLag = serverLagSampleCount >= SAMPLES_TO_CONFIRM;
        boolean confirmedNetworkLag = networkLagSampleCount >= SAMPLES_TO_CONFIRM;
        
        if (confirmedServerLag && confirmedNetworkLag) {
            currentLagSource = LagSource.BOTH;
        } else if (confirmedServerLag) {
            currentLagSource = LagSource.SERVER;
        } else if (confirmedNetworkLag) {
            currentLagSource = LagSource.NETWORK;
        } else if (clientLagging) {
            currentLagSource = LagSource.CLIENT;
        } else {
            currentLagSource = LagSource.UNKNOWN;
        }
        
        return currentLagSource;
    }

    /**
     * Check if server is currently lagging.
     */
    private boolean isServerLagging() {
        if (serverDetector == null) {
            return false;
        }
        
        double tps = serverDetector.getEstimatedTps();
        boolean directLag = serverDetector.isServerLagging();
        
        return directLag || tps < SERVER_LAG_TPS_THRESHOLD;
    }

    /**
     * Check if network is currently lagging.
     */
    private boolean isNetworkLagging() {
        if (networkTracker == null) {
            return false;
        }
        
        double ping = networkTracker.getSmoothedPingMs();
        double jitter = networkTracker.getJitterMs();
        
        if (ping < 0) {
            return false; // No data
        }
        
        return ping > NETWORK_LAG_PING_THRESHOLD || jitter > NETWORK_JITTER_THRESHOLD;
    }

    /**
     * Get current lag source.
     */
    public LagSource getCurrentLagSource() {
        return currentLagSource;
    }

    /**
     * Get optimization guidance based on current conditions.
     * 
     * @return recommended optimization approach
     */
    public OptimizationGuidance getOptimizationGuidance() {
        // If in recovery cooldown, check if stable
        long timeSinceLag = System.currentTimeMillis() - lastLagDetectedTime;
        boolean inRecoveryCooldown = timeSinceLag < RECOVERY_COOLDOWN_MS;
        
        switch (currentLagSource) {
            case SERVER:
                // Server lag - don't bother optimizing client
                return OptimizationGuidance.PAUSE;
                
            case NETWORK:
                // Network issues - be conservative
                return OptimizationGuidance.MINIMAL;
                
            case BOTH:
                // Multiple issues - definitely pause
                return OptimizationGuidance.PAUSE;
                
            case CLIENT:
                // Client is the issue - optimize normally
                if (inRecoveryCooldown) {
                    return OptimizationGuidance.CONSERVATIVE;
                }
                return OptimizationGuidance.FULL;
                
            case UNKNOWN:
            default:
                if (stableSampleCount > SAMPLES_TO_CONFIRM * 2) {
                    return OptimizationGuidance.FULL;
                } else if (inRecoveryCooldown) {
                    return OptimizationGuidance.RECOVER;
                }
                return OptimizationGuidance.CONSERVATIVE;
        }
    }

    /**
     * Check if client-side optimizations are appropriate right now.
     */
    public boolean shouldOptimize() {
        OptimizationGuidance guidance = getOptimizationGuidance();
        return guidance != OptimizationGuidance.PAUSE;
    }

    /**
     * Check if conditions are stable enough for quality recovery.
     */
    public boolean canRecoverQuality() {
        OptimizationGuidance guidance = getOptimizationGuidance();
        return guidance == OptimizationGuidance.FULL || 
               guidance == OptimizationGuidance.RECOVER;
    }

    /**
     * Get confidence that client optimization will be effective.
     * 
     * @return 0.0 to 1.0 confidence score
     */
    public double getOptimizationConfidence() {
        switch (currentLagSource) {
            case CLIENT:
                return 1.0;
            case UNKNOWN:
                return 0.7;
            case NETWORK:
                return 0.3;
            case SERVER:
                return 0.1;
            case BOTH:
                return 0.0;
            default:
                return 0.5;
        }
    }

    /**
     * Check if performance issue is likely server-side.
     */
    public boolean isLikelyServerIssue() {
        return currentLagSource == LagSource.SERVER || 
               currentLagSource == LagSource.BOTH;
    }

    /**
     * Check if performance issue is likely network-related.
     */
    public boolean isLikelyNetworkIssue() {
        return currentLagSource == LagSource.NETWORK || 
               currentLagSource == LagSource.BOTH;
    }

    /**
     * Reset state.
     * Call when changing servers or on disconnect.
     */
    public void reset() {
        currentLagSource = LagSource.UNKNOWN;
        serverLagSampleCount = 0;
        networkLagSampleCount = 0;
        stableSampleCount = 0;
        lastLagDetectedTime = 0;
    }

    /**
     * Get comprehensive compensation status.
     */
    public CompensationStatus getStatus() {
        return new CompensationStatus(
            currentLagSource,
            getOptimizationGuidance(),
            shouldOptimize(),
            canRecoverQuality(),
            getOptimizationConfidence(),
            serverLagSampleCount,
            networkLagSampleCount,
            stableSampleCount
        );
    }

    /**
     * Compensation status snapshot.
     */
    public record CompensationStatus(
        LagSource lagSource,
        OptimizationGuidance guidance,
        boolean shouldOptimize,
        boolean canRecover,
        double optimizationConfidence,
        int serverLagSamples,
        int networkLagSamples,
        int stableSamples
    ) {
        public String summary() {
            return String.format(
                "Source: %s | Guidance: %s | Optimize: %s | Confidence: %.0f%%",
                lagSource.name(),
                guidance.name(),
                shouldOptimize ? "Yes" : "NO",
                optimizationConfidence * 100
            );
        }
    }
}
