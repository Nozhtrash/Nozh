package dev.nozh.core.ui;

public enum HudMode {
    COMPACT("nozh.hud.mode.compact"),
    ANALYST("nozh.hud.mode.analyst"),
    EXPERT("nozh.hud.mode.expert");

    private final String translationKey;

    HudMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public static HudMode fromConfig(String value) {
        if (value == null) {
            return ANALYST;
        }
        try {
            return HudMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ANALYST;
        }
    }

    public HudMode next() {
        HudMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
