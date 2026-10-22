package com.smartpark.enums;

public enum AnomalySeverity {
    INFO("Info", "#3b82f6"),
    WARNING("Warning", "#f59e0b"),
    CRITICAL("Critical", "#ef4444");

    private final String label;
    private final String colorHex;

    AnomalySeverity(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }
}
