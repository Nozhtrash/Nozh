package dev.nozh.core.prediction;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PerformancePredictor.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>Division by zero protection (P0 fix)</li>
 *   <li>Invalid input rejection (NaN, Infinity)</li>
 *   <li>FPS drop prediction accuracy</li>
 *   <li>Performance spike detection</li>
 *   <li>Constructor validation</li>
 *   <li>Array optimization (P1 #6)</li>
 *   <li>Confidence scoring</li>
 *   <li>Reset functionality</li>
 * </ul>
 * 
 * @author Nozh Team
 * @since 0.3.0
 */
class PerformancePredictorTest {
    
    @Test
    @DisplayName("Should handle division by zero gracefully")
    void testDivisionByZeroProtection() {
        PerformancePredictor predictor = new PerformancePredictor(60);
        
        // Add constant values (zero variance)
        for (int i = 0; i < 30; i++) {
            predictor.addSample(16.67);
        }
        
        assertFalse(predictor.predictFpsDrop());
        assertTrue(predictor.isWarmedUp());
        assertEquals(30, predictor.getSampleCount());
    }
    
    @Test
    @DisplayName("Should reject NaN and Infinity inputs")
    void testInvalidInputRejection() {
        PerformancePredictor predictor = new PerformancePredictor(60);
        
        predictor.addSample(Double.NaN);
        predictor.addSample(Double.POSITIVE_INFINITY);
        predictor.addSample(Double.NEGATIVE_INFINITY);
        predictor.addSample(-5.0);
        predictor.addSample(15000.0); // > 10s
        
        assertEquals(0, predictor.getSampleCount());
    }
    
    @Test
    @DisplayName("Should detect FPS drop trend")
    void testTrendDetection() {
        PerformancePredictor predictor = new PerformancePredictor(60);
        
        // Add increasing frametimes (worsening performance)
        for (int i = 0; i < 30; i++) {
            predictor.addSample(16.0 + i * 0.5); // 16ms → 30.5ms
        }
        
        assertTrue(predictor.predictFpsDrop());
        assertTrue(predictor.getPredictionConfidence() > 0.0);
    }
    
    @Test
    @DisplayName("Should detect performance spike")
    void testSpikeDetection() {
        PerformancePredictor predictor = new PerformancePredictor(60);
        
        // Add stable baseline
        for (int i = 0; i < 10; i++) {
            predictor.addSample(16.67);
        }
        
        // Add spike
        predictor.addSample(50.0); // 3x increase
        
        assertTrue(predictor.detectSpike());
    }
    
    @Test
    @DisplayName("Should throw on invalid constructor")
    void testInvalidConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PerformancePredictor(0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new PerformancePredictor(-1);
        });
    }
    
    @Test
    @DisplayName("Should reuse array buffer (P1 #6 optimization)")
    void testArrayBufferReuse() {
        PerformancePredictor predictor = new PerformancePredictor(60);
        
        // Fill history
        for (int i = 0; i < 60; i++) {
            predictor.addSample(16.67 + i * 0.1);
        }
        
        // Check that buffer is allocated
        int capacity1 = predictor.getReusableArrayCapacity();
        assertTrue(capacity1 >= 60, "Buffer should be at least 60");
        
        // Run prediction multiple times (should reuse buffer)
        for (int i = 0; i < 10; i++) {
            predictor.predictFpsDrop();
        }
        
        // Buffer size should remain stable (not growing)
        int capacity2 = predictor.getReusableArrayCapacity();
        assertEquals(capacity1, capacity2, "Buffer should be reused, not reallocated");
    }
    
    @Test
    @DisplayName("Should calculate confidence score correctly")
    void testConfidenceScoring() {
        PerformancePredictor predictor = new PerformancePredictor(60);
        
        // Not warmed up yet
        assertEquals(0.0, predictor.getPredictionConfidence());
        
        // Add stable samples (low variance = high confidence)
        for (int i = 0; i < 30; i++) {
            predictor.addSample(16.67 + (i % 2) * 0.1); // Very stable
        }
        
        double confidence = predictor.getPredictionConfidence();
        assertTrue(confidence > 0.8, "Stable samples should have high confidence");
        
        // Reset and add unstable samples
        predictor.reset();
        for (int i = 0; i < 30; i++) {
            predictor.addSample(10.0 + Math.random() * 20.0); // High variance
        }
        
        double lowConfidence = predictor.getPredictionConfidence();
        assertTrue(lowConfidence < 0.7, "Unstable samples should have lower confidence");
    }
    
    @Test
    @DisplayName("Should reset state correctly")
    void testReset() {
        PerformancePredictor predictor = new PerformancePredictor(60);
        
        // Add samples
        for (int i = 0; i < 30; i++) {
            predictor.addSample(16.67);
        }
        
        assertTrue(predictor.isWarmedUp());
        assertEquals(30, predictor.getSampleCount());
        
        // Reset
        predictor.reset();
        
        assertFalse(predictor.isWarmedUp());
        assertEquals(0, predictor.getSampleCount());
        assertEquals(0.0, predictor.getPredictionConfidence());
    }
}
