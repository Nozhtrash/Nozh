package dev.nozh.core.safety;

import java.util.HashMap;
import java.util.Map;

/**
 * Complete state snapshot of a capability provider.
 * 
 * Captures all settings before applying changes,
 * allowing atomic rollback if something goes wrong.
 * 
 * Includes:
 * - Provider ID
 * - All setting values
 * - Timestamp
 * - Validation status
 * 
 * TASK 4: Safe rollback - state capture
 */
public final class ProviderSnapshot {

    private final String providerId;
    private final Map<String, Object> state;
    private final long timestamp;
    private final boolean validated;

    private ProviderSnapshot(String providerId, Map<String, Object> state, boolean validated) {
        this.providerId = providerId;
        this.state = new HashMap<>(state); // Defensive copy
        this.timestamp = System.currentTimeMillis();
        this.validated = validated;
    }

    /**
     * Create snapshot from provider state.
     */
    public static ProviderSnapshot create(String providerId, Map<String, Object> state) {
        return new ProviderSnapshot(providerId, state, false);
    }

    /**
     * Create validated snapshot (state verified as safe).
     */
    public static ProviderSnapshot createValidated(String providerId, Map<String, Object> state) {
        return new ProviderSnapshot(providerId, state, true);
    }

    /**
     * Get provider ID.
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Get captured state (immutable).
     */
    public Map<String, Object> getState() {
        return new HashMap<>(state); // Return copy
    }

    /**
     * Get value for specific key.
     */
    public Object getValue(String key) {
        return state.get(key);
    }

    /**
     * Get value with type cast.
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String key, Class<T> type) {
        Object value = state.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException("Cannot cast " + value.getClass() + " to " + type);
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
     * Check if snapshot was validated.
     */
    public boolean isValidated() {
        return validated;
    }

    /**
     * Check if snapshot is fresh (less than 5 seconds old).
     */
    public boolean isFresh() {
        return getAgeMs() < 5000;
    }

    /**
     * Compare with another snapshot to detect changes.
     */
    public boolean hasChangedFrom(ProviderSnapshot other) {
        if (other == null) {
            return true;
        }
        if (!this.providerId.equals(other.providerId)) {
            return true;
        }
        if (this.state.size() != other.state.size()) {
            return true;
        }
        for (Map.Entry<String, Object> entry : this.state.entrySet()) {
            Object thisValue = entry.getValue();
            Object otherValue = other.state.get(entry.getKey());
            if (thisValue == null && otherValue == null) {
                continue;
            }
            if (thisValue == null || !thisValue.equals(otherValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ProviderSnapshot{" +
                "providerId='" + providerId + '\'' +
                ", entries=" + state.size() +
                ", age=" + getAgeMs() + "ms" +
                ", validated=" + validated +
                '}';
    }
}
