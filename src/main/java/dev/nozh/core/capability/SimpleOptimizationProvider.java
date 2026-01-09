package dev.nozh.core.capability;

/**
 * Simple base implementation of OptimizationProvider.
 * <p>
 * Providers can extend this class to avoid implementing boilerplate methods.
 */
public abstract class SimpleOptimizationProvider implements OptimizationProvider {

    private final String id;
    private final String name;
    private final String description;
    private final double expectedFpsImpact;

    protected SimpleOptimizationProvider(
            String id,
            String name,
            String description,
            double expectedFpsImpact
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.expectedFpsImpact = expectedFpsImpact;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public double getExpectedFpsImpact() {
        return expectedFpsImpact;
    }

    @Override
    public String getCategory() {
        return "general";
    }

    @Override
    public boolean isReversible() {
        return true;
    }

    /**
     * Default implementation: always returns true.
     * Override if you need more complex logic.
     */
    @Override
    public boolean canExecute() {
        return true;
    }
}