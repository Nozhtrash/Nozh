package dev.nozh.core.executor;

import dev.nozh.api.governor.ActionType;

public record ExecutedAction(
        long timestamp,
        ActionType type,
        String oldValue,
        String newValue) {
}
