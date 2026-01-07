package dev.nozh.core.testing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record QuickTestPlan(
    int warmupSeconds,
    int measurementSeconds,
    int cooldownSeconds,
    List<ScenarioDefinition> scenarios
) {
    public Map<String, Integer> timingMap() {
        Map<String, Integer> timing = new LinkedHashMap<>();
        timing.put("warmup_seconds", warmupSeconds);
        timing.put("measurement_seconds", measurementSeconds);
        timing.put("cooldown_seconds", cooldownSeconds);
        return timing;
    }
}
