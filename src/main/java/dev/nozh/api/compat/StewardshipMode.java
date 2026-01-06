package dev.nozh.api.compat;

public enum StewardshipMode {
    NONE,
    EXCLUSIVE,
    SHARED;

    public boolean isExclusive() {
        return this == EXCLUSIVE;
    }

    public boolean isShared() {
        return this == SHARED;
    }
}
