package dev.nozh.fabric.priority2;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * v0.2: Applies Priority2 suggestion IDs to real client-side settings.
 *
 * <p>Design goals:
 * - Must never crash the game if mappings/methods differ across mappings.
 * - Prefer reflection for optional fields (viewDistance, simulationDistance, etc.).
 * - Always attempt to persist options (best-effort).
 */
public final class Priority2ActionApplier {

    public static final String SUGGEST_CPU_REDUCE_ENTITIES = "cpu.reduce_entities";
    public static final String SUGGEST_GPU_REDUCE_SHADERS = "gpu.reduce_shaders";
    public static final String SUGGEST_GPU_REDUCE_PARTICLES = "gpu.reduce_particles";
    public static final String SUGGEST_COMBAT_STABILIZE = "scenario.combat_stabilize";

    public enum ApplyResult {
        APPLIED,
        NOOP,
        FAILED
    }

    public static final class Result {
        public final ApplyResult result;
        public final String message;

        public Result(ApplyResult result, String message) {
            this.result = result;
            this.message = message == null ? "" : message;
        }
    }

    public Result applySuggestion(MinecraftClient client, String suggestionId) {
        if (client == null || client.options == null) {
            return new Result(ApplyResult.FAILED, "Client/options unavailable");
        }
        if (suggestionId == null || suggestionId.isBlank()) {
            return new Result(ApplyResult.NOOP, "Empty suggestion");
        }

        try {
            switch (suggestionId) {
                case SUGGEST_GPU_REDUCE_PARTICLES -> {
                    boolean changed = decreaseParticles(client);
                    persistOptions(client);
                    return new Result(changed ? ApplyResult.APPLIED : ApplyResult.NOOP, "Particles decreased");
                }
                case SUGGEST_GPU_REDUCE_SHADERS -> {
                    // Can't disable Iris shaders reliably from a generic mod; fall back to reducing particle load.
                    boolean changed = decreaseParticles(client);
                    persistOptions(client);
                    return new Result(changed ? ApplyResult.APPLIED : ApplyResult.NOOP, "Shader cost mitigation: particles decreased");
                }
                case SUGGEST_CPU_REDUCE_ENTITIES -> {
                    boolean changed = decreaseViewDistance(client) | decreaseSimulationDistance(client) | decreaseEntityDistanceScale(client);
                    persistOptions(client);
                    return new Result(changed ? ApplyResult.APPLIED : ApplyResult.NOOP, "CPU load mitigation: distances decreased");
                }
                case SUGGEST_COMBAT_STABILIZE -> {
                    boolean changed = decreaseParticles(client) | capFps(client, 90);
                    persistOptions(client);
                    return new Result(changed ? ApplyResult.APPLIED : ApplyResult.NOOP, "Combat stabilize: particles reduced (and FPS capped if needed)");
                }
                default -> {
                    return new Result(ApplyResult.NOOP, "Unknown suggestion id: " + suggestionId);
                }
            }
        } catch (Throwable t) {
            NozhConstants.LOGGER.warn("Failed to apply suggestion " + suggestionId, t);
            return new Result(ApplyResult.FAILED, "Exception while applying: " + t.getClass().getSimpleName());
        }
    }

    /**
     * v0.3-ish: gradually restore quality when stable.
     */
    public boolean tryGradualRecovery(MinecraftClient client, boolean performanceVeryGood) {
        if (!performanceVeryGood) return false;
        boolean changed = false;
        changed |= increaseViewDistance(client);
        changed |= increaseSimulationDistance(client);
        changed |= increaseParticles(client);
        if (changed) {
            persistOptions(client);
        }
        return changed;
    }

    // -----------------
    // Particles
    // -----------------

    private boolean decreaseParticles(MinecraftClient client) {
        Object opt = findOption(client.options, "particles");
        if (opt == null) {
            return false;
        }

        // Try calling opt.getValue() and opt.setValue(next)
        try {
            Method getValue = opt.getClass().getMethod("getValue");
            Object current = getValue.invoke(opt);

            Object next = pickNextEnum(current, -1);
            if (next == null || next == current) {
                return false;
            }

            Method setValue = opt.getClass().getMethod("setValue", current.getClass());
            setValue.invoke(opt, next);
            return true;
        } catch (Throwable ignored) {
        }

        return false;
    }

    private boolean increaseParticles(MinecraftClient client) {
        Object opt = findOption(client.options, "particles");
        if (opt == null) {
            return false;
        }
        try {
            Method getValue = opt.getClass().getMethod("getValue");
            Object current = getValue.invoke(opt);

            Object next = pickNextEnum(current, +1);
            if (next == null || next == current) {
                return false;
            }

            Method setValue = opt.getClass().getMethod("setValue", current.getClass());
            setValue.invoke(opt, next);
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    // -----------------
    // Distances
    // -----------------

    private boolean decreaseViewDistance(MinecraftClient client) {
        return stepIntOption(client.options, "viewDistance", -2, 2, 32);
    }

    private boolean increaseViewDistance(MinecraftClient client) {
        return stepIntOption(client.options, "viewDistance", +2, 2, 32);
    }

    private boolean decreaseSimulationDistance(MinecraftClient client) {
        return stepIntOption(client.options, "simulationDistance", -1, 2, 32);
    }

    private boolean increaseSimulationDistance(MinecraftClient client) {
        return stepIntOption(client.options, "simulationDistance", +1, 2, 32);
    }

    private boolean decreaseEntityDistanceScale(MinecraftClient client) {
        // Some mappings call it entityDistanceScaling; it's a double option in [0.5..1.0] typically.
        Object opt = findOption(client.options, "entityDistanceScaling");
        if (opt == null) return false;
        try {
            Method getValue = opt.getClass().getMethod("getValue");
            Object current = getValue.invoke(opt);
            if (!(current instanceof Double d) || !Double.isFinite(d)) return false;
            double next = Math.max(0.5, d - 0.1);
            if (next == d) return false;
            Method setValue = opt.getClass().getMethod("setValue", Double.class);
            setValue.invoke(opt, next);
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private boolean capFps(MinecraftClient client, int maxFps) {
        Object opt = findOption(client.options, "maxFps");
        if (opt == null) return false;
        try {
            Method getValue = opt.getClass().getMethod("getValue");
            Object current = getValue.invoke(opt);
            if (!(current instanceof Integer cur)) return false;
            if (cur <= maxFps) return false;
            Method setValue = opt.getClass().getMethod("setValue", Integer.class);
            setValue.invoke(opt, maxFps);
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    // -----------------
    // Reflection helpers
    // -----------------

    private static Object findOption(Object options, String fieldName) {
        try {
            Field f = options.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(options);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean stepIntOption(Object options, String fieldName, int delta, int min, int max) {
        Object opt = findOption(options, fieldName);
        if (opt == null) return false;
        try {
            Method getValue = opt.getClass().getMethod("getValue");
            Object current = getValue.invoke(opt);
            if (!(current instanceof Integer cur)) return false;
            int next = clamp(cur + delta, min, max);
            if (next == cur) return false;
            Method setValue = opt.getClass().getMethod("setValue", Integer.class);
            setValue.invoke(opt, next);
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static Object pickNextEnum(Object current, int direction) {
        if (current == null) return null;
        Class<?> c = current.getClass();
        if (!c.isEnum()) return null;
        Object[] values = c.getEnumConstants();
        if (values == null || values.length == 0) return null;

        int idx = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                idx = i;
                break;
            }
        }
        if (idx < 0) return null;

        int next = idx + direction;
        if (next < 0) next = 0;
        if (next >= values.length) next = values.length - 1;
        return values[next];
    }

    private static void persistOptions(MinecraftClient client) {
        try {
            // Yarn: options.write() exists; keep it reflection-safe.
            Method write = client.options.getClass().getMethod("write");
            write.invoke(client.options);
        } catch (Throwable ignored) {
        }
    }
}
