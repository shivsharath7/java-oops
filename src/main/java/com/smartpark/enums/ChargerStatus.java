package com.smartpark.enums;

public enum ChargerStatus {
    AVAILABLE("Available", "#10b981"),
    IN_USE("In Use / Charging", "#3b82f6"),
    MAINTENANCE("Under Maintenance", "#f59e0b"),
    OFFLINE("Offline", "#64748b");

    private final String label;
    private final String colorHex;

    ChargerStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }
}
