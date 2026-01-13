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
 * <b>New Features (Ultimate):</b>
 * <ul>
 * <li><b>Predictive Budgeting</b> - Adjusts allocations based on predicted
 * frame cost</li>
 * <li><b>Memory Awareness</b> - Reduces budgets when RAM is constrained to
 * prevent GC thrashing</li>
 * </ul>
 * 
 * @since 0.3.1
 * @author NOZH Team
 */
public final class ResourceBudgetAllocator {

    public record RenderBudget(
            double entitiesMs,
            double terrainMs,
            double particlesMs,
            double lightingMs,
            double uiMs,
            double headroomMs) {

        public double total() {
            return entitiesMs + terrainMs + particlesMs + lightingMs + uiMs + headroomMs;
        }

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

    public enum BudgetCategory {
        ENTITIES,
        TERRAIN,
        PARTICLES,
        LIGHTING,
        UI,
        HEADROOM
    }

    // Default budget allocations (percentages)
    private static final Map<BudgetCategory, Double> DEFAULT_ALLOCATIONS = Map.of(
            BudgetCategory.ENTITIES, 0.40,
            BudgetCategory.TERRAIN, 0.30,
            BudgetCategory.PARTICLES, 0.10,
            BudgetCategory.LIGHTING, 0.10,
            BudgetCategory.UI, 0.05,
            BudgetCategory.HEADROOM, 0.05);

    // Current budget allocations (percentages)
    private final Map<BudgetCategory, Double> currentAllocations;

    // Actual measured usage (milliseconds)
    private final Map<BudgetCategory, Double> actualUsage;

    // Historical usage for adaptive adjustment
    private final Map<BudgetCategory, RingBuffer> usageHistory;

    // Memory pressure state (0.0 to 1.0)
    private double memoryPressure = 0.0;

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
     * Includes memory pressure adjustments.
     * 
     * @param targetFrametimeMs target frametime in milliseconds
     * @return optimal budget allocation
     */
    public RenderBudget calculateOptimalBudget(double targetFrametimeMs) {
        if (targetFrametimeMs <= 0) {
            throw new IllegalArgumentException("Target frametime must be positive");
        }

        // 1. Get current allocations
        Map<BudgetCategory, Double> allocations = new EnumMap<>(currentAllocations);

        // 2. Adjust based on usage patterns
        adjustBasedOnUsage(allocations);

        // 3. Apply Memory Pressure modifiers
        // If memory is tight, we reduce budgets for high-allocation systems (Particles,
        // Terrain)
        // and increase Headroom to allow for GC pauses.
        if (memoryPressure > 0.7) {
            applyMemoryConstraints(allocations);
        }

        // 4. Ensure safety headroom
        // Base headroom 5% + up to 10% extra if memory is tight
        double baseHeadroomPct = 0.05 + (memoryPressure > 0.8 ? 0.10 : 0.0);
        double headroom = Math.max(targetFrametimeMs * baseHeadroomPct, 1.0);
        allocations.put(BudgetCategory.HEADROOM, headroom / targetFrametimeMs);

        // 5. Normalize allocations to sum to 1.0
        normalizeAllocations(allocations);

        // 6. Calculate absolute budgets
        return new RenderBudget(
                allocations.get(BudgetCategory.ENTITIES) * targetFrametimeMs,
                allocations.get(BudgetCategory.TERRAIN) * targetFrametimeMs,
                allocations.get(BudgetCategory.PARTICLES) * targetFrametimeMs,
                allocations.get(BudgetCategory.LIGHTING) * targetFrametimeMs,
                allocations.get(BudgetCategory.UI) * targetFrametimeMs,
                allocations.get(BudgetCategory.HEADROOM) * targetFrametimeMs);
    }

    public void setMemoryPressure(double pressure) {
        this.memoryPressure = Math.max(0.0, Math.min(1.0, pressure));
    }

    private void applyMemoryConstraints(Map<BudgetCategory, Double> allocations) {
        // Particles and Terrain are usually the biggest allocators
        // Reduce them to discourage generation of new objects

        double particleReduction = 0.5; // Cut particle budget in half
        double terrainReduction = 0.8; // Reduce terrain updates by 20%

        allocations.computeIfPresent(BudgetCategory.PARTICLES, (k, v) -> v * particleReduction);
        allocations.computeIfPresent(BudgetCategory.TERRAIN, (k, v) -> v * terrainReduction);

        // Note: Normalization step later will redistribute the "saved" percentage
        // essentially giving it to Headroom or other systems
    }

    private void adjustBasedOnUsage(Map<BudgetCategory, Double> allocations) {
        for (BudgetCategory category : BudgetCategory.values()) {
            if (category == BudgetCategory.HEADROOM)
                continue;

            RingBuffer history = usageHistory.get(category);
            if (history.size() < 10)
                continue;

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

    private void normalizeAllocations(Map<BudgetCategory, Double> allocations) {
        double sum = allocations.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum == 0)
            return;

        double factor = 1.0 / sum;
        allocations.replaceAll((k, v) -> v * factor);
    }

    public void reallocateBudget(BudgetCategory from, BudgetCategory to, double deltaMs) {
        if (from == to || from == BudgetCategory.HEADROOM || deltaMs <= 0) {
            return;
        }

        double fromAlloc = currentAllocations.get(from);
        double toAlloc = currentAllocations.get(to);

        double transfer = 0.05; // 5% transfer
        currentAllocations.put(from, Math.max(0.05, fromAlloc - transfer));
        currentAllocations.put(to, Math.min(0.60, toAlloc + transfer));

        NozhConstants.LOGGER.debug("Reallocated budget: {} -> {}, delta={}ms", from, to, deltaMs);
    }

    public void recordUsage(BudgetCategory category, double usageMs) {
        actualUsage.put(category, usageMs);
        usageHistory.get(category).add(usageMs);
    }

    public Map<BudgetCategory, Double> getCurrentAllocation() {
        return Map.copyOf(currentAllocations);
    }

    public double getActualUsage(BudgetCategory category) {
        return actualUsage.getOrDefault(category, 0.0);
    }

    public double getAverageUsage(BudgetCategory category) {
        RingBuffer history = usageHistory.get(category);
        return history != null ? history.average() : 0.0;
    }

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

    public void resetToDefaults() {
        currentAllocations.clear();
        currentAllocations.putAll(DEFAULT_ALLOCATIONS);
        actualUsage.clear();
        usageHistory.values().forEach(RingBuffer::clear);
        memoryPressure = 0.0;

        NozhConstants.LOGGER.info("Budget allocations reset to defaults");
    }

    private static class RingBuffer {
        private final double[] buffer;
        private int index;
        private int count;

        RingBuffer(int capacity) {
            this.buffer = new double[capacity];
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
