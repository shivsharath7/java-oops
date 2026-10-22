package com.smartpark.model;

import com.smartpark.enums.VehicleType;

/**
 * Two-wheeler / Bike model extending Vehicle.
 */
public class Bike extends Vehicle {
    private boolean helmetIncluded;

    public Bike() {
        super();
        setVehicleType(VehicleType.BIKE);
    }

    public Bike(String vehicleNumber, String customerName, String contactNumber) {
        super(0, vehicleNumber, VehicleType.BIKE, customerName, contactNumber);
    }

    public Bike(int id, String vehicleNumber, String customerName, String contactNumber, boolean helmetIncluded) {
        super(id, vehicleNumber, VehicleType.BIKE, customerName, contactNumber);
        this.helmetIncluded = helmetIncluded;
    }

    public boolean isHelmetIncluded() { return helmetIncluded; }
    public void setHelmetIncluded(boolean helmetIncluded) { this.helmetIncluded = helmetIncluded; }
}
