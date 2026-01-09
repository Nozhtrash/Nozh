package dev.nozh.core.profiler;

public enum SpikeCauseType {
    CRITICAL_EVENT("nozh.hud.stutter.critical"),
    GC("nozh.hud.stutter.gc"),
    TICK("nozh.hud.stutter.tick"),
    RENDER("nozh.hud.stutter.render"),
    FRAME("nozh.hud.stutter.frame"),
    UNKNOWN("nozh.hud.stutter.unknown");

    private final String causeKey;

    SpikeCauseType(String causeKey) {
        this.causeKey = causeKey;
    }

    public String causeKey() {
        return causeKey;
    }
}
