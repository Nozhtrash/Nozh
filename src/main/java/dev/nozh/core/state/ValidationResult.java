package dev.nozh.core.state;

/**
 * Validation result from StateInvariantValidator.
 * 
 * Contract 1: State mutations MUST be validated before applying.
 */
public sealed interface ValidationResult {

    /**
     * Validation passed. State is valid.
     */
    record Valid() implements ValidationResult {
    }

    /**
     * Validation failed. State violates invariants.
     * 
     * @param violations List of violated invariants
     */
    record Invalid(java.util.List<String> violations) implements ValidationResult {
        public Invalid {
            violations = java.util.List.copyOf(violations); // Immutable
        }

        public String formatViolations() {
            return String.join("; ", violations);
        }
    }

    default boolean isValid() {
        return this instanceof Valid;
    }

    default boolean isInvalid() {
        return this instanceof Invalid;
    }
}
