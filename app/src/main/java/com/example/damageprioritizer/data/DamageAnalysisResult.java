package com.example.damageprioritizer.data;

public class DamageAnalysisResult {

    public enum Severity {
        MINOR, MODERATE, MAJOR
    }

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    private final Severity severity;
    private final Priority priority;
    private final float confidence;
    private final String explanation;

    public DamageAnalysisResult(Severity severity, float confidence, String explanation) {
        this.severity = severity;
        this.confidence = confidence;
        this.explanation = explanation;
        this.priority = mapSeverityToPriority(severity);
    }

    public Severity getSeverity() {
        return severity;
    }

    public Priority getPriority() {
        return priority;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getExplanation() {
        return explanation;
    }

    private Priority mapSeverityToPriority(Severity severity) {
        switch (severity) {
            case MINOR:
                return Priority.LOW;
            case MODERATE:
                return Priority.MEDIUM;
            case MAJOR:
                return Priority.HIGH;
            default:
                return Priority.LOW;
        }
    }
}
