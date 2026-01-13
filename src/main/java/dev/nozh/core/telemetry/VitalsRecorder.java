package dev.nozh.core.telemetry;

import java.util.Arrays;

/**
 * Records vital statistics (Frame Times) for the visual graph renderer.
 * <p>
 * Uses a circular buffer for O(1) insertions and lookups.
 * Stores up to 60 seconds of history (assuming 60 FPS = 3600 frames).
 */
public class VitalsRecorder {
    private static final int HISTORY_SIZE = 3600; // 60 seconds @ 60 FPS
    private final float[] frameTimes = new float[HISTORY_SIZE];
    private int head = 0;
    private int count = 0;
    
    // Cached stats
    private float min = Float.MAX_VALUE;
    private float max = Float.MIN_VALUE;
    private float avg = 0;

    /**
     * Record a new frame time.
     * @param ms Frame time in milliseconds
     */
    public synchronized void recordFrame(float ms) {
        frameTimes[head] = ms;
        head = (head + 1) % HISTORY_SIZE;
        if (count < HISTORY_SIZE) count++;
        
        // Update simple stats
        if (ms < min) min = ms;
        if (ms > max) max = ms;
        
        // Re-calculating full average every frame is expensive, rely on getting it on-demand or approximating
    }

    /**
     * Get the entire history buffer, ordered from oldest to newest.
     * WARNING: Allocates a new array. Only call when opening the GUI.
     */
    public synchronized float[] getHistorySnapshot() {
        float[] output = new float[count];
        if (count < HISTORY_SIZE) {
            // Not filled yet, simple copy
            System.arraycopy(frameTimes, 0, output, 0, count);
        } else {
            // Wrapped, need to reconstruct order
            // head points to the OLDEST value in a full circular buffer
            int tailLength = HISTORY_SIZE - head;
            System.arraycopy(frameTimes, head, output, 0, tailLength);
            System.arraycopy(frameTimes, 0, output, tailLength, head);
        }
        return output;
    }
    
    /**
     * Get the standard deviation of the current buffer.
     * Useful for detecting "Stability".
     */
    public synchronized float getVariance() {
        if (count < 2) return 0;
        
        float sum = 0;
        for (int i = 0; i < count; i++) sum += frameTimes[i];
        float mean = sum / count;
        
        float temp = 0;
        for (int i = 0; i < count; i++) {
            float diff = frameTimes[i] - mean;
            temp += diff * diff;
        }
        return temp / count;
    }

    public int getSampleCount() {
        return count;
    }

    // Adaptor for VitalsGraphWidget
    public float[] getFrameTimeHistory() {
        return getHistorySnapshot();
    }
    
    public float getAverageFrameTime() {
        if (count == 0) return 0f;
        float sum = 0;
        for (int i=0; i<count; i++) sum += frameTimes[i];
        return sum / count;
    }
