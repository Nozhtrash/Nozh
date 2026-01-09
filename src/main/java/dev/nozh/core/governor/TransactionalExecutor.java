package dev.nozh.core.governor;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.ActionResult;
import dev.nozh.core.capability.StateSnapshot;
import dev.nozh.core.capability.CapabilityProviderRegistry;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import dev.nozh.core.telemetry.IntegratedRingTelemetryBuffer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Executes actions transactionally with automatic rollback on failure.
 * 
 * Measures performance before and after action execution, and rolls back
 * if the action did not improve performance or made it worse.
 */
public class TransactionalExecutor {
    
    private final IntegratedRingTelemetryBuffer telemetryBuffer;
    private final Map<String, ActionTransaction> activeTransactions = new ConcurrentHashMap<>();
    
    // Improvement thresholds
    private static final double FPS_IMPROVEMENT_THRESHOLD = 0.05; // 5% FPS gain
    private static final double P95_IMPROVEMENT_THRESHOLD = 0.10; // 10% P95 reduction
    private static final double SPIKE_REDUCTION_THRESHOLD = 0.30; // 30% spike reduction
    
    public TransactionalExecutor(IntegratedRingTelemetryBuffer telemetryBuffer) {
        this.telemetryBuffer = telemetryBuffer;
    }
    
    /**
     * Execute an action with automatic rollback if it doesn't improve performance.
     * 
     * @param actionId unique identifier for the action
     * @param action supplier that executes the action
     * @param stabilizationPeriod time to wait before measuring results
     * @return future with transaction result
     */
    public CompletableFuture<TransactionResult> executeWithRollback(
            String actionId,
            Supplier<ActionResult> action,
            Duration stabilizationPeriod) {
        
        // Create transaction
        ActionTransaction tx = new ActionTransaction(actionId);
        activeTransactions.put(actionId, tx);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Capture state BEFORE
                TelemetrySnapshot before = telemetryBuffer.snapshot();
                // Removed: StateSnapshot stateBefore = StateSnapshot.captureAll();
                // The snapshot will come from the ActionResult itself
                
                NozhConstants.LOGGER.info("[TX {}] Captured pre-action state", actionId);
                
                // 2. Execute action
                ActionResult result = action.get();
                
                if (!result.isSuccess()) {
                    NozhConstants.LOGGER.warn("[TX {}] Action failed: {}", actionId, result.getError());
                    return TransactionResult.failed(result.getError());
                }
                
                NozhConstants.LOGGER.info("[TX {}] Action executed, waiting for stabilization ({}ms)", 
                                        actionId, stabilizationPeriod.toMillis());
                
                // 3. Wait for stabilization
                Thread.sleep(stabilizationPeriod.toMillis());
                
                // 4. Measure result AFTER
                TelemetrySnapshot after = telemetryBuffer.snapshot();
                
                // 5. Evaluate improvement
                ImprovementEvaluation eval = evaluateImprovement(before, after);
                
                NozhConstants.LOGGER.info("[TX {}] Improvement evaluation: improved={}, reason={}",
                                        actionId, eval.improved, eval.reason);
                
                if (!eval.improved) {
                    // ROLLBACK
                    NozhConstants.LOGGER.warn("[TX {}] Action did not improve performance, rolling back", actionId);
                    
                    if (result.canRollback()) {
                        // Use CapabilityProviderRegistry.restore() instead of non-existent restoreAll()
                        boolean restored = CapabilityProviderRegistry.restore(
                            actionId, 
                            result.getSnapshot()
                        );
                        
                        if (restored) {
                            NozhConstants.LOGGER.info("[TX {}] Rollback completed", actionId);
                        } else {
                            NozhConstants.LOGGER.warn("[TX {}] Rollback failed", actionId);
                        }
                    } else {
                        NozhConstants.LOGGER.warn("[TX {}] Cannot rollback - no snapshot available", actionId);
                    }
                    
                    return TransactionResult.rolledBack(eval.reason);
                }
                
                // 6. Commit
                tx.commit();
                NozhConstants.LOGGER.info("[TX {}] Transaction committed successfully", actionId);
                
                return TransactionResult.success(after, eval);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                NozhConstants.LOGGER.error("[TX {}] Transaction interrupted", actionId, e);
                return TransactionResult.failed("Transaction interrupted");
                
            } catch (Exception e) {
                NozhConstants.LOGGER.error("[TX {}] Transaction failed with exception", actionId, e);
                return TransactionResult.failed("Exception: " + e.getMessage());
                
            } finally {
                activeTransactions.remove(actionId);
            }
        });
    }
    
    /**
     * Evaluate if performance improved after action.
     */
    private ImprovementEvaluation evaluateImprovement(TelemetrySnapshot before, TelemetrySnapshot after) {
        // Can't evaluate if insufficient data
        if (before.getSampleCount() < 10 || after.getSampleCount() < 10) {
            return ImprovementEvaluation.insufficient("Not enough samples");
        }
        
        // Calculate metrics
        double fpsImprovement = (after.getAvgFps() - before.getAvgFps()) / before.getAvgFps();
        double p95Improvement = (before.getP95FrametimeMs() - after.getP95FrametimeMs()) / 
                               before.getP95FrametimeMs();
        double spikeReduction = (before.getSpikeCount() - after.getSpikeCount()) / 
                               (double) Math.max(1, before.getSpikeCount());
        
        // Check for negative impact
        if (fpsImprovement < -0.05) {
            return ImprovementEvaluation.degraded(
                String.format("FPS decreased by %.1f%%", Math.abs(fpsImprovement * 100))
            );
        }
        
        if (p95Improvement < -0.10) {
            return ImprovementEvaluation.degraded(
                String.format("P95 frametime increased by %.1f%%", Math.abs(p95Improvement * 100))
            );
        }
        
        // Check for positive impact
        if (fpsImprovement > FPS_IMPROVEMENT_THRESHOLD) {
            return ImprovementEvaluation.improved(
                String.format("FPS improved by %.1f%%", fpsImprovement * 100),
                fpsImprovement
            );
        }
        
        if (p95Improvement > P95_IMPROVEMENT_THRESHOLD) {
            return ImprovementEvaluation.improved(
                String.format("P95 improved by %.1f%%", p95Improvement * 100),
                p95Improvement
            );
        }
        
        if (spikeReduction > SPIKE_REDUCTION_THRESHOLD) {
            return ImprovementEvaluation.improved(
                String.format("Spikes reduced by %.1f%%", spikeReduction * 100),
                spikeReduction
            );
        }
        
        // No significant change
        return ImprovementEvaluation.noChange("No significant performance change detected");
    }
    
    /**
     * Check if a transaction is currently active.
     */
    public boolean hasActiveTransaction(String actionId) {
        return activeTransactions.containsKey(actionId);
    }
    
    /**
     * Get count of active transactions.
     */
    public int getActiveTransactionCount() {
        return activeTransactions.size();
    }
    
    // Inner classes
    
    private static class ActionTransaction {
        final String actionId;
        final long startTime;
        boolean committed = false;
        
        ActionTransaction(String actionId) {
            this.actionId = actionId;
            this.startTime = System.currentTimeMillis();
        }
        
        void commit() {
            this.committed = true;
        }
    }
    
    public static class TransactionResult {
        public enum Status {
            SUCCESS,
            ROLLED_BACK,
            FAILED
        }
        
        private final Status status;
        private final String message;
        private final TelemetrySnapshot afterSnapshot;
        private final ImprovementEvaluation evaluation;
        
        private TransactionResult(Status status, String message, 
                                 TelemetrySnapshot afterSnapshot, 
                                 ImprovementEvaluation evaluation) {
            this.status = status;
            this.message = message;
            this.afterSnapshot = afterSnapshot;
            this.evaluation = evaluation;
        }
        
        public static TransactionResult success(TelemetrySnapshot after, ImprovementEvaluation eval) {
            return new TransactionResult(Status.SUCCESS, "Success", after, eval);
        }
        
        public static TransactionResult rolledBack(String reason) {
            return new TransactionResult(Status.ROLLED_BACK, reason, null, null);
        }
        
        public static TransactionResult failed(String reason) {
            return new TransactionResult(Status.FAILED, reason, null, null);
        }
        
        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
        
        public boolean wasRolledBack() {
            return status == Status.ROLLED_BACK;
        }
        
        public Status getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public ImprovementEvaluation getEvaluation() {
            return evaluation;
        }
    }
    
    public static class ImprovementEvaluation {
        final boolean improved;
        final String reason;
        final double improvementScore;
        
        private ImprovementEvaluation(boolean improved, String reason, double score) {
            this.improved = improved;
            this.reason = reason;
            this.improvementScore = score;
        }
        
        static ImprovementEvaluation improved(String reason, double score) {
            return new ImprovementEvaluation(true, reason, score);
        }
        
        static ImprovementEvaluation degraded(String reason) {
            return new ImprovementEvaluation(false, reason, -1.0);
        }
        
        static ImprovementEvaluation noChange(String reason) {
            return new ImprovementEvaluation(false, reason, 0.0);
        }
        
        static ImprovementEvaluation insufficient(String reason) {
            return new ImprovementEvaluation(false, reason, 0.0);
        }
    }
}
