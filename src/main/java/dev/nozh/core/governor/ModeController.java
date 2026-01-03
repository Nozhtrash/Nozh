package dev.nozh.core.governor;

/**
 * Mode controller wrapper (Contract 6).
 *
 * Encapsulates ModePolicy so callers don't need to reason about policy shape.
 */
public final class ModeController {

    private final ModePolicy policy;

    private ModeController(ModePolicy policy) {
        this.policy = policy;
    }

    public static ModeController forMode(GovernorMode mode) {
        return new ModeController(ModePolicy.forMode(mode));
    }

    public ModePolicy policy() {
        return policy;
    }

    public boolean requiresUserConfirmation() {
        return policy.requiresUserConfirmation();
    }
}
