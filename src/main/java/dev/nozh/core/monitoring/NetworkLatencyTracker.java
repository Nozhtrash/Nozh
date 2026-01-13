package dev.nozh.core.monitoring;

import dev.nozh.core.math.ExponentialMovingAverage;
import dev.nozh.core.math.RollingVariance;

/**
 * Network Latency Tracker - Monitors connection quality to server.
 * 
 * Tracks:
 * - Round-trip ping time
 * - Jitter (variance in latency)
 * - Connection quality classification
 * - Packet timing consistency
 * 
 * Used to determine if network issues are causing performance problems
 * vs actual client or server performance issues.
 * 
 * THREAD SAFETY NOTE:
 * Written from Netty thread (Ping/Packet events).
 * Read from Render thread (HUD/Debug).
 * All shared state access is synchronized.
 */
public final class NetworkLatencyTracker {

    /**
     * Connection quality classification.
     */
    public enum ConnectionQuality {
        EXCELLENT,  // < 50ms, low jitter
        GOOD,       // < 100ms, moderate jitter
        FAIR,       // < 200ms
        POOR,       // < 500ms
        CRITICAL    // >= 500ms or high packet loss
    }

    // Thresholds in milliseconds
    private static final double EXCELLENT_PING_MS = 50.0;
    private static final double GOOD_PING_MS = 100.0;
    private static final double FAIR_PING_MS = 200.0;
    private static final double POOR_PING_MS = 500.0;
    
    // Jitter thresholds
    private static final double LOW_JITTER_MS = 10.0;
    private static final double HIGH_JITTER_MS = 50.0;
    
    // Tracking configuration
    private static final int PING_HISTORY_SIZE = 30;
    private static final double EMA_ALPHA = 0.2;

    // Ping tracking
    private final double[] pingHistory = new double[PING_HISTORY_SIZE];
    private int pingIndex = 0;
    private int pingCount = 0;
    
    // EMA for smoothed latency
    private final ExponentialMovingAverage emaLatency;
    
    // Jitter tracking (variance of latency)
    private final RollingVariance jitterTracker;
    
    // Packet tracking
    private long packetsSent = 0;
    private long packetsReceived = 0;
    private long lastPacketSentTime = 0;
    private long lastPacketReceivedTime = 0;
    
    // Current ping measurement
    private long currentPingStartTime = 0;
    private double lastMeasuredPingMs = -1;

    public NetworkLatencyTracker() {
        this.emaLatency = new ExponentialMovingAverage(EMA_ALPHA);
        this.jitterTracker = new RollingVariance(PING_HISTORY_SIZE);
    }

    /**
     * Start a ping measurement.
     * Call when sending a packet that expects a response.
     */
    public synchronized void startPingMeasurement() {
        currentPingStartTime = System.nanoTime();
        packetsSent++;
        lastPacketSentTime = System.currentTimeMillis();
    }

    /**
     * Complete a ping measurement.
     * Call when receiving the response to the ping.
     * 
     * @return measured ping in milliseconds
     */
    public synchronized double completePingMeasurement() {
        if (currentPingStartTime <= 0) {
            return -1;
        }
        
        long endTime = System.nanoTime();
        double pingMs = (endTime - currentPingStartTime) / 1_000_000.0;
        
        recordPing(pingMs);
        
        currentPingStartTime = 0;
        packetsReceived++;
        lastPacketReceivedTime = System.currentTimeMillis();
        
        return pingMs;
    }

    /**
     * Record a ping measurement directly.
     * 
     * @param pingMs ping in milliseconds
     */
    public synchronized void recordPing(double pingMs) {
        if (pingMs < 0 || Double.isNaN(pingMs) || Double.isInfinite(pingMs)) {
            return;
        }
        
        // Cap at reasonable value (10 seconds)
        pingMs = Math.min(pingMs, 10000.0);
        
        // Store in history
        pingHistory[pingIndex] = pingMs;
        pingIndex = (pingIndex + 1) % PING_HISTORY_SIZE;
        if (pingCount < PING_HISTORY_SIZE) {
            pingCount++;
        }
        
        // Update trackers
        emaLatency.addSample(pingMs);
        jitterTracker.addSample(pingMs);
        
        lastMeasuredPingMs = pingMs;
    }

    /**
     * Record a packet received (for packet loss estimation).
     */
    public synchronized void onPacketReceived() {
        packetsReceived++;
        lastPacketReceivedTime = System.currentTimeMillis();
    }

    /**
     * Record a packet sent (for packet loss estimation).
     */
    public synchronized void onPacketSent() {
        packetsSent++;
        lastPacketSentTime = System.currentTimeMillis();
    }

    /**
     * Get smoothed average ping.
     * 
     * @return EMA-smoothed ping in ms, or -1 if no data
     */
    public synchronized double getSmoothedPingMs() {
        if (!emaLatency.isInitialized()) {
            return -1;
        }
        return emaLatency.getValue();
    }

    /**
     * Get raw average ping from history.
     * 
     * @return average ping in ms, or -1 if no data
     */
    public synchronized double getAveragePingMs() {
        if (pingCount == 0) {
            return -1;
        }
        
        double sum = 0;
        for (int i = 0; i < pingCount; i++) {
            sum += pingHistory[i];
        }
        return sum / pingCount;
    }

    /**
     * Get minimum ping from recent history.
     */
    public synchronized double getMinPingMs() {
        if (pingCount == 0) {
            return -1;
        }
        
        double min = Double.MAX_VALUE;
        for (int i = 0; i < pingCount; i++) {
            min = Math.min(min, pingHistory[i]);
        }
        return min;
    }

    /**
     * Get maximum ping from recent history.
     */
    public synchronized double getMaxPingMs() {
        if (pingCount == 0) {
            return -1;
        }
        
        double max = 0;
        for (int i = 0; i < pingCount; i++) {
            max = Math.max(max, pingHistory[i]);
        }
        return max;
    }

    /**
     * Get jitter (standard deviation of ping).
     * Higher jitter = less stable connection.
     * 
     * @return jitter in ms, or 0 if not enough data
     */
    public synchronized double getJitterMs() {
        if (!jitterTracker.isFull()) {
            return 0;
        }
        return jitterTracker.getStandardDeviation();
    }

    /**
     * Get connection quality classification.
     */
    public synchronized ConnectionQuality getConnectionQuality() {
        double ping = getSmoothedPingMs();
        double jitter = getJitterMs();
        
        if (ping < 0) {
            return ConnectionQuality.GOOD; // Assume good until proven otherwise
        }
        
        // Factor in jitter - high jitter degrades quality
        double effectivePing = ping + (jitter * 0.5);
        
        if (effectivePing <= EXCELLENT_PING_MS && jitter <= LOW_JITTER_MS) {
            return ConnectionQuality.EXCELLENT;
        }
        if (effectivePing <= GOOD_PING_MS) {
            return ConnectionQuality.GOOD;
        }
        if (effectivePing <= FAIR_PING_MS) {
            return ConnectionQuality.FAIR;
        }
        if (effectivePing <= POOR_PING_MS) {
            return ConnectionQuality.POOR;
        }
        return ConnectionQuality.CRITICAL;
    }

    /**
     * Estimate packet loss percentage.
     * Note: This is a rough estimate based on sent vs received count.
     * 
     * @return estimated packet loss 0-100, or 0 if not enough data
     */
    public synchronized double getEstimatedPacketLossPercent() {
        if (packetsSent < 10) {
            return 0; // Not enough data
        }
        
        // Allow for some in-flight packets
        long expectedReceived = Math.max(0, packetsSent - 5);
        if (packetsReceived >= expectedReceived) {
            return 0;
        }
        
        long lost = expectedReceived - packetsReceived;
        return Math.min(100.0, (lost * 100.0) / expectedReceived);
    }

    /**
     * Check if connection has high latency.
     */
    public synchronized boolean isHighLatency() {
        double ping = getSmoothedPingMs();
        return ping >= FAIR_PING_MS;
    }

    /**
     * Check if connection is unstable (high jitter).
     */
    public synchronized boolean isUnstable() {
        return getJitterMs() > HIGH_JITTER_MS;
    }

    /**
     * Check if connection quality is good enough for real-time gameplay.
     */
    public synchronized boolean isGoodForGameplay() {
        ConnectionQuality quality = getConnectionQuality();
        return quality == ConnectionQuality.EXCELLENT || 
               quality == ConnectionQuality.GOOD;
    }

    /**
     * Time since last packet was received.
     * 
     * @return ms since last packet, or -1 if never received
     */
    public synchronized long getTimeSinceLastPacket() {
        if (lastPacketReceivedTime <= 0) {
            return -1;
        }
        return System.currentTimeMillis() - lastPacketReceivedTime;
    }

    /**
     * Check if connection appears to be lost.
     * 
     * @return true if no packets received in 5+ seconds
     */
    public synchronized boolean isConnectionLost() {
        long timeSince = getTimeSinceLastPacket();
        return timeSince > 5000;
    }

    /**
     * Reset all tracking data.
     * Call when changing servers or on disconnect.
     */
    public synchronized void reset() {
        pingIndex = 0;
        pingCount = 0;
        emaLatency.reset();
        jitterTracker.reset();
        packetsSent = 0;
        packetsReceived = 0;
        lastPacketSentTime = 0;
        lastPacketReceivedTime = 0;
        currentPingStartTime = 0;
        lastMeasuredPingMs = -1;
    }

    /**
     * Get comprehensive network status.
     */
    public synchronized NetworkStatus getStatus() {
        return new NetworkStatus(
            getSmoothedPingMs(),
            getAveragePingMs(),
            getMinPingMs(),
            getMaxPingMs(),
            getJitterMs(),
            getConnectionQuality(),
            getEstimatedPacketLossPercent(),
            isConnectionLost(),
            pingCount
        );
    }

    /**
     * Network status snapshot.
     */
    public record NetworkStatus(
        double smoothedPingMs,
        double averagePingMs,
        double minPingMs,
        double maxPingMs,
        double jitterMs,
        ConnectionQuality quality,
        double packetLossPercent,
        boolean connectionLost,
        int sampleCount
    ) {
        public String summary() {
            if (connectionLost) {
                return "CONNECTION LOST";
            }
            return String.format(
                "Ping: %.0fms (%.0f-%.0f) | Jitter: %.1fms | Quality: %s | Loss: %.1f%%",
                smoothedPingMs,
                minPingMs,
                maxPingMs,
                jitterMs,
                quality.name(),
                packetLossPercent
            );
        }
        
        public boolean hasEnoughData() {
            return sampleCount >= 5;
        }
    }
}
