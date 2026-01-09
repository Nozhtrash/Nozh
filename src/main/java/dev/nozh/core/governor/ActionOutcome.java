package dev.nozh.core.governor;

public enum ActionOutcome {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
    PENDING;
    
    public boolean isSuccess() {
        return this == POSITIVE;
    }
    
    public boolean isFailure() {
        return this == NEGATIVE;
    }
}
