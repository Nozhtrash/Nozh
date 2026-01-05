package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.governor.OptimizationProfile;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ActionMatrixRules {

    private final Map<Scenario, Map<OptimizationProfile, ScenarioRuleSet>> rules;

    private ActionMatrixRules(Map<Scenario, Map<OptimizationProfile, ScenarioRuleSet>> rules) {
        this.rules = rules;
    }

    public static ActionMatrixRules defaultRules() {
        return builder()
                .registerScenario(Scenario.COMBAT, combatRules())
                .registerScenario(Scenario.AFK, afkRules())
                .registerScenario(Scenario.MINING, miningRules())
                .registerScenario(Scenario.BUILDING, buildingRules())
                .registerScenario(Scenario.MENU, menuRules())
                .registerScenario(Scenario.LOADING, loadingRules())
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Scenario, Map<OptimizationProfile, ScenarioRuleSet>> rules =
                new EnumMap<>(Scenario.class);

        public Builder registerScenario(Scenario scenario, Map<OptimizationProfile, ScenarioRuleSet> perProfile) {
            rules.put(scenario, Collections.unmodifiableMap(new EnumMap<>(perProfile)));
            return this;
        }

        public ActionMatrixRules build() {
            return new ActionMatrixRules(Collections.unmodifiableMap(rules));
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

    private static Map<OptimizationProfile, ScenarioRuleSet> perProfile(
            Map<CapabilityId, CapabilityValue> aggressive,
            Map<CapabilityId, CapabilityValue> balanced) {
        Map<OptimizationProfile, ScenarioRuleSet> perProfile = new EnumMap<>(OptimizationProfile.class);
        perProfile.put(OptimizationProfile.AGGRESSIVE, new ScenarioRuleSet(aggressive));
        perProfile.put(OptimizationProfile.BALANCED, new ScenarioRuleSet(balanced));
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
}
