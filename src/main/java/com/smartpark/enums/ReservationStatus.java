package com.smartpark.enums;

public enum ReservationStatus {
    PENDING("Active Hold", "#f59e0b"),
    CLAIMED("Checked In", "#10b981"),
    EXPIRED("Expired", "#ef4444"),
    CANCELLED("Cancelled", "#64748b");

    private final String label;
    private final String colorHex;

    ReservationStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }
}
