package dev.nozh.core.profiler;

public record StutterCause(
        String causeKey,
        String detail,
        double confidence) {

    public static StutterCause unknown() {
        return new StutterCause("nozh.hud.stutter.unknown", "", 0.0);
    }
}
