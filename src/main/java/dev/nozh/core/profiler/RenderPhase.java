package dev.nozh.core.profiler;

public enum RenderPhase {
    FRAME("nozh.hud.render_phase.frame"),
    CLIENT_TICK("nozh.hud.render_phase.client_tick"),
    WORLD("nozh.hud.render_phase.world"),
    ENTITIES("nozh.hud.render_phase.entities"),
    BLOCK_ENTITIES("nozh.hud.render_phase.block_entities"),
    PARTICLES("nozh.hud.render_phase.particles"),
    HUD("nozh.hud.render_phase.hud"),
    UNKNOWN("nozh.hud.render_phase.unknown");

    private final String translationKey;

    RenderPhase(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
