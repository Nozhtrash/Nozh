package dev.nozh.core.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.compatibility.ModConflictDetector;

/**
 * Service to detect other optimization mods and provide compatibility warnings.
 * Safe, read-only, no hard dependencies.
 * 
 * Phase 9: Detailed detection + i18n
 */
public class CompatService {

    public record CompatReport(
            List<String> performanceMods,
            List<String> shaderMods,
            List<String> worldGenMods,
            List<String> diagnosicMods,
            List<Text> hints) {
    }

    public record CapabilitySteward(
            CapabilityId capabilityId,
            String steward,
            boolean conflict) {
    }

    public static CompatReport generateReport() {
        List<String> perf = new ArrayList<>();
        List<String> shaders = new ArrayList<>();
        List<String> world = new ArrayList<>();
        List<String> diag = new ArrayList<>();
        List<Text> hints = new ArrayList<>();

        // Performance
        check(perf, "sodium", "Sodium");
        check(perf, "sodium-extra", "Sodium Extra");
        check(perf, "lithium", "Lithium");
        check(perf, "starlight", "Starlight");
        check(perf, "ferritecore", "FerriteCore");
        check(perf, "modernfix", "ModernFix");
        check(perf, "entityculling", "EntityCulling");
        check(perf, "immediatelyfast", "ImmediatelyFast");
        check(perf, "dynamic_fps", "Dynamic FPS");
        check(perf, "threadtweak", "ThreadTweak");
        check(perf, "krypton", "Krypton");
        check(perf, "gpumemleakfix", "GpuMemLeakFix");
        check(perf, "nvidium", "Nvidium");

        // Shaders
        check(shaders, "iris", "Iris");
        check(shaders, "oculus", "Oculus");

        // WorldGen
        check(world, "terralith", "Terralith");
        check(world, "distant-horizons", "Distant Horizons");

        // Diagnostics
        check(diag, "spark", "spark");
        check(diag, "notenoughcrashes", "Not Enough Crashes");
        check(diag, "neruina", "Neruina");
        check(diag, "observable", "Observable");

        // Hints & Risks (Translatable)
        if (!shaders.isEmpty()) {
            hints.add(Text.translatable("nozh.hint.shaders"));
        }
        if (FabricLoader.getInstance().isModLoaded("distant-horizons")) {
            hints.add(Text.translatable("nozh.hint.distant_horizons"));
        }
        if (FabricLoader.getInstance().isModLoaded("dynamic_fps")) {
            hints.add(Text.translatable("nozh.hint.dynamic_fps"));
        }
        if (FabricLoader.getInstance().isModLoaded("sodium-extra")) {
            hints.add(Text.translatable("nozh.hint.sodium_extra"));
        }
        SodiumCompat.getVersion().ifPresent(version -> hints.add(
                Text.literal("Sodium version: " + version)));
        IrisCompat.getShaderStatus().ifPresent(active -> hints.add(
                Text.literal("Iris shaders: " + (active ? "active" : "inactive"))));
        if (!perf.contains("sodium") && !shaders.contains("iris")) {
            // Maybe a hint suggesting sodium? No, "Non-Goals": we don't fix user's setup,
            // we governs.
        }

        return new CompatReport(perf, shaders, world, diag, hints);
    }

    public static List<CapabilitySteward> generateStewardReport() {
        ModConflictDetector detector = new ModConflictDetector();
        List<CapabilitySteward> stewards = new ArrayList<>();

        for (CapabilityId capability : CapabilityId.values()) {
            boolean conflict = detector.hasConflict(capability);
            String steward = detector.getSteward(capability);
            stewards.add(new CapabilitySteward(capability, steward, conflict));
        }

        return stewards;
    }

    public static String getSteward(CapabilityId capability) {
        return new ModConflictDetector().getSteward(capability);
    }

    private static void check(List<String> list, String modid, String name) {
        if (FabricLoader.getInstance().isModLoaded(modid)) {
            list.add(name);
        }
    }
}
