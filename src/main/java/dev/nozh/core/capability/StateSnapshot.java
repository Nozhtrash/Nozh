package dev.nozh.core.capability;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.CloudRenderMode;

import java.util.HashMap;
import java.util.Map;

/**
 * Snapshot of game state for rollback purposes.
 * Stores settings values that can be restored later.
 */
public class StateSnapshot {
    
    private final Map<String, Object> values = new HashMap<>();
    private final long timestamp;
    
    public StateSnapshot() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Store a value in the snapshot.
     */
    public void put(String key, Object value) {
        values.put(key, value);
    }
    
    /**
     * Get a value from the snapshot.
     */
    public Object get(String key) {
        return values.get(key);
    }
    
    /**
     * Check if snapshot contains a key.
     */
    public boolean has(String key) {
        return values.containsKey(key);
    }
    
    /**
     * Get snapshot timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get age of snapshot in milliseconds.
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - timestamp;
    }
    
    /**
     * Capture complete snapshot of all Nozh-managed settings.
     */
    public static StateSnapshot captureAll() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            throw new IllegalStateException("Cannot capture snapshot: client is null");
        }
        
        GameOptions opts = client.options;
        StateSnapshot snapshot = new StateSnapshot();
        
        try {
            snapshot.put("render_distance", opts.getViewDistance().getValue());
            snapshot.put("simulation_distance", opts.getSimulationDistance().getValue());
            snapshot.put("particles", opts.getParticles().getValue());
            snapshot.put("entity_distance", opts.getEntityDistanceScaling().getValue());
            snapshot.put("graphics_mode", opts.getGraphicsMode().getValue());
            snapshot.put("mipmap_levels", opts.getMipmapLevels().getValue());
            snapshot.put("smooth_lighting", opts.getSmoothLighting().getValue());
            snapshot.put("clouds", opts.getCloudRenderMode().getValue());
            snapshot.put("vsync", opts.getEnableVsync().getValue());
            snapshot.put("max_fps", opts.getMaxFps().getValue());
        } catch (Exception e) {
            throw new RuntimeException("Failed to capture settings snapshot", e);
        }
        
        return snapshot;
    }
    
    /**
     * Restore all settings from this snapshot.
     */
    public void restoreAll() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            throw new IllegalStateException("Cannot restore: client is null");
        }
        
        GameOptions opts = client.options;
        
        try {
            if (has("render_distance")) {
                opts.getViewDistance().setValue((int) get("render_distance"));
            }
            if (has("simulation_distance")) {
                opts.getSimulationDistance().setValue((int) get("simulation_distance"));
            }
            if (has("particles")) {
                opts.getParticles().setValue((ParticlesMode) get("particles"));
            }
            if (has("entity_distance")) {
                opts.getEntityDistanceScaling().setValue((double) get("entity_distance"));
            }
            if (has("graphics_mode")) {
                opts.getGraphicsMode().setValue((GraphicsMode) get("graphics_mode"));
            }
            if (has("mipmap_levels")) {
                opts.getMipmapLevels().setValue((int) get("mipmap_levels"));
            }
            if (has("smooth_lighting")) {
                opts.getSmoothLighting().setValue((boolean) get("smooth_lighting"));
            }
            if (has("clouds")) {
                opts.getCloudRenderMode().setValue((CloudRenderMode) get("clouds"));
            }
            if (has("vsync")) {
                opts.getEnableVsync().setValue((boolean) get("vsync"));
            }
            if (has("max_fps")) {
                opts.getMaxFps().setValue((int) get("max_fps"));
            }
            
            opts.write();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore settings from snapshot", e);
        }
    }
    
    @Override
    public String toString() {
        return String.format("StateSnapshot{age=%dms, keys=%s}", 
                           getAgeMs(), values.keySet());
    }
}
