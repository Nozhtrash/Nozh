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
    /**
     * Fast absolute value (avoids checking sign bit technically, but Math.abs is
     * intrinsic).
     * This is just for consistency.
     */
    public static float abs(float a) {
        return (a <= 0.0F) ? 0.0F - a : a;
    }

    // === Fast Trigonometry (LUT) ===
    private static final float[] SIN_TABLE = new float[65536];
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = PI * 2.0F;
    private static final float HALF_PI = PI / 2.0F;
    private static final float RAD_TO_INDEX = 65536.0F / TWO_PI;

    static {
        for (int i = 0; i < 65536; ++i) {
            SIN_TABLE[i] = (float) Math.sin(i * TWO_PI / 65536.0D);
        }
    }

    /**
     * Fast sine approximation using a 65536-entry lookup table.
     * Precision error ~0.0001, but 10x-50x faster than Math.sin.
     */
    public static float fastSin(float radians) {
        return SIN_TABLE[(int) (radians * RAD_TO_INDEX) & 65535];
    }

    /**
     * Fast cosine approximation using the sine lookup table.
     */
    public static float fastCos(float radians) {
        return SIN_TABLE[(int) ((radians + HALF_PI) * RAD_TO_INDEX) & 65535];
    }
}
