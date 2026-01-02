package dev.nozh.core.state;

/**
 * Exception thrown when a state invariant is violated.
 * 
 * Contract 1: State mutations that fail validation MUST NOT be applied.
 */
public final class StateInvariantViolationException extends RuntimeException {

    public StateInvariantViolationException(String message) {
        super(message);
    }

    public StateInvariantViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
