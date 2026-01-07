package dev.nozh.core.testing;

public final class ScenarioStep {
    public enum StepType {
        TELEPORT,
        ACTION,
        WAIT,
        NOTE
    }

    private final StepType type;
    private final String description;
    private final String command;
    private final Integer durationSeconds;

    private ScenarioStep(StepType type, String description, String command, Integer durationSeconds) {
        this.type = type;
        this.description = description;
        this.command = command;
        this.durationSeconds = durationSeconds;
    }

    public static ScenarioStep teleport(String command, String description) {
        return new ScenarioStep(StepType.TELEPORT, description, command, null);
    }

    public static ScenarioStep action(String description, int durationSeconds) {
        return new ScenarioStep(StepType.ACTION, description, null, durationSeconds);
    }

    public static ScenarioStep waitFor(String description, int durationSeconds) {
        return new ScenarioStep(StepType.WAIT, description, null, durationSeconds);
    }

    public static ScenarioStep note(String description) {
        return new ScenarioStep(StepType.NOTE, description, null, null);
    }

    public StepType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getCommand() {
        return command;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }
}
