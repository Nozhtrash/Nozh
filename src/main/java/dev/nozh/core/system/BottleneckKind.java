package dev.nozh.core.system;

/**
 * High-level classification for the current bottleneck.
 */
public enum BottleneckKind {
    CPU,
    GPU,
    MIXED,
    UNKNOWN
}
