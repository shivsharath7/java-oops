package com.smartpark.enums;

public enum VehicleType {
    CAR("Car", 30.0, 20.0),
    BIKE("Bike", 10.0, 10.0),
    EV_CAR("EV Car", 40.0, 25.0);

    private final String displayName;
    private final double firstHourRate;
    private final double additionalHourRate;

    VehicleType(String displayName, double firstHourRate, double additionalHourRate) {
        this.displayName = displayName;
        this.firstHourRate = firstHourRate;
        this.additionalHourRate = additionalHourRate;
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
}