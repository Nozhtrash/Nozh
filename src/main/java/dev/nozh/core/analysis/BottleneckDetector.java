package dev.nozh.core.analysis;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
/**
 * Detects whether performance is CPU-bound or GPU-bound.
 * Analyzes tick time vs render time to determine bottleneck.
 */
public class BottleneckDetector {
    
    private final RollingAverage tickTime = new RollingAverage(100);
    private final RollingAverage renderTime = new RollingAverage(100);
    
    public enum Bottleneck {
        CPU_BOUND,      // Tick time is the bottleneck
        GPU_BOUND,      // Render time is the bottleneck
        BALANCED,       // Both contribute equally
        NEITHER         // Performance is good, no bottleneck
    }
    
    /**
     * Sample current frame timing.
     */
    public void sample(MinecraftClient client) {
        try {
            // Get tick time (CPU work)
            double tickMs = estimateTickTime(client);
            tickTime.add(tickMs);
            
            // Get total frame time
            double frameMs = client.getLastFrameDuration();
            if (frameMs < 1.0) {
                frameMs *= 1000.0; // Convert to ms if needed
            }
            
            // Render time = total - tick (approximation)
            double renderMs = Math.max(0, frameMs - tickMs);
            renderTime.add(renderMs);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.debug("Failed to sample bottleneck data", e);
        }
    }
    
    /**
     * Detect current bottleneck type.
     */
    public Bottleneck detect() {
        if (!tickTime.hasEnoughSamples() || !renderTime.hasEnoughSamples()) {
            return Bottleneck.NEITHER;
        }
        
        double avgTick = tickTime.getAverage();
        double avgRender = renderTime.getAverage();
        double total = avgTick + avgRender;
        
        // If total is low, no bottleneck
        if (total < 16.67) { // 60+ FPS
            return Bottleneck.NEITHER;
        }
        
        // Calculate tick's percentage of total frame time
        double tickRatio = avgTick / total;
        
        // CPU bound if tick takes >65% of frame time
        if (tickRatio > 0.65) {
            return Bottleneck.CPU_BOUND;
        }
        
        // GPU bound if render takes >65% (tick <35%)
        if (tickRatio < 0.35) {
            return Bottleneck.GPU_BOUND;
        }
        
        // Balanced if both contribute
        return Bottleneck.BALANCED;
    }
    
    /**
     * Get detailed analysis string.
     */
    public String getDetailedAnalysis() {
        Bottleneck type = detect();
        double avgTick = tickTime.getAverage();
        double avgRender = renderTime.getAverage();
        
        StringBuilder sb = new StringBuilder();
        sb.append("Bottleneck Analysis:\n");
        sb.append(String.format("  Type: %s\n", type));
        sb.append(String.format("  Tick time: %.2f ms\n", avgTick));
        sb.append(String.format("  Render time: %.2f ms\n", avgRender));
        sb.append(String.format("  Total: %.2f ms (%.1f FPS)\n", 
                              avgTick + avgRender, 
                              1000.0 / (avgTick + avgRender)));
        
        // Add recommendations
        sb.append("\nRecommendations:\n");
        switch (type) {
            case CPU_BOUND:
                sb.append("  - Reduce simulation distance\n");
                sb.append("  - Lower entity count\n");
                sb.append("  - Reduce redstone complexity\n");
                sb.append("  - Disable unnecessary mods\n");
                break;
            case GPU_BOUND:
                sb.append("  - Reduce render distance\n");
                sb.append("  - Lower graphics quality\n");
                sb.append("  - Disable shaders\n");
                sb.append("  - Reduce particles\n");
                break;
            case BALANCED:
                sb.append("  - Both CPU and GPU are limiting\n");
                sb.append("  - Apply balanced optimizations\n");
                break;
            case NEITHER:
                sb.append("  - Performance is good!\n");
                sb.append("  - No optimizations needed\n");
                break;
        }
        
        return sb.toString();
    }
    
    /**
     * Estimate tick time from available sources.
     */
    private double estimateTickTime(MinecraftClient client) {
        try {
            // Try integrated server first (singleplayer)
            if (client.getServer() != null) {
                return client.getServer().getTickTime();
            }
            
            // Fallback: estimate based on game complexity
            if (client.world != null) {
                // Count entities by iterating the Iterable
                int entities = 0;
                Iterator<Entity> it = client.world.getEntities().iterator();
                while (it.hasNext()) {
                    it.next();
                    entities++;
                }
                
                // Base tick time + entity overhead
                double baseTickTime = 8.0; // Base 8ms
                double entityOverhead = Math.min(entities * 0.015, 42.0); // Max 42ms
                
                return baseTickTime + entityOverhead;
            }
            
            return 10.0; // Default
            
        } catch (Exception e) {
            return 10.0; // Safe default
        }
    }
    
    /**
     * Get tick time average.
     */
    public double getAverageTickTime() {
        return tickTime.getAverage();
    }
    
    /**
     * Get render time average.
     */
    public double getAverageRenderTime() {
        return renderTime.getAverage();
    }
    
    /**
     * Reset all collected data.
     */
    public void reset() {
        tickTime.clear();
        renderTime.clear();
    }
    
    /**
     * Simple rolling average calculator.
     */
    private static class RollingAverage {
        private final Deque<Double> values;
        private final int maxSize;
        private double sum = 0.0;
        
        RollingAverage(int maxSize) {
            this.maxSize = maxSize;
            this.values = new ArrayDeque<>(maxSize);
        }
        
        void add(double value) {
            if (values.size() >= maxSize) {
                double removed = values.removeFirst();
                sum -= removed;
            }
            values.addLast(value);
            sum += value;
        }
        
        double getAverage() {
            return values.isEmpty() ? 0.0 : sum / values.size();
        }
        
        boolean hasEnoughSamples() {
            return values.size() >= 20; // Need at least 20 samples
        }
        
        void clear() {
            values.clear();
            sum = 0.0;
        }
    }
}
