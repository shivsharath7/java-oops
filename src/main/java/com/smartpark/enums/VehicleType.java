package com.smartpark.enums;

/**
 * Supported vehicle categories in SmartPark.
 */
public enum VehicleType {
    CAR("Car", 30.0, 20.0, "Standard 4-wheeler passenger vehicle"),
    BIKE("Two-Wheeler", 10.0, 10.0, "Motorcycles, scooters, and mopeds"),
    EV_CAR("Electric Vehicle (EV)", 40.0, 25.0, "Battery-electric vehicle with optional charging"),
    ACCESSIBLE_CAR("Accessible / Special Needs", 20.0, 15.0, "Specially designated accessibility vehicle");

    private final String displayName;
    private final double firstHourRate;
    private final double additionalHourRate;
    private final String description;

    VehicleType(String displayName, double firstHourRate, double additionalHourRate, String description) {
        this.displayName = displayName;
        this.firstHourRate = firstHourRate;
        this.additionalHourRate = additionalHourRate;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getFirstHourRate() {
        return firstHourRate;
    }

    public double getAdditionalHourRate() {
        return additionalHourRate;
    }

    public String getDescription() {
        return description;
    }
}