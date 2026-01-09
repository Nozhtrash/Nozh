package dev.nozh.core.monitoring;

import dev.nozh.core.util.RollingAverage;
import net.minecraft.client.MinecraftClient;

/**
 * Detects CPU vs GPU bottlenecks.
 * 
 * ROADMAP: Phase 2, Sprint 4 - CPU/GPU Bottleneck Detection
 * 
 * Analyzes tick time vs render time to identify performance bottlenecks.
 */
public class BottleneckDetector {
    
    private final RollingAverage tickTime = new RollingAverage(100);
    private final RollingAverage renderTime = new RollingAverage(100);
    
    public enum Bottleneck {
        CPU_BOUND,      // Tick time high, render ok
        GPU_BOUND,      // Render time high, tick ok
        BALANCED,       // Both similar
        NEITHER         // Both low (no bottleneck)
    }
    
    /**
     * Sample performance metrics.
     */
    public void sample(MinecraftClient client) {
        if (client == null) {
            return;
        }
        
        // Estimate tick time (CPU)
        double lastTickMs = estimateTickTime(client);
        tickTime.add(lastTickMs);
        
        // Render time (GPU)
        double frameTime = client.getLastFrameDuration();
        double estimatedRenderTime = frameTime - lastTickMs;
        if (estimatedRenderTime > 0) {
            renderTime.add(estimatedRenderTime);
        }
    }
    
    /**
     * Estimate tick time from server or client.
     */
    private double estimateTickTime(MinecraftClient client) {
        // Try to get from integrated server
        if (client.getServer() != null) {
            return client.getServer().getTickTime() / 1_000_000.0; // ns to ms
        }
        
        // Fallback: estimate from frame time (assume 50ms target tick time)
        return Math.min(50.0, client.getLastFrameDuration() * 0.3);
    }
    
    /**
     * Detect current bottleneck.
     */
    public Bottleneck detect() {
        double avgTick = tickTime.getAverage();
        double avgRender = renderTime.getAverage();
        double total = avgTick + avgRender;
        
        // No bottleneck if maintaining 60+ FPS
        if (total < 16.67) {
            return Bottleneck.NEITHER;
        }
        
        // Calculate tick ratio
        double tickRatio = avgTick / total;
        
        if (tickRatio > 0.65) {
            // Tick is 65%+ of frame time - CPU bound
            return Bottleneck.CPU_BOUND;
        } else if (tickRatio < 0.35) {
            // Render is 65%+ of frame time - GPU bound
            return Bottleneck.GPU_BOUND;
        } else {
            // Both contribute - balanced
            return Bottleneck.BALANCED;
        }
    }
    
    /**
     * Get detailed analysis string.
     */
    public String getDetailedAnalysis() {
        Bottleneck type = detect();
        double avgTick = tickTime.getAverage();
        double avgRender = renderTime.getAverage();
        
        StringBuilder sb = new StringBuilder();
        sb.append("Bottleneck: ").append(type).append("\n");
        sb.append(String.format("Tick time: %.2f ms\n", avgTick));
        sb.append(String.format("Render time: %.2f ms\n", avgRender));
        
        // Add recommendations
        switch (type) {
            case CPU_BOUND:
                sb.append("Recommendations:\n");
                sb.append("- Reduce simulation distance\n");
                sb.append("- Lower entity count\n");
                sb.append("- Disable complex redstone\n");
                break;
                
            case GPU_BOUND:
                sb.append("Recommendations:\n");
                sb.append("- Reduce render distance\n");
                sb.append("- Lower graphics quality\n");
                sb.append("- Disable shaders\n");
                break;
                
            case BALANCED:
                sb.append("Both CPU and GPU contributing to performance impact.\n");
                break;
                
            case NEITHER:
                sb.append("Performance is good, no bottleneck detected.\n");
                break;
        }
        
        return sb.toString();
    }
    
    /**
     * Get average tick time.
     */
    public double getAvgTickTime() {
        return tickTime.getAverage();
    }
    
    /**
     * Get average render time.
     */
    public double getAvgRenderTime() {
        return renderTime.getAverage();
    }
    
    /**
     * Filter actions by bottleneck type.
     */
    public boolean isCPUOptimization(String actionId) {
        return actionId.contains("simulation") || 
               actionId.contains("entity") ||
               actionId.contains("tick");
    }
    
    /**
     * Check if action is GPU optimization.
     */
    public boolean isGPUOptimization(String actionId) {
        return actionId.contains("render") || 
               actionId.contains("graphics") ||
               actionId.contains("particles") ||
               actionId.contains("mipmap") ||
               actionId.contains("lighting");
    }
}