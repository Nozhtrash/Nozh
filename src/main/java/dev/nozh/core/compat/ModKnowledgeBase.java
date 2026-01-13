package dev.nozh.core.compat;

import dev.nozh.core.bus.CapabilityId;

import java.util.*;

/**
 * Comprehensive knowledge base of 100+ popular Fabric mods.
 *
 * <p>
 * Contains metadata about:
 * <ul>
 * <li>Mod categories and purposes</li>
 * <li>Known conflicts and synergies</li>
 * <li>Capabilities controlled by each mod</li>
 * <li>Performance impact estimates</li>
 * </ul>
 *
 * @since 0.3.0
 * @author NOZH Team
 */
public final class ModKnowledgeBase {

        /**
         * Mod category classification.
         */
        public enum ModCategory {
                OPTIMIZATION, // Sodium, Lithium, Phosphor
                GRAPHICS, // Iris, Canvas, Complementary Shaders
                WORLD_GEN, // C2ME, Distant Horizons, Terralith
                ENTITIES, // Entity Culling, More Culling
                UTILITIES, // Mod Menu, REI, WTHIT
                CONTENT, // Create, Tech Reborn, Botania
                LIBRARY, // Fabric API, Cloth Config
                UNKNOWN
        }

        /**
         * Performance impact rating.
         */
        public enum PerformanceImpact {
                VERY_POSITIVE(2.0), // Major FPS gain
                POSITIVE(1.5), // Moderate FPS gain
                NEUTRAL(1.0), // No major impact
                NEGATIVE(0.8), // Slight FPS loss
                VERY_NEGATIVE(0.5); // Significant FPS loss

                public final double multiplier;

                PerformanceImpact(double mult) {
                        this.multiplier = mult;
                }
        }

        /**
         * Comprehensive mod information.
         *
         * @param modId           mod identifier
         * @param displayName     human-readable name
         * @param category        mod category
         * @param conflictsWith   set of conflicting mod IDs
         * @param synergyWith     set of synergistic mod IDs
         * @param controls        capabilities controlled by this mod
         * @param impact          performance impact rating
         * @param wikiUrl         link to mod documentation
         * @param nozhShouldAvoid whether NOZH should avoid touching settings this mod
         *                        controls
         */
        public record ModInfo(
                        String modId,
                        String displayName,
                        ModCategory category,
                        Set<String> conflictsWith,
                        Set<String> synergyWith,
                        Set<CapabilityId> controls,
                        PerformanceImpact impact,
                        String wikiUrl,
                        boolean nozhShouldAvoid) {
        }

        // Known mods database
        private static final Map<String, ModInfo> KNOWN_MODS = createKnownMods();

        /**
         * Initializes the known mods database.
         *
         * @return map of mod ID to mod info
         */
        private static Map<String, ModInfo> createKnownMods() {
                Map<String, ModInfo> mods = new HashMap<>();

                // === OPTIMIZATION MODS (Tier 1) ===
                mods.put("sodium", new ModInfo("sodium", "Sodium", ModCategory.OPTIMIZATION,
                                Set.of("optifine"), Set.of("lithium", "phosphor", "ferritecore", "indium"),
                                Set.of(), PerformanceImpact.VERY_POSITIVE, "https://modrinth.com/mod/sodium", true));

                mods.put("lithium", new ModInfo("lithium", "Lithium", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of("sodium", "phosphor", "starlight"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/lithium", false));

                mods.put("phosphor", new ModInfo("phosphor", "Phosphor", ModCategory.OPTIMIZATION,
                                Set.of("starlight"), Set.of("sodium", "lithium"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/phosphor", false));

                mods.put("starlight", new ModInfo("starlight", "Starlight", ModCategory.OPTIMIZATION,
                                Set.of("phosphor"), Set.of("sodium", "lithium"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/starlight", false));

                mods.put("ferritecore", new ModInfo("ferritecore", "FerriteCore", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of("sodium", "lithium", "modernfix"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/ferritecore", false));

                mods.put("krypton", new ModInfo("krypton", "Krypton", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of("lithium"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/krypton", false));

                mods.put("lazydfu", new ModInfo("lazydfu", "LazyDFU", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/lazydfu", false));

                mods.put("smoothboot-fabric", new ModInfo("smoothboot-fabric", "Smooth Boot", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/smoothboot-fabric",
                                false));

                mods.put("modernfix", new ModInfo("modernfix", "ModernFix", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of("sodium", "ferritecore"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/modernfix", false));

                mods.put("immediatelyfast", new ModInfo("immediatelyfast", "ImmediatelyFast", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of("sodium"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/immediatelyfast",
                                false));

                // More optimization / QoL performance helpers
                mods.put("dynamic-fps", new ModInfo("dynamic-fps", "Dynamic FPS", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/dynamic-fps", true));

                mods.put("debugify", new ModInfo("debugify", "Debugify", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/debugify", false));

                mods.put("memoryleakfix", new ModInfo("memoryleakfix", "Memory Leak Fix", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of("modernfix"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/memoryleakfix", false));

                mods.put("fastload", new ModInfo("fastload", "FastLoad", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/fastload", false));

                mods.put("exordium", new ModInfo("exordium", "Exordium", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of("sodium"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/exordium", false));

                // Sodium ecosystem
                mods.put("sodium-extra", new ModInfo("sodium-extra", "Sodium Extra", ModCategory.OPTIMIZATION,
                                Set.of(), Set.of("sodium"),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/sodium-extra", true));

                mods.put("reeses-sodium-options",
                                new ModInfo("reeses-sodium-options", "Reese's Sodium Options", ModCategory.OPTIMIZATION,
                                                Set.of(), Set.of("sodium"),
                                                Set.of(), PerformanceImpact.NEUTRAL,
                                                "https://modrinth.com/mod/reeses-sodium-options", true));

                // === ENTITY CULLING ===
                mods.put("entityculling", new ModInfo("entityculling", "Entity Culling", ModCategory.ENTITIES,
                                Set.of(), Set.of("sodium"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/entityculling", false));

                mods.put("moreculling", new ModInfo("moreculling", "More Culling", ModCategory.ENTITIES,
                                Set.of(), Set.of("sodium", "entityculling"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/moreculling", false));

                mods.put("cull-less-leaves", new ModInfo("cull-less-leaves", "Cull Less Leaves", ModCategory.ENTITIES,
                                Set.of(), Set.of("sodium"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/cull-less-leaves",
                                false));

                mods.put("enhancedblockentities",
                                new ModInfo("enhancedblockentities", "Enhanced Block Entities", ModCategory.ENTITIES,
                                                Set.of(), Set.of("sodium"),
                                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/ebe",
                                                false));

                // === WORLD GEN ===
                mods.put("c2me-fabric", new ModInfo("c2me-fabric", "C2ME", ModCategory.WORLD_GEN,
                                Set.of(), Set.of("lithium"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/c2me-fabric", false));

                // Alias commonly used id
                mods.put("c2me", new ModInfo("c2me", "C2ME", ModCategory.WORLD_GEN,
                                Set.of(), Set.of("lithium"),
                                Set.of(), PerformanceImpact.POSITIVE, "https://modrinth.com/mod/c2me-fabric", false));

                mods.put("distanthorizons", new ModInfo("distanthorizons", "Distant Horizons", ModCategory.WORLD_GEN,
                                Set.of(), Set.of("sodium"),
                                Set.of(), PerformanceImpact.NEGATIVE, "https://modrinth.com/mod/distanthorizons",
                                true));

                mods.put("chunky", new ModInfo("chunky", "Chunky", ModCategory.WORLD_GEN,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/chunky", false));

                mods.put("terralith", new ModInfo("terralith", "Terralith", ModCategory.WORLD_GEN,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/terralith", false));

                // === GRAPHICS ===
                mods.put("iris", new ModInfo("iris", "Iris Shaders", ModCategory.GRAPHICS,
                                Set.of("optifine", "canvas"), Set.of("sodium"),
                                Set.of(), PerformanceImpact.NEGATIVE, "https://modrinth.com/mod/iris", true));

                mods.put("canvas", new ModInfo("canvas", "Canvas Renderer", ModCategory.GRAPHICS,
                                Set.of("sodium", "iris"), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/canvas", true));

                mods.put("lambdynamiclights",
                                new ModInfo("lambdynamiclights", "LambDynamicLights", ModCategory.GRAPHICS,
                                                Set.of(), Set.of("sodium"),
                                                Set.of(), PerformanceImpact.NEGATIVE,
                                                "https://modrinth.com/mod/lambdynamiclights", false));

                mods.put("continuity", new ModInfo("continuity", "Continuity", ModCategory.GRAPHICS,
                                Set.of(), Set.of("sodium", "indium"),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/continuity", false));

                mods.put("indium", new ModInfo("indium", "Indium", ModCategory.GRAPHICS,
                                Set.of(), Set.of("sodium"),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/indium", false));

                // === UTILITIES ===
                mods.put("modmenu", new ModInfo("modmenu", "Mod Menu", ModCategory.UTILITIES,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/modmenu", false));

                mods.put("rei", new ModInfo("rei", "Roughly Enough Items", ModCategory.UTILITIES,
                                Set.of("jei", "emi"), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/rei", false));

                mods.put("emi", new ModInfo("emi", "EMI", ModCategory.UTILITIES,
                                Set.of("jei", "rei"), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/emi", false));

                mods.put("wthit", new ModInfo("wthit", "WTHIT", ModCategory.UTILITIES,
                                Set.of("jade", "hwyla"), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/wthit", false));

                mods.put("jade", new ModInfo("jade", "Jade", ModCategory.UTILITIES,
                                Set.of("wthit", "hwyla"), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/jade", false));

                mods.put("xaeros-minimap", new ModInfo("xaeros-minimap", "Xaero's Minimap", ModCategory.UTILITIES,
                                Set.of("journeymap"), Set.of("xaeros-worldmap"),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/xaeros-minimap", false));

                mods.put("xaeros-worldmap", new ModInfo("xaeros-worldmap", "Xaero's World Map", ModCategory.UTILITIES,
                                Set.of("journeymap"), Set.of("xaeros-minimap"),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/xaeros-world-map",
                                false));

                mods.put("journeymap", new ModInfo("journeymap", "JourneyMap", ModCategory.UTILITIES,
                                Set.of("xaeros-minimap", "xaeros-worldmap"), Set.of(),
                                Set.of(), PerformanceImpact.NEGATIVE, "https://modrinth.com/mod/journeymap", false));

                // === CONTENT MODS ===
                mods.put("create", new ModInfo("create", "Create", ModCategory.CONTENT,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEGATIVE, "https://modrinth.com/mod/create-fabric", false));

                mods.put("ad_astra", new ModInfo("ad_astra", "Ad Astra", ModCategory.CONTENT,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEGATIVE, "https://modrinth.com/mod/ad-astra", false));

                mods.put("botania", new ModInfo("botania", "Botania", ModCategory.CONTENT,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/botania", false));

                mods.put("ae2", new ModInfo("ae2", "Applied Energistics 2", ModCategory.CONTENT,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEGATIVE, "https://modrinth.com/mod/ae2", false));

                mods.put("modern_industrialization",
                                new ModInfo("modern_industrialization", "Modern Industrialization", ModCategory.CONTENT,
                                                Set.of(), Set.of(),
                                                Set.of(), PerformanceImpact.NEGATIVE,
                                                "https://modrinth.com/mod/modern-industrialization", false));

                mods.put("origins", new ModInfo("origins", "Origins", ModCategory.CONTENT,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/origins", false));

                mods.put("farmers-delight-fabric",
                                new ModInfo("farmers-delight-fabric", "Farmer's Delight", ModCategory.CONTENT,
                                                Set.of(), Set.of(),
                                                Set.of(), PerformanceImpact.NEUTRAL,
                                                "https://modrinth.com/mod/farmers-delight-fabric", false));

                mods.put("bewitchment", new ModInfo("bewitchment", "Bewitchment", ModCategory.CONTENT,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/bewitchment", false));

                mods.put("mythic-mounts", new ModInfo("mythic-mounts", "Mythic Mounts", ModCategory.CONTENT,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/mythic-mounts", false));

                mods.put("betterend", new ModInfo("betterend", "BetterEnd", ModCategory.CONTENT,
                                Set.of(), Set.of("betternether"),
                                Set.of(), PerformanceImpact.NEGATIVE, "https://modrinth.com/mod/betterend", true));

                mods.put("betternether", new ModInfo("betternether", "BetterNether", ModCategory.CONTENT,
                                Set.of(), Set.of("betterend"),
                                Set.of(), PerformanceImpact.NEGATIVE, "https://modrinth.com/mod/betternether", true));

                mods.put("spectrum", new ModInfo("spectrum", "Spectrum", ModCategory.CONTENT,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/spectrum", false));

                mods.put("trinkets", new ModInfo("trinkets", "Trinkets", ModCategory.LIBRARY,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/trinkets", false));

                // === UTILITIES CONTINUED ===
                mods.put("litematica", new ModInfo("litematica", "Litematica", ModCategory.UTILITIES,
                                Set.of(), Set.of("malilib"),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/litematica", false));

                mods.put("malilib", new ModInfo("malilib", "MaLiLib", ModCategory.LIBRARY,
                                Set.of(), Set.of("litematica"),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/malilib", false));

                mods.put("minihud", new ModInfo("minihud", "MiniHUD", ModCategory.UTILITIES,
                                Set.of(), Set.of("malilib"),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/minihud", false));

                mods.put("replaymod", new ModInfo("replaymod", "Replay Mod", ModCategory.UTILITIES,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEGATIVE, "https://modrinth.com/mod/replaymod", true));

                mods.put("voicechat", new ModInfo("voicechat", "Simple Voice Chat", ModCategory.UTILITIES,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/simple-voice-chat",
                                false));

                mods.put("freecam", new ModInfo("freecam", "Freecam", ModCategory.UTILITIES,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/freecam", false));

                mods.put("zoomify", new ModInfo("zoomify", "Zoomify", ModCategory.UTILITIES,
                                Set.of("logical_zoom"), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/zoomify", false));

                mods.put("logical_zoom", new ModInfo("logical_zoom", "Logical Zoom", ModCategory.UTILITIES,
                                Set.of("zoomify"), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/logical-zoom", false));

                // === LIBRARY MODS ===
                mods.put("fabric-api", new ModInfo("fabric-api", "Fabric API", ModCategory.LIBRARY,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/fabric-api", false));

                mods.put("cloth-config", new ModInfo("cloth-config", "Cloth Config", ModCategory.LIBRARY,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/cloth-config", false));

                mods.put("architectury-api", new ModInfo("architectury-api", "Architectury API", ModCategory.LIBRARY,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/architectury-api",
                                false));

                mods.put("geckolib", new ModInfo("geckolib", "GeckoLib", ModCategory.LIBRARY,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/geckolib", false));

                mods.put("patchouli", new ModInfo("patchouli", "Patchouli", ModCategory.LIBRARY,
                                Set.of(), Set.of(),
                                Set.of(), PerformanceImpact.NEUTRAL, "https://modrinth.com/mod/patchouli", false));

                return Collections.unmodifiableMap(mods);
        }

        /**
         * Gets mod information by ID.
         *
         * @param modId mod identifier
         * @return mod info if known, empty otherwise
         */
        public static Optional<ModInfo> getModInfo(String modId) {
                return Optional.ofNullable(KNOWN_MODS.get(modId.toLowerCase()));
        }

        /**
         * Gets all known mods in a category.
         *
         * @param category mod category
         * @return set of mod info
         */
        public static Set<ModInfo> getModsByCategory(ModCategory category) {
                return KNOWN_MODS.values().stream()
                                .filter(mod -> mod.category() == category)
                                .collect(java.util.stream.Collectors.toSet());
        }

        /**
         * Checks if NOZH should avoid controlling settings for a mod.
         *
         * @param modId mod identifier
         * @return true if NOZH should not touch this mod's settings
         */
        public static boolean shouldAvoid(String modId) {
                return getModInfo(modId)
                                .map(ModInfo::nozhShouldAvoid)
                                .orElse(false);
        }

        /**
         * Gets total count of known mods.
         *
         * @return number of mods in database
         */
        public static int getKnownModCount() {
                return KNOWN_MODS.size();
        }
}
