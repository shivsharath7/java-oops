package com.smartpark.enums;

/**
 * Parking Slot Operational Status.
 * Green: Available
 * Red: Occupied
 * Amber: Temporarily reserved
 * Blue: EV charging bay in use
 * Grey: Maintenance / Out of service
 */
public enum SlotStatus {
    AVAILABLE("Available", "#10b981", "badge-available"),
    OCCUPIED("Occupied", "#ef4444", "badge-occupied"),
    RESERVED("Reserved", "#f59e0b", "badge-reserved"),
    EV_CHARGING("EV Charging", "#3b82f6", "badge-charging"),
    MAINTENANCE("Maintenance", "#64748b", "badge-maintenance");

    private final String label;
    private final String colorHex;
    private final String cssClass;

    SlotStatus(String label, String colorHex, String cssClass) {
        this.label = label;
        this.colorHex = colorHex;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getCssClass() {
        return cssClass;
    }
}