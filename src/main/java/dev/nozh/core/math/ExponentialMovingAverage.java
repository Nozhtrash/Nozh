package dev.nozh.core.math;

/**
 * Exponential Moving Average (EMA) calculator.
 * 
 * Zero-allocation after warmup. Thread-safe for reads.
 * 
 * Formula: EMA_t = α * value_t + (1 - α) * EMA_{t-1}
 * 
 * @author Nozh Team
 * @since 0.3.0
 */
public final class ExponentialMovingAverage {

    private final double alpha;
    private volatile double ema;
    private volatile int sampleCount;
    private volatile boolean initialized;

    /**
     * Creates an EMA calculator with the specified smoothing factor.
     * 
     * @param alpha Smoothing factor in range (0, 1]. Higher = more weight on recent values.
     *              Typical values: 0.1 (slow), 0.3 (medium), 0.5 (fast)
     * @throws IllegalArgumentException if alpha is not in valid range
     */
    public ExponentialMovingAverage(double alpha) {
        if (alpha <= 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("Alpha must be in range (0, 1], got: " + alpha);
        }
        this.alpha = alpha;
        this.ema = 0.0;
        this.sampleCount = 0;
        this.initialized = false;
    }

    /**
     * Creates an EMA with default alpha of 0.3 (medium responsiveness).
     */
    public ExponentialMovingAverage() {
        this(0.3);
    }

    /**
     * Adds a new sample and updates the EMA.
     * 
     * @param value New sample value
     * @return Updated EMA value
     */
    public double addSample(double value) {
        if (!Double.isFinite(value)) {
            return ema; // Ignore invalid values
        }

        if (!initialized) {
            ema = value;
            initialized = true;
        } else {
            ema = alpha * value + (1.0 - alpha) * ema;
        }
        
        sampleCount++;
        return ema;
    }

    /**
     * Gets the current EMA value.
     * 
     * @return Current EMA, or 0.0 if no samples added
     */
    public double getValue() {
        return ema;
    }

    /**
     * Checks if EMA has been initialized with at least one sample.
     * 
     * @return true if at least one sample has been added
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Gets the total number of samples added.
     * 
     * @return Sample count
     */
    public int getSampleCount() {
        return sampleCount;
    }

    /**
     * Gets the alpha (smoothing factor).
     * 
     * @return Alpha value
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * Resets the EMA to uninitialized state.
     */
    public void reset() {
        ema = 0.0;
        sampleCount = 0;
        initialized = false;
    }

    /**
     * Creates an EMA with recommended alpha based on desired half-life.
     * 
     * @param halfLifeSamples Number of samples for value to decay to 50%
     * @return New EMA calculator
     */
    public static ExponentialMovingAverage withHalfLife(int halfLifeSamples) {
        if (halfLifeSamples <= 0) {
            throw new IllegalArgumentException("Half-life must be positive: " + halfLifeSamples);
        }
        // α = 1 - exp(-ln(2) / halfLife)
        double alpha = 1.0 - Math.exp(-Math.log(2) / halfLifeSamples);
        return new ExponentialMovingAverage(alpha);
    }
}
