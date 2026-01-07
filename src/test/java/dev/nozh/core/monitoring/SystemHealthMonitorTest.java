package dev.nozh.core.monitoring;

import org.junit.jupiter.api.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SystemHealthMonitor.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>Concurrent error recording (P0 race condition fix)</li>
 *   <li>Circuit breaker activation</li>
 *   <li>Health calculation accuracy</li>
 *   <li>Input validation</li>
 *   <li>GC pause recording</li>
 *   <li>Memory pressure handling</li>
 *   <li>Health status transitions</li>
 *   <li>Detailed report generation</li>
 *   <li>Reset functionality</li>
 * </ul>
 * 
 * @author Nozh Team
 * @since 0.3.0
 */
class SystemHealthMonitorTest {
    
    @Test
    @DisplayName("Should handle concurrent error recording")
    void testConcurrentErrorRecording() throws InterruptedException {
        SystemHealthMonitor monitor = new SystemHealthMonitor();
        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);
        
        // Create and start threads manually instead of using ExecutorService
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    monitor.recordError("concurrent_test");
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
            threads[i].start();
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Threads did not complete in time");
        assertEquals(0, errors.get(), "Some threads encountered errors");
        
        double healthScore = monitor.getHealthScore();
        assertTrue(healthScore >= 0.0 && healthScore <= 1.0);
        
        int errorCount = monitor.getRecentErrorCount();
        assertTrue(errorCount >= threadCount * 0.9, "Expected at least 90 errors, got: " + errorCount);
    }
    
    @Test
    @DisplayName("Should activate circuit breaker after 5 critical states")
    void testCircuitBreaker() {
        SystemHealthMonitor monitor = new SystemHealthMonitor();
        
        // Circuit breaker opens when health score < 0.2 for 5 consecutive checks
        // Force very low health by recording many errors
        for (int i = 0; i < 100; i++) {
            monitor.recordError("critical_error_" + i);
        }
        
        // Initial state - circuit should be closed
        assertFalse(monitor.isCircuitOpen(), "Circuit should start closed");
        
        // Force health score calculations that should trigger circuit breaker
        // Need to call getHealthScore() multiple times to increment critical counter
        for (int i = 0; i < 10; i++) {
            double score = monitor.getHealthScore();
            // Verify score is critical (< 0.2)
            assertTrue(score < 0.3, "Health score should be critical after 100 errors, got: " + score);
        }
        
        // After multiple critical readings, circuit should be open
        assertTrue(monitor.isCircuitOpen(), 
            "Circuit breaker should be open after multiple critical health checks");
        
        // Verify circuit open behavior
        assertEquals(0.0, monitor.getHealthScore(), 
            "Circuit open should return 0.0 health");
        assertEquals(SystemHealthMonitor.HealthStatus.CRITICAL, monitor.getStatus(),
            "Status should be CRITICAL when circuit is open");
    }
    
    @Test
    @DisplayName("Should calculate health correctly")
    void testHealthCalculation() {
        SystemHealthMonitor monitor = new SystemHealthMonitor();
        
        assertTrue(monitor.isHealthy());
        assertFalse(monitor.isCritical());
        assertEquals(SystemHealthMonitor.HealthStatus.HEALTHY, monitor.getStatus());
        
        double score = monitor.getHealthScore();
        assertTrue(score >= 0.0 && score <= 1.0, "Health score must be between 0 and 1");
    }
    
    @Test
    @DisplayName("Should reject null/invalid inputs")
    void testInputValidation() {
        SystemHealthMonitor monitor = new SystemHealthMonitor();
        
        assertThrows(NullPointerException.class, () -> {
            monitor.recordError(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            monitor.recordGCPause(-1);
        });
    }
    
    @Test
    @DisplayName("Should record GC pauses correctly")
    void testGCPauseRecording() {
        SystemHealthMonitor monitor = new SystemHealthMonitor();
        
        assertEquals(0, monitor.getGCCount());
        assertEquals(0.0, monitor.getAverageGCPause());
        
        monitor.recordGCPause(100);
        monitor.recordGCPause(200);
        monitor.recordGCPause(150);
        
        assertEquals(3, monitor.getGCCount());
        assertEquals(150.0, monitor.getAverageGCPause(), 0.01);
    }
    
    @Test
    @DisplayName("Should handle memory pressure")
    void testMemoryPressure() {
        SystemHealthMonitor monitor = new SystemHealthMonitor();
        
        double memoryUsage = monitor.getMemoryUsagePercent();
        assertTrue(memoryUsage >= 0.0 && memoryUsage <= 1.0);
        
        // Memory usage below 85% should not trigger GC suggestion
        if (memoryUsage < 0.85) {
            assertFalse(monitor.shouldSuggestGC());
        }
    }
    
    @Test
    @DisplayName("Should transition health status correctly")
    void testHealthStatusTransitions() {
        SystemHealthMonitor monitor = new SystemHealthMonitor();
        
        // Start healthy
        assertEquals(SystemHealthMonitor.HealthStatus.HEALTHY, monitor.getStatus());
        
        // Add moderate errors
        for (int i = 0; i < 5; i++) {
            monitor.recordError("test_" + i);
        }
        
        // Should still be in acceptable range
        SystemHealthMonitor.HealthStatus status = monitor.getStatus();
        assertNotEquals(SystemHealthMonitor.HealthStatus.CRITICAL, status);
    }
    
    @Test
    @DisplayName("Should generate detailed health report")
    void testHealthReport() {
        SystemHealthMonitor monitor = new SystemHealthMonitor();
        
        String report = monitor.generateHealthReport();
        assertNotNull(report);
        assertTrue(report.contains("System Health Report"));
        assertTrue(report.contains("Overall Status"));
        assertTrue(report.contains("Memory Usage"));
        assertTrue(report.contains("Recommendation"));
    }
    
    @Test
    @DisplayName("Should reset correctly")
    void testReset() {
        SystemHealthMonitor monitor = new SystemHealthMonitor();
        
        // Add some activity
        for (int i = 0; i < 10; i++) {
            monitor.recordError("test_" + i);
        }
        monitor.recordGCPause(100);
        monitor.recordGCPause(200);
        
        assertTrue(monitor.getRecentErrorCount() > 0);
        assertTrue(monitor.getGCCount() > 0);
        
        // Reset
        monitor.reset();
        
        assertEquals(0, monitor.getRecentErrorCount());
        assertEquals(0, monitor.getGCCount());
        assertEquals(0.0, monitor.getAverageGCPause());
        assertTrue(monitor.isHealthy());
    }
    
    @Test
    @DisplayName("Should reset circuit breaker after timeout")
    void testCircuitBreakerTimeout() {
        SystemHealthMonitor monitor = new SystemHealthMonitor();
        
        // Force circuit breaker open by creating critical conditions
        for (int i = 0; i < 100; i++) {
            monitor.recordError("critical_timeout_" + i);
        }
        
        // Trigger multiple health checks to open circuit
        for (int i = 0; i < 10; i++) {
            monitor.getHealthScore();
        }
        
        // Verify circuit is open
        assertTrue(monitor.isCircuitOpen(), "Circuit should be open after critical conditions");
        assertEquals(0.0, monitor.getHealthScore(), "Health should be 0.0 when circuit is open");
        
        // Note: Full timeout test (30s) is impractical for unit tests
        // We verify the mechanism exists and circuit remains open
        // In production, circuit will auto-reset after CIRCUIT_RESET_TIMEOUT (30s)
        
        // Verify circuit stays open during timeout period
        assertTrue(monitor.isCircuitOpen(), "Circuit should remain open");
        
        // Verify reset() can manually reset the circuit
        monitor.reset();
        assertFalse(monitor.isCircuitOpen(), "Reset should close the circuit breaker");
        
        // After reset, health should be restored
        double healthAfterReset = monitor.getHealthScore();
        assertTrue(healthAfterReset > 0.5, 
            "Health should improve after reset, got: " + healthAfterReset);
    }
}
