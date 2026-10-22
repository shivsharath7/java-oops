package com.smartpark.enums;

public enum DisputeStatus {
    OPEN("Open", "#ef4444"),
    UNDER_INVESTIGATION("Under Review", "#f59e0b"),
    RESOLVED("Resolved / Closed", "#10b981"),
    REJECTED("Rejected", "#64748b");

    private final String label;
    private final String colorHex;

    DisputeStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }
}
