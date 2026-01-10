package dev.nozh.core.capability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Snapshot inmutable del estado de una capability.
 * Usado para rollback en caso de fallo.
 * 
 * <p>Soporta múltiples pares clave-valor para snapshots complejos.
 * Thread-safe y con getters type-safe.
 * 
 * @author Nozh Team
 * @since 0.5.0 (consolidado)
 */
public final class StateSnapshot {
    private final Map<String, Object> state;
    private final long timestamp;
    private final String primaryKey; // La clave principal del snapshot
    
    /**
     * Constructor privado. Usar factory methods.
     */
    private StateSnapshot(Map<String, Object> state, String primaryKey) {
        this.state = Collections.unmodifiableMap(new HashMap<>(state));
        this.timestamp = System.currentTimeMillis();
        this.primaryKey = primaryKey;
    }
    
    // ========== Factory Methods ==========
    
    /**
     * Crea snapshot con un solo valor.
     */
    public static StateSnapshot single(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return new StateSnapshot(map, key);
    }
    
    /**
     * Crea snapshot con múltiples valores.
     */
    public static StateSnapshot of(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Snapshot values cannot be null or empty");
        }
        String primaryKey = values.keySet().iterator().next();
        return new StateSnapshot(values, primaryKey);
    }
    
    /**
     * Crea snapshot con múltiples valores y clave primaria específica.
     */
    public static StateSnapshot of(Map<String, Object> values, String primaryKey) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Snapshot values cannot be null or empty");
        }
        if (!values.containsKey(primaryKey)) {
            throw new IllegalArgumentException("Primary key must exist in values");
        }
        return new StateSnapshot(values, primaryKey);
    }
    
    /**
     * Crea snapshot vacío (para casos especiales).
     */
    public static StateSnapshot empty() {
        return new StateSnapshot(Collections.emptyMap(), null);
    }
    
    // ========== Builder Pattern (opcional) ==========
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final Map<String, Object> values = new HashMap<>();
        private String primaryKey;
        
        public Builder put(String key, Object value) {
            values.put(key, value);
            if (primaryKey == null) {
                primaryKey = key;
            }
            return this;
        }
        
        public Builder setPrimaryKey(String key) {
            if (!values.containsKey(key)) {
                throw new IllegalArgumentException("Primary key must be added first");
            }
            this.primaryKey = key;
            return this;
        }
        
        public StateSnapshot build() {
            if (values.isEmpty()) {
                throw new IllegalStateException("Cannot build empty snapshot");
            }
            return new StateSnapshot(values, primaryKey);
        }
    }
    
    // ========== Query Methods ==========
    
    /**
     * @return true si contiene la clave especificada
     */
    public boolean has(String key) {
        return state.containsKey(key);
    }
    
    /**
     * @return true si el snapshot está vacío
     */
    public boolean isEmpty() {
        return state.isEmpty();
    }
    
    /**
     * @return número de valores en el snapshot
     */
    public int size() {
        return state.size();
    }
    
    /**
     * @return conjunto inmutable de claves
     */
    public Set<String> keys() {
        return state.keySet();
    }
    
    // ========== Getters (Generic) ==========
    
    /**
     * Obtiene valor genérico.
     */
    public Object get(String key) {
        return state.get(key);
    }
    
    /**
     * Obtiene valor como Optional.
     */
    public Optional<Object> getOptional(String key) {
        return Optional.ofNullable(state.get(key));
    }
    
    /**
     * Obtiene valor con default.
     */
    public Object getOrDefault(String key, Object defaultValue) {
        return state.getOrDefault(key, defaultValue);
    }
    
    // ========== Getters (Type-Safe) ==========
    
    public Integer getInteger(String key) {
        Object value = state.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    public Double getDouble(String key) {
        Object value = state.get(key);
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
    
    public Boolean getBoolean(String key) {
        Object value = state.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
    
    public String getString(String key) {
        Object value = state.get(key);
        return value != null ? value.toString() : null;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getTyped(String key, Class<T> type) {
        Object value = state.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    // ========== Metadata ==========
    
    /**
     * @return timestamp de creación del snapshot
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * @return edad del snapshot en milisegundos
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - timestamp;
    }
    
    /**
     * @return clave primaria del snapshot
     */
    public String getPrimaryKey() {
        return primaryKey;
    }
    
    /**
     * @return valor de la clave primaria
     */
    public Object getPrimaryValue() {
        return primaryKey != null ? state.get(primaryKey) : null;
    }
    
    /**
     * @return mapa inmutable de todo el estado
     */
    public Map<String, Object> getAll() {
        return state;
    }
    
    // ========== Display ==========
    
    @Override
    public String toString() {
        if (state.isEmpty()) {
            return "StateSnapshot[empty]";
        }
        if (state.size() == 1) {
            String key = state.keySet().iterator().next();
            return String.format("StateSnapshot[%s=%s]", key, state.get(key));
        }
        return String.format("StateSnapshot[%d values, primary=%s]", state.size(), primaryKey);
    }
    
    /**
     * @return representación detallada para debugging
     */
    public String toDebugString() {
        StringBuilder sb = new StringBuilder("StateSnapshot {\n");
        sb.append("  timestamp: ").append(timestamp).append("\n");
        sb.append("  age: ").append(getAgeMs()).append(" ms\n");
        sb.append("  primary: ").append(primaryKey).append("\n");
        sb.append("  values:\n");
        state.forEach((k, v) -> sb.append("    ").append(k).append(": ").append(v).append("\n"));
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StateSnapshot)) return false;
        StateSnapshot other = (StateSnapshot) obj;
        return state.equals(other.state);
    }
    
    @Override
    public int hashCode() {
        return state.hashCode();
    }
}