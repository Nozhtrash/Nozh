package dev.nozh.core.safety;

/**
 * Exception thrown when circuit breaker is open.
 * 
 * @author Nozh Team
 * @since 0.4.0
 */
public class CircuitBreakerOpenException extends RuntimeException {
    
    /**
     * Constructs a new CircuitBreakerOpenException.
     * 
     * @param message error message
     */
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new CircuitBreakerOpenException with cause.
     * 
     * @param message error message
     * @param cause underlying cause
     */
    public CircuitBreakerOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
