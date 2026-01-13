package dev.nozh.core.util;

/**
 * High-performance math approximations for "God Mode" optimization.
 * Used in hot paths where precision can be traded for speed.
 * 
 * @since 0.3.5
 * @author NOZH Team
 */
public final class MathOptimizer {

    private MathOptimizer() {
    }

    /**
     * Fast Inverse Square Root (Quake III Algorithm).
     * Useful for vector normalization in physics/rendering.
     */
    public static float fastInvSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1); // Evil floating point bit level hacking
        x = Float.intBitsToFloat(i);
        x = x * (1.5f - xhalf * x * x); // 1st Newton iteration
        return x;
    }

    /**
     * Linear interpolation.
     */
    public static double lerp(double start, double end, double t) {
        return start + t * (end - start);
    }

    public static float lerp(float start, float end, float t) {
        return start + t * (end - start);
    }

    /**
     * Clamps a value between min and max.
     */
    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Fast absolute value (avoids checking sign bit technically, but Math.abs is
     * intrinsic).
     * This is just for consistency.
     */
    public static float abs(float a) {
        return (a <= 0.0F) ? 0.0F - a : a;
    }
}
