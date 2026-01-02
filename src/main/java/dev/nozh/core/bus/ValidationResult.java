package dev.nozh.core.bus;

/**
 * Validation result for commands.
 * 
 * Sealed: Valid | Invalid
 */
public sealed interface ValidationResult {

    /**
     * Command is valid and can be queued.
     */
    record Valid() implements ValidationResult {
    }

    /**
     * Command is invalid and rejected.
     * 
     * @param reason Human-readable rejection reason
     */
    record Invalid(String reason) implements ValidationResult {
    }

    default boolean isValid() {
        return this instanceof Valid;
    }

    default boolean isInvalid() {
        return this instanceof Invalid;
    }
}
