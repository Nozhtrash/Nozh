package dev.nozh.core.benchmark;

import java.util.List;

/**
 * Benchmark matrix for environment coverage and scenario usage.
 */
public record BenchmarkMatrix(
        List<BenchmarkEnvironment> environments,
        List<BenchmarkScenario> scenarios) {

    public static BenchmarkMatrix defaultMatrix() {
        List<BenchmarkEnvironment> environments = List.of(
                new BenchmarkEnvironment("low-tier CPU + iGPU", "1280x720", "vanilla+lite", "cpu-bound"),
                new BenchmarkEnvironment("mid-tier CPU + mid GPU", "1920x1080", "modpack", "balanced"),
                new BenchmarkEnvironment("high-tier CPU + high GPU", "2560x1440", "shader-heavy", "gpu-bound"),
                new BenchmarkEnvironment("high-tier CPU + high GPU", "1920x1080", "large-modpack (200+ mods)",
                        "modpack-heavy"),
                new BenchmarkEnvironment("enthusiast CPU + high GPU", "2560x1440", "mega-modpack (350+ mods + shaders)",
                        "modpack-compare"));
        List<BenchmarkScenario> scenarios = List.of(
                new BenchmarkScenario("MENU", "Launcher/menu navigation", 45),
                new BenchmarkScenario("EXPLORING", "Overworld traversal with chunk generation", 120),
                new BenchmarkScenario("COMBAT", "High-entity combat encounter", 90),
                new BenchmarkScenario("BUILDING", "Structure placement with redstone updates", 90),
                new BenchmarkScenario("LOADING", "Dimension or world load transition", 60),
                new BenchmarkScenario("MODPACK_COMPARE", "Comparative pass vs baseline in large modpacks", 120));
        return new BenchmarkMatrix(environments, scenarios);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"environments\": [\n");
        for (int i = 0; i < environments.size(); i++) {
            sb.append("    ").append(environments.get(i).toJson());
            if (i < environments.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ],\n");
        sb.append("  \"scenarios\": [\n");
        for (int i = 0; i < scenarios.size(); i++) {
            sb.append("    ").append(scenarios.get(i).toJson());
            if (i < scenarios.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }
}
