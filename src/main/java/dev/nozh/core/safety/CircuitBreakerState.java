package dev.nozh.core.safety;

/**
 * Circuit breaker states.
 * 
 * @author Nozh Team
 * @since 0.4.0
 */
public enum CircuitBreakerState {
    /**
     * Normal operation. Requests are allowed.
     * Transitions to OPEN when failure threshold is exceeded.
     */
    CLOSED,
    
    /**
     * Circuit is open. Requests are rejected immediately.
     * Transitions to HALF_OPEN after timeout period.
     */
    OPEN,
    
    /**
     * Testing recovery. Limited requests allowed.
     * Transitions to CLOSED if successful, back to OPEN if failures continue.
     */
    HALF_OPEN
}
