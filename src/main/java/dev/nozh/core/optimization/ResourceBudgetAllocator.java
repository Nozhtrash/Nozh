package dev.nozh.core.optimization;

import dev.nozh.NozhConstants;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allocates render budget across different rendering systems.
 * Ensures total GPU/CPU time stays within target frametime.
 * 
 * <p>
 * Budget is divided into categories:
 * <ul>
 * <li><b>Entities</b>: 30-50% - Entity rendering, animations</li>
 * <li><b>Terrain</b>: 20-40% - Chunk rendering, block updates</li>
 * <li><b>Particles</b>: 5-15% - Particle effects</li>
 * <li><b>Lighting</b>: 10-20% - Light calculations, shadows</li>
 * <li><b>UI</b>: 5-10% - HUD, menus, overlays</li>
 * </ul>
 * 
 * <p>
 * <b>Thread Safety:</b> Uses ConcurrentHashMap for thread-safe budget tracking
 * <p>
 * <b>Performance:</b> O(1) budget queries, minimal allocation
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class ResourceBudgetAllocator {

    /**
     * Render budget allocation for different systems.
     * 
     * @param entitiesMs  budget for entity rendering in milliseconds
     * @param terrainMs   budget for terrain rendering in milliseconds
     * @param particlesMs budget for particle effects in milliseconds
     * @param lightingMs  budget for lighting calculations in milliseconds
     * @param uiMs        budget for UI/HUD rendering in milliseconds
     * @param headroomMs  safety margin for unexpected overhead in milliseconds
     */
    public record RenderBudget(
            double entitiesMs,
            double terrainMs,
            double particlesMs,
            double lightingMs,
            double uiMs,
            double headroomMs) {
        /**
         * Gets total allocated budget.
         * 
         * @return total budget in milliseconds
         */
        public double total() {
            return entitiesMs + terrainMs + particlesMs + lightingMs + uiMs + headroomMs;
        }

        /**
         * Gets allocation percentage for a category.
         * 
         * @param category budget category
         * @return percentage (0.0 to 1.0)
         */
        public double getPercentage(BudgetCategory category) {
            double total = total();
            if (total == 0)
                return 0;

            return switch (category) {
                case ENTITIES -> entitiesMs / total;
                case TERRAIN -> terrainMs / total;
                case PARTICLES -> particlesMs / total;
                case LIGHTING -> lightingMs / total;
                case UI -> uiMs / total;
                case HEADROOM -> headroomMs / total;
            };
        }
    }

    /**
     * Budget categories for different rendering systems.
     */
    public enum BudgetCategory {
        /** Entity rendering (mobs, players, armor stands, etc.) */
        ENTITIES,

        /** Terrain rendering (chunks, blocks, block entities) */
        TERRAIN,

        /** Particle effects (smoke, flames, magic, etc.) */
        PARTICLES,

        /** Lighting calculations (dynamic lights, shadows, AO) */
        LIGHTING,

        /** UI rendering (HUD, menus, overlays, text) */
        UI,

        /** Safety margin for unexpected overhead */
        HEADROOM
    }

    // Default budget allocations (percentages)
    private static final Map<BudgetCategory, Double> DEFAULT_ALLOCATIONS = Map.of(
            BudgetCategory.ENTITIES, 0.40, // 40%
            BudgetCategory.TERRAIN, 0.30, // 30%
            BudgetCategory.PARTICLES, 0.10, // 10%
            BudgetCategory.LIGHTING, 0.10, // 10%
            BudgetCategory.UI, 0.05, // 5%
            BudgetCategory.HEADROOM, 0.05 // 5% safety margin
    );

    // Current budget allocations (percentages)
    private final Map<BudgetCategory, Double> currentAllocations;

    // Actual measured usage (milliseconds)
    private final Map<BudgetCategory, Double> actualUsage;

    // Historical usage for adaptive adjustment
    private final Map<BudgetCategory, RingBuffer> usageHistory;

    /**
     * Constructs a new ResourceBudgetAllocator with default allocations.
     */
    public ResourceBudgetAllocator() {
        this.currentAllocations = new ConcurrentHashMap<>(DEFAULT_ALLOCATIONS);
        this.actualUsage = new ConcurrentHashMap<>();
        this.usageHistory = new EnumMap<>(BudgetCategory.class);

        // Initialize history buffers
        for (BudgetCategory category : BudgetCategory.values()) {
            usageHistory.put(category, new RingBuffer(60)); // 1 second at 60fps
        }
    }

    /**
     * Calculates optimal budget distribution for target frametime.
     * 
     * <p>
     * Algorithm:
     * <ol>
     * <li>Start with default allocations</li>
     * <li>Adjust based on recent usage patterns</li>
     * <li>Ensure safety headroom</li>
     * <li>Normalize to target frametime</li>
     * </ol>
     * 
     * @param targetFrametimeMs target frametime in milliseconds (e.g., 16.67 for
     *                          60fps)
     * @return optimal budget allocation
     * @throws IllegalArgumentException if targetFrametime <= 0
     */
    public RenderBudget calculateOptimalBudget(double targetFrametimeMs) {
        if (targetFrametimeMs <= 0) {
            throw new IllegalArgumentException("Target frametime must be positive");
        }

        // Get current allocations or defaults
        Map<BudgetCategory, Double> allocations = new EnumMap<>(currentAllocations);

        // Adjust based on usage patterns
        adjustBasedOnUsage(allocations);

        // Ensure minimum headroom (at least 5% or 1ms)
        double headroom = Math.max(targetFrametimeMs * 0.05, 1.0);
        allocations.put(BudgetCategory.HEADROOM, headroom / targetFrametimeMs);

        // Normalize allocations to sum to 1.0
        normalizeAllocations(allocations);

        // Calculate absolute budgets
        return new RenderBudget(
                allocations.get(BudgetCategory.ENTITIES) * targetFrametimeMs,
                allocations.get(BudgetCategory.TERRAIN) * targetFrametimeMs,
                allocations.get(BudgetCategory.PARTICLES) * targetFrametimeMs,
                allocations.get(BudgetCategory.LIGHTING) * targetFrametimeMs,
                allocations.get(BudgetCategory.UI) * targetFrametimeMs,
                allocations.get(BudgetCategory.HEADROOM) * targetFrametimeMs);
    }

    /**
     * Adjusts budget allocation based on historical usage.
     * 
     * <p>
     * Categories that consistently exceed their budget get more allocation,
     * while underutilized categories get less.
     * 
     * @param allocations current allocations to adjust (modified in-place)
     */
    private void adjustBasedOnUsage(Map<BudgetCategory, Double> allocations) {
        for (BudgetCategory category : BudgetCategory.values()) {
            if (category == BudgetCategory.HEADROOM)
                continue;

            RingBuffer history = usageHistory.get(category);
            if (history.size() < 10)
                continue; // Need more data

            double avgUsage = history.average();
            double allocation = allocations.get(category);

            // If consistently over budget, increase allocation
            if (avgUsage > allocation * 1.1) {
                allocations.put(category, Math.min(0.6, allocation * 1.1));
            }
            // If consistently under budget, decrease allocation
            else if (avgUsage < allocation * 0.7) {
                allocations.put(category, Math.max(0.05, allocation * 0.9));
            }
        }
    }

    /**
     * Normalizes allocations to sum to exactly 1.0.
     * 
     * @param allocations allocations to normalize (modified in-place)
     */
    private void normalizeAllocations(Map<BudgetCategory, Double> allocations) {
        double sum = allocations.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum == 0)
            return;

        double factor = 1.0 / sum;
        allocations.replaceAll((k, v) -> v * factor);
    }

    /**
     * Reallocates budget from one category to another.
     * 
     * <p>
     * Example: Steal 2ms from TERRAIN and give it to ENTITIES
     * 
     * <pre>{@code
     * allocator.reallocateBudget(BudgetCategory.TERRAIN, BudgetCategory.ENTITIES, 2.0);
     * }</pre>
     * 
     * @param from    category to take budget from
     * @param to      category to give budget to
     * @param deltaMs amount to transfer in milliseconds
     */
    public void reallocateBudget(BudgetCategory from, BudgetCategory to, double deltaMs) {
        if (from == to || from == BudgetCategory.HEADROOM || deltaMs <= 0) {
            return;
        }

        // Convert delta to percentage of total budget
        // This is approximate since we don't know total frametime here
        double fromAlloc = currentAllocations.get(from);
        double toAlloc = currentAllocations.get(to);

        // Transfer 10% relative allocation
        double transfer = 0.05;
        currentAllocations.put(from, Math.max(0.05, fromAlloc - transfer));
        currentAllocations.put(to, Math.min(0.60, toAlloc + transfer));

        NozhConstants.LOGGER.debug("Reallocated budget: {} -> {}, delta={}ms", from, to, deltaMs);
    }

    /**
     * Records actual usage for a category.
     * 
     * <p>
     * Call this each frame to track actual render times.
     * Used for adaptive budget adjustment.
     * 
     * @param category budget category
     * @param usageMs  actual time used in milliseconds
     */
    public void recordUsage(BudgetCategory category, double usageMs) {
        actualUsage.put(category, usageMs);
        usageHistory.get(category).add(usageMs);
    }

    /**
     * Gets current budget allocations as percentages.
     * 
     * @return map of category to percentage (0.0 to 1.0)
     */
    public Map<BudgetCategory, Double> getCurrentAllocation() {
        return Map.copyOf(currentAllocations);
    }

    /**
     * Gets actual usage for a category.
     * 
     * @param category budget category
     * @return last recorded usage in milliseconds, or 0 if none recorded
     */
    public double getActualUsage(BudgetCategory category) {
        return actualUsage.getOrDefault(category, 0.0);
    }

    /**
     * Gets average usage over recent frames.
     * 
     * @param category budget category
     * @return average usage in milliseconds
     */
    public double getAverageUsage(BudgetCategory category) {
        RingBuffer history = usageHistory.get(category);
        return history != null ? history.average() : 0.0;
    }

    /**
     * Checks if a category is over budget.
     * 
     * @param category budget category
     * @param budget   current budget
     * @return true if actual usage exceeds budget
     */
    public boolean isOverBudget(BudgetCategory category, RenderBudget budget) {
        double actual = getActualUsage(category);
        double allocated = switch (category) {
            case ENTITIES -> budget.entitiesMs();
            case TERRAIN -> budget.terrainMs();
            case PARTICLES -> budget.particlesMs();
            case LIGHTING -> budget.lightingMs();
            case UI -> budget.uiMs();
            case HEADROOM -> budget.headroomMs();
        };

        return actual > allocated;
    }

    /**
     * Resets all allocations to defaults.
     */
    public void resetToDefaults() {
        currentAllocations.clear();
        currentAllocations.putAll(DEFAULT_ALLOCATIONS);
        actualUsage.clear();
        usageHistory.values().forEach(RingBuffer::clear);

        NozhConstants.LOGGER.info("Budget allocations reset to defaults");
    }

    /**
     * Simple ring buffer for tracking recent values.
     */
    private static class RingBuffer {
        private final double[] buffer;
        private int index;
        private int count;

        RingBuffer(int capacity) {
            this.buffer = new double[capacity];
            this.index = 0;
            this.count = 0;
        }

        void add(double value) {
            buffer[index] = value;
            index = (index + 1) % buffer.length;
            count = Math.min(count + 1, buffer.length);
        }

        double average() {
            if (count == 0)
                return 0;
            double sum = 0;
            for (int i = 0; i < count; i++) {
                sum += buffer[i];
            }
            return sum / count;
        }

        int size() {
            return count;
        }

        void clear() {
            index = 0;
            count = 0;
        }
    }
}
