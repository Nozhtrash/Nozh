package dev.nozh.core.state;

/**
 * Exception thrown when state migration fails.
 */
public final class StateMigrationException extends RuntimeException {

    public StateMigrationException(String message) {
        super(message);
    }

    public StateMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
