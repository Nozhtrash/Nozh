package dev.nozh.core.telemetry;

/**
 * Telemetry data snapshot for performance tracking.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 2)
 */
public final class TelemetrySnapshot {
    private final long timestamp;
    private final double fps;
    private final int renderDistance;
    private final double tickTime;
    private final double renderTime;
    
    public TelemetrySnapshot(double fps, int renderDistance, double tickTime, double renderTime) {
        this.timestamp = System.currentTimeMillis();
        this.fps = fps;
        this.renderDistance = renderDistance;
        this.tickTime = tickTime;
        this.renderTime = renderTime;
    }
    
    public long getTimestamp() { 
        return timestamp; 
    }
    
    public double getFps() { 
        return fps; 
    }
    
    public int getRenderDistance() { 
        return renderDistance; 
    }
    
    public double getTickTime() {
        return tickTime;
    }
    
    public double getRenderTime() {
        return renderTime;
    }
    
    @Override
    public String toString() {
        return String.format("TelemetrySnapshot[fps=%.2f, renderDist=%d, tick=%.2fms, render=%.2fms]",
            fps, renderDistance, tickTime, renderTime);
    }
}
