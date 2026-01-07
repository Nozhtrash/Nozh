package dev.nozh.core.evaluation;

import dev.nozh.core.telemetry.TelemetrySnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates action outcomes comprehensively.
 * 
 * Assesses:
 * - FPS improvement (primary)
 * - Frame pacing stability
 * - Visual quality impact
 * - Gameplay responsiveness
 * - Side effects
 * 
 * Determines if action should be kept or rolled back.
 * 
 * EVALUATION: Post-action analysis
 */
public final class ActionEvaluator {

    private static final double MIN_FPS_IMPROVEMENT = 3.0;
    private static final double MAX_VISUAL_IMPACT = 0.3;
    private static final double MAX_GAMEPLAY_IMPACT = 0.2;
    private static final double MIN_OVERALL_SCORE = 0.6;

    /**
     * Evaluate action outcome.
     */
    public static EvaluationResult evaluate(
            String actionId,
            TelemetrySnapshot before,
            TelemetrySnapshot after,
            double visualImpact,
            double gameplayImpact) {

        List<String> findings = new ArrayList<>();
        List<String> concerns = new ArrayList<>();

        // Calculate FPS change
        double fpsBefore = 1000.0 / before.avgFrametimeMs();
        double fpsAfter = 1000.0 / after.avgFrametimeMs();
        double fpsDelta = fpsAfter - fpsBefore;

        findings.add(String.format("FPS: %.1f -> %.1f (%+.1f)", fpsBefore, fpsAfter, fpsDelta));

        // Check FPS improvement
        double fpsScore = calculateFpsScore(fpsDelta);
        if (fpsDelta < MIN_FPS_IMPROVEMENT) {
            concerns.add("Insufficient FPS improvement");
        }

        // Check frame pacing
        double pacingBefore = before.p95FrametimeMs() / before.avgFrametimeMs();
        double pacingAfter = after.p95FrametimeMs() / after.avgFrametimeMs();
        double pacingScore = calculatePacingScore(pacingBefore, pacingAfter);

        if (pacingAfter > pacingBefore * 1.2) {
            concerns.add("Worse frame pacing");
        }

        findings.add(String.format("Frame pacing ratio: %.2f -> %.2f", pacingBefore, pacingAfter));

        // Check spike reduction
        int spikeDelta = after.spikeCount() - before.spikeCount();
        double spikeScore = spikeDelta <= 0 ? 1.0 : Math.max(0.0, 1.0 - (spikeDelta / 10.0));

        if (spikeDelta > 5) {
            concerns.add("Increased frame spikes");
        }

        findings.add(String.format("Spikes: %d -> %d (%+d)", 
                before.spikeCount(), after.spikeCount(), spikeDelta));

        // Check visual impact
        double visualScore = 1.0 - visualImpact;
        if (visualImpact > MAX_VISUAL_IMPACT) {
            concerns.add(String.format("High visual impact: %.1f%%", visualImpact * 100));
        }

        findings.add(String.format("Visual impact: %.1f%%", visualImpact * 100));

        // Check gameplay impact
        double gameplayScore = 1.0 - gameplayImpact;
        if (gameplayImpact > MAX_GAMEPLAY_IMPACT) {
            concerns.add(String.format("High gameplay impact: %.1f%%", gameplayImpact * 100));
        }

        findings.add(String.format("Gameplay impact: %.1f%%", gameplayImpact * 100));

        // Calculate overall score
        double overallScore = (
                fpsScore * 0.4 +
                pacingScore * 0.2 +
                spikeScore * 0.1 +
                visualScore * 0.2 +
                gameplayScore * 0.1
        );

        // Determine verdict
        Verdict verdict = determineVerdict(overallScore, fpsDelta, concerns.size());

        return new EvaluationResult(
                actionId,
                overallScore,
                verdict,
                fpsDelta,
                visualImpact,
                gameplayImpact,
                findings,
                concerns
        );
    }

    private static double calculateFpsScore(double fpsDelta) {
        if (fpsDelta < 0) return 0.0;
        if (fpsDelta >= 20) return 1.0;
        return fpsDelta / 20.0;
    }

    private static double calculatePacingScore(double before, double after) {
        if (after <= before) return 1.0;
        double increase = (after - before) / before;
        return Math.max(0.0, 1.0 - increase);
    }

    private static Verdict determineVerdict(double score, double fpsDelta, int concernCount) {
        if (score < MIN_OVERALL_SCORE) {
            return Verdict.ROLLBACK;
        }

        if (fpsDelta < 0 || concernCount >= 3) {
            return Verdict.ROLLBACK;
        }

        if (score >= 0.8 && fpsDelta >= 10) {
            return Verdict.EXCELLENT;
        }

        if (score >= 0.7 && fpsDelta >= 5) {
            return Verdict.GOOD;
        }

        return Verdict.ACCEPTABLE;
    }

    public enum Verdict {
        EXCELLENT,   // Keep and learn from
        GOOD,        // Keep
        ACCEPTABLE,  // Keep but monitor
        ROLLBACK     // Revert action
    }

    public record EvaluationResult(
            String actionId,
            double overallScore,
            Verdict verdict,
            double fpsDelta,
            double visualImpact,
            double gameplayImpact,
            List<String> findings,
            List<String> concerns
    ) {
        public boolean shouldRollback() {
            return verdict == Verdict.ROLLBACK;
        }

        public boolean isSuccess() {
            return verdict == Verdict.EXCELLENT || verdict == Verdict.GOOD;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Evaluation: ").append(actionId).append(" ===").append("\n");
            sb.append("Verdict: ").append(verdict).append("\n");
            sb.append("Overall Score: ").append(String.format("%.2f", overallScore)).append("\n");
            sb.append("\nFindings:\n");
            for (String finding : findings) {
                sb.append("  - ").append(finding).append("\n");
            }
            if (!concerns.isEmpty()) {
                sb.append("\nConcerns:\n");
                for (String concern : concerns) {
                    sb.append("  ! ").append(concern).append("\n");
                }
            }
            return sb.toString();
        }
    }
}
