## Priority 3 Bundle (single PR)

This PR ships a **large v0.3 bundle** in one go:

- PredictiveAnalyzer: simple linear regression to predict next frametime and gate actions when recovery is expected.
- SpikePrediction: lightweight spike detection signal.
- EfficiencyScorer: gain/cost ratio scoring and `finalScore = efficiency * confidence`.
- Wiring: the Priority2 client entrypoint now uses predictive gating + scoring when queuing manual suggestions.
- Localization: adds missing translations for the manual apply keybind in pt_br / fr_fr / de_de.

Safety goals:
- Predictive layer is conservative: only suppresses suggestions when improvement trend is clear.
- Scoring is informational (included in reason text) and does not change action execution order yet.
