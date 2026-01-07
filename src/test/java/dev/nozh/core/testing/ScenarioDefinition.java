package dev.nozh.core.testing;

import java.util.Collections;
import java.util.List;

public final class ScenarioDefinition {
    private final String id;
    private final String title;
    private final String description;
    private final int defaultDurationSeconds;
    private final List<ScenarioStep> steps;

    public ScenarioDefinition(String id, String title, String description, int defaultDurationSeconds, List<ScenarioStep> steps) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.defaultDurationSeconds = defaultDurationSeconds;
        this.steps = List.copyOf(steps);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultDurationSeconds() {
        return defaultDurationSeconds;
    }

    public List<ScenarioStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }
}
