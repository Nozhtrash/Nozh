package dev.nozh.core.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Snapshot of game state for rollback support.
 * 
 * @author Nozh Team
 * @since 0.5.0
 */
public final class StateSnapshot {
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    
    public static StateSnapshot single(String key, Object value) {
        StateSnapshot snapshot = new StateSnapshot();
        snapshot.put(key, value);
        return snapshot;
    }
    
    public void put(String key, Object value) {
        data.put(key, value);
    }
    
    public Object get(String key) {
        return data.get(key);
    }
    
    public Integer getInteger(String key) {
        Object value = data.get(key);
        return value instanceof Integer ? (Integer) value : null;
    }
    
    public Boolean getBoolean(String key) {
        Object value = data.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
    
    public Double getDouble(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
    
    public boolean has(String key) {
        return data.containsKey(key);
    }
    
    public Map<String, Object> getAll() {
        return new ConcurrentHashMap<>(data);
    }
}
