package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.governor.OptimizationProfile;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ActionMatrixRules {

    private final Map<Scenario, Map<OptimizationProfile, ScenarioRuleSet>> rules;
    private final Map<Scenario, Map<OptimizationProfile, ScenarioLimitSet>> limits;

    private ActionMatrixRules(Map<Scenario, Map<OptimizationProfile, ScenarioRuleSet>> rules,
            Map<Scenario, Map<OptimizationProfile, ScenarioLimitSet>> limits) {
        this.rules = rules;
        this.limits = limits;
    }

    public static ActionMatrixRules defaultRules() {
        return builder()
                .registerScenario(Scenario.COMBAT, combatRules())
                .registerScenario(Scenario.AFK, afkRules())
                .registerScenario(Scenario.MINING, miningRules())
                .registerScenario(Scenario.BUILDING, buildingRules())
                .registerScenario(Scenario.MENU, menuRules())
                .registerScenario(Scenario.LOADING, loadingRules())
                .registerScenarioLimits(Scenario.COMBAT, combatLimits())
                .registerScenarioLimits(Scenario.AFK, afkLimits())
                .build();
    }

    public CapabilityValue resolveTarget(CapabilityId id, Scenario scenario, OptimizationProfile profile) {
        ScenarioRuleSet ruleSet = ruleSet(scenario, profile);
        if (ruleSet == null) {
            return null;
        }
        return ruleSet.resolve(id);
    }

    public double ruleWeight(CapabilityId id, Scenario scenario, OptimizationProfile profile) {
        ScenarioRuleSet ruleSet = ruleSet(scenario, profile);
        if (ruleSet == null) {
            return 0.0;
        }
        return ruleSet.hasRule(id) ? 1.0 : 0.0;
    }

    public CapabilityValue applyLimits(CapabilityId id, Scenario scenario, OptimizationProfile profile,
            CapabilityValue value) {
        ScenarioLimitSet limitSet = limitSet(scenario, profile);
        if (limitSet == null) {
            return value;
        }
        return limitSet.apply(id, value);
    }

    public ScenarioRuleSet ruleSet(Scenario scenario, OptimizationProfile profile) {
        if (scenario == null) {
            return null;
        }
        Map<OptimizationProfile, ScenarioRuleSet> perProfile = rules.get(scenario);
        if (perProfile == null || perProfile.isEmpty()) {
            return null;
        }
        OptimizationProfile resolvedProfile = profile != null ? profile : OptimizationProfile.BALANCED;
        return perProfile.get(resolvedProfile);
    }

    public ScenarioLimitSet limitSet(Scenario scenario, OptimizationProfile profile) {
        if (scenario == null) {
            return null;
        }
        Map<OptimizationProfile, ScenarioLimitSet> perProfile = limits.get(scenario);
        if (perProfile == null || perProfile.isEmpty()) {
            return null;
        }
        OptimizationProfile resolvedProfile = profile != null ? profile : OptimizationProfile.BALANCED;
        return perProfile.get(resolvedProfile);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Scenario, Map<OptimizationProfile, ScenarioRuleSet>> rules =
                new EnumMap<>(Scenario.class);
        private final Map<Scenario, Map<OptimizationProfile, ScenarioLimitSet>> limits =
                new EnumMap<>(Scenario.class);

        public Builder registerScenario(Scenario scenario, Map<OptimizationProfile, ScenarioRuleSet> perProfile) {
            rules.put(scenario, Collections.unmodifiableMap(new EnumMap<>(perProfile)));
            return this;
        }

        public Builder registerScenarioLimits(Scenario scenario,
                Map<OptimizationProfile, ScenarioLimitSet> perProfile) {
            limits.put(scenario, Collections.unmodifiableMap(new EnumMap<>(perProfile)));
            return this;
        }

        public ActionMatrixRules build() {
            return new ActionMatrixRules(Collections.unmodifiableMap(rules), Collections.unmodifiableMap(limits));
        }
    }

    static Map<OptimizationProfile, ScenarioRuleSet> combatRules() {
        Map<CapabilityId, CapabilityValue> aggressive = new EnumMap<>(CapabilityId.class);
        Map<CapabilityId, CapabilityValue> balanced = new EnumMap<>(CapabilityId.class);

        aggressive.put(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("MINIMAL"));
        balanced.put(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("MINIMAL"));
        aggressive.put(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("OFF"));
        balanced.put(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("OFF"));
        aggressive.put(CapabilityId.ENTITY_SHADOWS, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.ENTITY_SHADOWS, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.RENDER_DISTANCE, new CapabilityValue.IntValue(6));
        balanced.put(CapabilityId.RENDER_DISTANCE, new CapabilityValue.IntValue(8));
        aggressive.put(CapabilityId.SIMULATION_DISTANCE, new CapabilityValue.IntValue(4));
        balanced.put(CapabilityId.SIMULATION_DISTANCE, new CapabilityValue.IntValue(6));
        aggressive.put(CapabilityId.ENTITY_DISTANCE, new CapabilityValue.IntValue(60));
        balanced.put(CapabilityId.ENTITY_DISTANCE, new CapabilityValue.IntValue(70));
        aggressive.put(CapabilityId.BIOME_BLEND, new CapabilityValue.IntValue(1));
        balanced.put(CapabilityId.BIOME_BLEND, new CapabilityValue.IntValue(2));
        aggressive.put(CapabilityId.MIPMAP_LEVEL, new CapabilityValue.IntValue(1));
        balanced.put(CapabilityId.MIPMAP_LEVEL, new CapabilityValue.IntValue(2));
        aggressive.put(CapabilityId.VSYNC, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.VSYNC, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.FOG, new CapabilityValue.IntValue(6));
        balanced.put(CapabilityId.FOG, new CapabilityValue.IntValue(8));
        aggressive.put(CapabilityId.GRAPHICS_MODE, new CapabilityValue.EnumValue("FAST"));
        balanced.put(CapabilityId.GRAPHICS_MODE, new CapabilityValue.EnumValue("FAST"));
        aggressive.put(CapabilityId.SMOOTH_LIGHTING, new CapabilityValue.EnumValue("OFF"));
        balanced.put(CapabilityId.SMOOTH_LIGHTING, new CapabilityValue.EnumValue("OFF"));
        aggressive.put(CapabilityId.ARMOR_STANDS, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.ARMOR_STANDS, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.ITEM_FRAMES, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.ITEM_FRAMES, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.BLOCK_ENTITIES, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.BLOCK_ENTITIES, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.ANIMATIONS, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.ANIMATIONS, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.DYNAMIC_LIGHTING, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.DYNAMIC_LIGHTING, new CapabilityValue.BoolValue(false));

        return perProfile(aggressive, balanced);
    }

    static Map<OptimizationProfile, ScenarioRuleSet> afkRules() {
        Map<CapabilityId, CapabilityValue> aggressive = new EnumMap<>(CapabilityId.class);
        Map<CapabilityId, CapabilityValue> balanced = new EnumMap<>(CapabilityId.class);

        aggressive.put(CapabilityId.ENTITY_DISTANCE, new CapabilityValue.IntValue(60));
        balanced.put(CapabilityId.ENTITY_DISTANCE, new CapabilityValue.IntValue(70));
        aggressive.put(CapabilityId.ARMOR_STANDS, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.ARMOR_STANDS, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.ITEM_FRAMES, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.ITEM_FRAMES, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.BLOCK_ENTITIES, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.BLOCK_ENTITIES, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.ANIMATIONS, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.ANIMATIONS, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.FPS_CAP, new CapabilityValue.IntValue(60));
        balanced.put(CapabilityId.FPS_CAP, new CapabilityValue.IntValue(60));

        return perProfile(aggressive, balanced);
    }

    static Map<OptimizationProfile, ScenarioRuleSet> miningRules() {
        Map<CapabilityId, CapabilityValue> aggressive = new EnumMap<>(CapabilityId.class);
        Map<CapabilityId, CapabilityValue> balanced = new EnumMap<>(CapabilityId.class);

        aggressive.put(CapabilityId.RENDER_DISTANCE, new CapabilityValue.IntValue(4));
        balanced.put(CapabilityId.RENDER_DISTANCE, new CapabilityValue.IntValue(6));
        aggressive.put(CapabilityId.SIMULATION_DISTANCE, new CapabilityValue.IntValue(4));
        balanced.put(CapabilityId.SIMULATION_DISTANCE, new CapabilityValue.IntValue(6));

        return perProfile(aggressive, balanced);
    }

    static Map<OptimizationProfile, ScenarioRuleSet> buildingRules() {
        Map<CapabilityId, CapabilityValue> aggressive = new EnumMap<>(CapabilityId.class);
        Map<CapabilityId, CapabilityValue> balanced = new EnumMap<>(CapabilityId.class);

        aggressive.put(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("DECREASED"));
        balanced.put(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("DECREASED"));
        aggressive.put(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("FAST"));
        balanced.put(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("FAST"));
        aggressive.put(CapabilityId.ENTITY_SHADOWS, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.ENTITY_SHADOWS, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.BIOME_BLEND, new CapabilityValue.IntValue(2));
        balanced.put(CapabilityId.BIOME_BLEND, new CapabilityValue.IntValue(3));

        return perProfile(aggressive, balanced);
    }

    static Map<OptimizationProfile, ScenarioRuleSet> menuRules() {
        Map<CapabilityId, CapabilityValue> aggressive = new EnumMap<>(CapabilityId.class);
        Map<CapabilityId, CapabilityValue> balanced = new EnumMap<>(CapabilityId.class);

        aggressive.put(CapabilityId.FPS_CAP, new CapabilityValue.IntValue(60));
        balanced.put(CapabilityId.FPS_CAP, new CapabilityValue.IntValue(60));
        aggressive.put(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("MINIMAL"));
        balanced.put(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("DECREASED"));
        aggressive.put(CapabilityId.DYNAMIC_LIGHTING, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.DYNAMIC_LIGHTING, new CapabilityValue.BoolValue(false));

        return perProfile(aggressive, balanced);
    }

    static Map<OptimizationProfile, ScenarioRuleSet> loadingRules() {
        Map<CapabilityId, CapabilityValue> aggressive = new EnumMap<>(CapabilityId.class);
        Map<CapabilityId, CapabilityValue> balanced = new EnumMap<>(CapabilityId.class);

        aggressive.put(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("OFF"));
        balanced.put(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("OFF"));
        aggressive.put(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("MINIMAL"));
        balanced.put(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("MINIMAL"));
        aggressive.put(CapabilityId.ANIMATIONS, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.ANIMATIONS, new CapabilityValue.BoolValue(false));
        aggressive.put(CapabilityId.VSYNC, new CapabilityValue.BoolValue(false));
        balanced.put(CapabilityId.VSYNC, new CapabilityValue.BoolValue(false));

        return perProfile(aggressive, balanced);
    }

    static Map<OptimizationProfile, ScenarioLimitSet> combatLimits() {
        Map<CapabilityId, CapabilityLimit> aggressive = new EnumMap<>(CapabilityId.class);
        Map<CapabilityId, CapabilityLimit> balanced = new EnumMap<>(CapabilityId.class);

        aggressive.put(CapabilityId.PARTICLES, CapabilityLimit.minEnum("DECREASED"));
        balanced.put(CapabilityId.PARTICLES, CapabilityLimit.minEnum("DECREASED"));
        aggressive.put(CapabilityId.RENDER_DISTANCE, CapabilityLimit.minInt(6));
        balanced.put(CapabilityId.RENDER_DISTANCE, CapabilityLimit.minInt(8));
        aggressive.put(CapabilityId.SIMULATION_DISTANCE, CapabilityLimit.minInt(4));
        balanced.put(CapabilityId.SIMULATION_DISTANCE, CapabilityLimit.minInt(6));
        aggressive.put(CapabilityId.ENTITY_DISTANCE, CapabilityLimit.minInt(65));
        balanced.put(CapabilityId.ENTITY_DISTANCE, CapabilityLimit.minInt(75));

        return perProfileLimits(aggressive, balanced);
    }

    static Map<OptimizationProfile, ScenarioLimitSet> afkLimits() {
        Map<CapabilityId, CapabilityLimit> aggressive = new EnumMap<>(CapabilityId.class);
        Map<CapabilityId, CapabilityLimit> balanced = new EnumMap<>(CapabilityId.class);

        aggressive.put(CapabilityId.RENDER_DISTANCE, CapabilityLimit.minInt(4));
        balanced.put(CapabilityId.RENDER_DISTANCE, CapabilityLimit.minInt(6));
        aggressive.put(CapabilityId.SIMULATION_DISTANCE, CapabilityLimit.minInt(4));
        balanced.put(CapabilityId.SIMULATION_DISTANCE, CapabilityLimit.minInt(5));
        aggressive.put(CapabilityId.ENTITY_DISTANCE, CapabilityLimit.minInt(60));
        balanced.put(CapabilityId.ENTITY_DISTANCE, CapabilityLimit.minInt(70));
        aggressive.put(CapabilityId.FPS_CAP, CapabilityLimit.maxInt(60));
        balanced.put(CapabilityId.FPS_CAP, CapabilityLimit.maxInt(60));

        return perProfileLimits(aggressive, balanced);
    }

    private static Map<OptimizationProfile, ScenarioRuleSet> perProfile(
            Map<CapabilityId, CapabilityValue> aggressive,
            Map<CapabilityId, CapabilityValue> balanced) {
        Map<OptimizationProfile, ScenarioRuleSet> perProfile = new EnumMap<>(OptimizationProfile.class);
        perProfile.put(OptimizationProfile.AGGRESSIVE, new ScenarioRuleSet(aggressive));
        perProfile.put(OptimizationProfile.BALANCED, new ScenarioRuleSet(balanced));
        
        // Extreme Profile (Derived from Aggressive)
        Map<CapabilityId, CapabilityValue> extreme = new HashMap<>(aggressive);
        extreme.put(CapabilityId.RENDER_DISTANCE, new CapabilityValue.IntValue(2));
        extreme.put(CapabilityId.SIMULATION_DISTANCE, new CapabilityValue.IntValue(2));
        extreme.put(CapabilityId.ENTITY_DISTANCE, new CapabilityValue.IntValue(24)); // Minimum useful
        extreme.put(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("MINIMAL"));
        extreme.put(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("OFF"));
        extreme.put(CapabilityId.ENTITY_SHADOWS, new CapabilityValue.BoolValue(false)); 
        extreme.put(CapabilityId.ANIMATIONS, new CapabilityValue.BoolValue(false));
        extreme.put(CapabilityId.BIOME_BLEND, new CapabilityValue.IntValue(0));
        extreme.put(CapabilityId.MIPMAP_LEVEL, new CapabilityValue.IntValue(0));
        extreme.put(CapabilityId.SMOOTH_LIGHTING, new CapabilityValue.EnumValue("OFF"));
        extreme.put(CapabilityId.GRAPHICS_MODE, new CapabilityValue.EnumValue("FAST"));
        extreme.put(CapabilityId.FOG, new CapabilityValue.IntValue(2));
        extreme.put(CapabilityId.VSYNC, new CapabilityValue.BoolValue(false));
        extreme.put(CapabilityId.ARMOR_STANDS, new CapabilityValue.BoolValue(false));
        extreme.put(CapabilityId.ITEM_FRAMES, new CapabilityValue.BoolValue(false));
        extreme.put(CapabilityId.BLOCK_ENTITIES, new CapabilityValue.BoolValue(false));
        extreme.put(CapabilityId.DYNAMIC_LIGHTING, new CapabilityValue.BoolValue(false));
         
        perProfile.put(OptimizationProfile.EXTREME, new ScenarioRuleSet(extreme));
        perProfile.put(OptimizationProfile.CONSERVATIVE, new ScenarioRuleSet(balanced));
        
        return perProfile;
    }

    private static Map<OptimizationProfile, ScenarioLimitSet> perProfileLimits(
            Map<CapabilityId, CapabilityLimit> aggressive,
            Map<CapabilityId, CapabilityLimit> balanced) {
        Map<OptimizationProfile, ScenarioLimitSet> perProfile = new EnumMap<>(OptimizationProfile.class);
        perProfile.put(OptimizationProfile.AGGRESSIVE, new ScenarioLimitSet(aggressive));
        perProfile.put(OptimizationProfile.BALANCED, new ScenarioLimitSet(balanced));
        
        Map<CapabilityId, CapabilityLimit> extreme = new HashMap<>(aggressive);
        extreme.put(CapabilityId.RENDER_DISTANCE, CapabilityLimit.maxInt(2));
        extreme.put(CapabilityId.SIMULATION_DISTANCE, CapabilityLimit.maxInt(2));
        extreme.put(CapabilityId.PARTICLES, CapabilityLimit.minEnum("MINIMAL"));
        
        perProfile.put(OptimizationProfile.EXTREME, new ScenarioLimitSet(extreme));
        perProfile.put(OptimizationProfile.CONSERVATIVE, new ScenarioLimitSet(balanced));
        
        return perProfile;
    }

    static final class ScenarioRuleSet {
        private final Map<CapabilityId, CapabilityValue> rules;

        private ScenarioRuleSet(Map<CapabilityId, CapabilityValue> rules) {
            this.rules = rules;
        }

        private CapabilityValue resolve(CapabilityId id) {
            return rules.get(id);
        }

        private boolean hasRule(CapabilityId id) {
            return rules.containsKey(id);
        }
    }

    static final class ScenarioLimitSet {
        private final Map<CapabilityId, CapabilityLimit> limits;

        private ScenarioLimitSet(Map<CapabilityId, CapabilityLimit> limits) {
            this.limits = limits;
        }

        private CapabilityValue apply(CapabilityId id, CapabilityValue value) {
            if (value == null || limits == null) {
                return value;
            }
            CapabilityLimit limit = limits.get(id);
            if (limit == null) {
                return value;
            }
            return limit.clamp(id, value);
        }
    }

    static final class CapabilityLimit {
        private final CapabilityValue min;
        private final CapabilityValue max;

        private CapabilityLimit(CapabilityValue min, CapabilityValue max) {
            this.min = min;
            this.max = max;
        }

        static CapabilityLimit minInt(int minValue) {
            return new CapabilityLimit(new CapabilityValue.IntValue(minValue), null);
        }

        static CapabilityLimit maxInt(int maxValue) {
            return new CapabilityLimit(null, new CapabilityValue.IntValue(maxValue));
        }

        static CapabilityLimit minEnum(String minValue) {
            return new CapabilityLimit(new CapabilityValue.EnumValue(minValue), null);
        }

        CapabilityValue clamp(CapabilityId id, CapabilityValue value) {
            if (value == null) {
                return value;
            }
            if (value instanceof CapabilityValue.IntValue intValue) {
                int resolved = intValue.value();
                if (min instanceof CapabilityValue.IntValue minInt) {
                    resolved = Math.max(resolved, minInt.value());
                }
                if (max instanceof CapabilityValue.IntValue maxInt) {
                    resolved = Math.min(resolved, maxInt.value());
                }
                return resolved == intValue.value() ? value : new CapabilityValue.IntValue(resolved);
            }
            if (value instanceof CapabilityValue.EnumValue enumValue) {
                List<String> ordering = enumOrdering(id);
                if (ordering.isEmpty()) {
                    return value;
                }
                int currentIndex = ordering.indexOf(enumValue.name());
                int minIndex = min instanceof CapabilityValue.EnumValue minEnum
                        ? ordering.indexOf(minEnum.name())
                        : -1;
                int maxIndex = max instanceof CapabilityValue.EnumValue maxEnum
                        ? ordering.indexOf(maxEnum.name())
                        : -1;
                if (currentIndex < 0) {
                    return value;
                }
                int clampedIndex = currentIndex;
                if (minIndex >= 0) {
                    clampedIndex = Math.max(clampedIndex, minIndex);
                }
                if (maxIndex >= 0) {
                    clampedIndex = Math.min(clampedIndex, maxIndex);
                }
                if (clampedIndex == currentIndex) {
                    return value;
                }
                return new CapabilityValue.EnumValue(ordering.get(clampedIndex));
            }
            if (value instanceof CapabilityValue.BoolValue boolValue) {
                boolean resolved = boolValue.value();
                if (min instanceof CapabilityValue.BoolValue minBool) {
                    resolved = resolved || minBool.value();
                }
                if (max instanceof CapabilityValue.BoolValue maxBool) {
                    resolved = resolved && maxBool.value();
                }
                return resolved == boolValue.value() ? value : new CapabilityValue.BoolValue(resolved);
            }
            return value;
        }

        private static List<String> enumOrdering(CapabilityId id) {
            return switch (id) {
                case PARTICLES -> List.of("MINIMAL", "DECREASED", "ALL");
                case CLOUDS -> List.of("OFF", "FAST", "FANCY");
                case GRAPHICS_MODE -> List.of("FAST", "FANCY", "FABULOUS");
                default -> List.of();
            };
        }
    }
}
