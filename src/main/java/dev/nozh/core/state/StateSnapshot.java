package dev.nozh.core.state;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.CapabilityProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Snapshot of game settings state for rollback support.
 * 
 * <p>Captures all Nozh-modifiable settings and supports restoration.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 2)
 */
public final class StateSnapshot {
    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private final long timestamp;
    
    public StateSnapshot() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
 * Create a snapshot with a single key-value pair.
     */
    public static StateSnapshot single(String key, Object value) {
        StateSnapshot snapshot = new StateSnapshot();
        snapshot.put(key, value);
        return snapshot;
    }
    
    /**
     * Capture complete state of all Nozh-controllable settings.
     */
    public static StateSnapshot captureAll() {
        StateSnapshot snapshot = new StateSnapshot();
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client == null || client.options == null) {
            NozhConstants.LOGGER.warn("Cannot capture state: client not available");
            return snapshot;
        }
        
        GameOptions opts = client.options;
        
        try {
            // Capture all settings that Nozh can modify
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
            
            NozhConstants.LOGGER.debug("Captured state snapshot with {} settings", snapshot.values.size());
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to capture complete state", e);
        }
        
        return snapshot;
    }
    
    /**
     * Restore all captured settings.
     */
    public void restore() {
        if (values.isEmpty()) {
            NozhConstants.LOGGER.warn("Cannot restore: snapshot is empty");
            return;
        }
        
        int restored = 0;
        int failed = 0;
        
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            try {
                CapabilityProviderRegistry.restore(entry.getKey(), entry.getValue());
                restored++;
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Failed to restore setting: {}", entry.getKey(), e);
                failed++;
            }
        }
        
        NozhConstants.LOGGER.info("State restore complete: {} restored, {} failed", restored, failed);
    }
    
    public void put(String key, Object value) {
        if (key != null && value != null) {
            values.put(key, value);
        }
    }
    
    public Object get(String key) {
        return values.get(key);
    }
    
    public boolean has(String key) {
        return values.containsKey(key);
    }
    
    public Integer getInteger(String key) {
        Object value = values.get(key);
        return value instanceof Integer ? (Integer) value : null;
    }
    
    public Double getDouble(String key) {
        Object value = values.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
    
    public Boolean getBoolean(String key) {
        Object value = values.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int size() {
        return values.size();
    }
    
    public Map<String, Object> getAllValues() {
        return new HashMap<>(values);
    }
    
    @Override
    public String toString() {
        return "StateSnapshot[" + values.size() + " settings, age=" + 
               (System.currentTimeMillis() - timestamp) + "ms]";
    }
}
