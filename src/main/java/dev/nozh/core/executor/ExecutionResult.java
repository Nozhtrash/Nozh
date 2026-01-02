package dev.nozh.core.executor;

public record ExecutionResult(
        ExecutionStatus status,
        String message,
        long timestampMillis) {
}
