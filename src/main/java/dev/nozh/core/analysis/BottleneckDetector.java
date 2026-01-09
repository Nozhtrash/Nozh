package dev.nozh.core.analysis;

import dev.nozh.core.util.RollingAverage;
import net.minecraft.client.MinecraftClient;

/**
 * Detects CPU vs GPU performance bottlenecks.
 * 
 * <p>Analyzes tick time (CPU) vs render time (GPU) to identify
 * which component is limiting performance.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 2 Sprint 4)
 */
public final class BottleneckDetector {
    private final RollingAverage tickTime = new RollingAverage(100);
    private final RollingAverage renderTime = new RollingAverage(100);
    
    /**
     * Bottleneck types.
     */
    public enum Bottleneck {
        CPU_BOUND,      // Tick time high, render ok
        GPU_BOUND,      // Render time high, tick ok
        BALANCED,       // Both similar
        NEITHER         // Both low (no bottleneck)
    }
    
    /**
     * Sample current performance metrics.
     */
    public void sample(MinecraftClient client) {
        if (client == null) {
            return;
        }
        
        // Estimate tick time (simplified - would need more accurate measurement)
        double frameTime = client.getLastFrameDuration();
        double estimatedTickMs = estimateTickTime(client);
        double estimatedRenderMs = Math.max(0, frameTime - estimatedTickMs);
        
        tickTime.add(estimatedTickMs);
        renderTime.add(estimatedRenderMs);
    }
    
    /**
     * Estimate tick time from server/client state.
     */
    private double estimateTickTime(MinecraftClient client) {
        // Simplified estimation - in production would use profiler data
        if (client.getServer() != null) {
            return client.getServer().getTickTime() / 1_000_000.0; // nanos to ms
        }
        
        // Fallback: assume ~10ms for client-side logic
        return 10.0;
    }
    
    /**
     * Detect current bottleneck type.
     */
    public Bottleneck detect() {
        double avgTick = tickTime.getAverage();
        double avgRender = renderTime.getAverage();
        double total = avgTick + avgRender;
        
        // No bottleneck if running at 60+ FPS
        if (total < 16.67) {
            return Bottleneck.NEITHER;
        }
        
        double tickRatio = avgTick / total;
        
        if (tickRatio > 0.65) {
            return Bottleneck.CPU_BOUND; // Tick is 65%+ of frame time
        } else if (tickRatio < 0.35) {
            return Bottleneck.GPU_BOUND; // Render is 65%+ of frame time
        } else {
            return Bottleneck.BALANCED;
        }
    }
    
    /**
     * Get detailed analysis with recommendations.
     */
    public String getDetailedAnalysis() {
        Bottleneck type = detect();
        double avgTick = tickTime.getAverage();
        double avgRender = renderTime.getAverage();
        
        StringBuilder sb = new StringBuilder();
        sb.append("Bottleneck: ").append(type).append("\n");
        sb.append(String.format("Tick time: %.2fms\n", avgTick));
        sb.append(String.format("Render time: %.2fms\n", avgRender));
        
        switch (type) {
            case CPU_BOUND -> {
                sb.append("Recommendations:\n");
                sb.append("- Reduce simulation distance\n");
                sb.append("- Lower entity count\n");
                sb.append("- Disable complex redstone\n");
            }
            case GPU_BOUND -> {
                sb.append("Recommendations:\n");
                sb.append("- Reduce render distance\n");
                sb.append("- Lower graphics quality\n");
                sb.append("- Disable shaders\n");
            }
            case BALANCED -> sb.append("Both CPU and GPU contributing to frame time\n");
            case NEITHER -> sb.append("Performance is good, no optimization needed\n");
        }
        
        return sb.toString();
    }
    
    public double getAverageTickTime() {
        return tickTime.getAverage();
    }
    
    public double getAverageRenderTime() {
        return renderTime.getAverage();
    }
}
