package com.smartpark.model;

import com.smartpark.enums.VehicleType;

/**
 * Base abstract class representing a Vehicle in SmartPark.
 * Demonstrates OOP Encapsulation and Polymorphism.
 */
public class Vehicle {
    private int id;
    private String vehicleNumber;
    private VehicleType vehicleType;
    private String customerName;
    private String contactNumber;
    private boolean accessibilityNeeded;
    private boolean requiresCharging;

    public Vehicle() {}

    public Vehicle(String vehicleNumber, VehicleType vehicleType, String customerName, String contactNumber) {
        this(0, vehicleNumber, vehicleType, customerName, contactNumber, false, false);
    }

    public Vehicle(int id, String vehicleNumber, VehicleType vehicleType, String customerName, String contactNumber) {
        this(id, vehicleNumber, vehicleType, customerName, contactNumber, false, false);
    }

    public Vehicle(int id, String vehicleNumber, VehicleType vehicleType, String customerName, String contactNumber,
                   boolean accessibilityNeeded, boolean requiresCharging) {
        this.id = id;
        this.vehicleNumber = (vehicleNumber != null) ? vehicleNumber.trim().toUpperCase() : "";
        this.vehicleType = vehicleType;
        this.customerName = customerName;
        this.contactNumber = contactNumber;
        this.accessibilityNeeded = accessibilityNeeded;
        this.requiresCharging = requiresCharging;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = (vehicleNumber != null) ? vehicleNumber.trim().toUpperCase() : "";
    }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public boolean isAccessibilityNeeded() { return accessibilityNeeded; }
    public void setAccessibilityNeeded(boolean accessibilityNeeded) { this.accessibilityNeeded = accessibilityNeeded; }

    public boolean isRequiresCharging() { return requiresCharging; }
    public void setRequiresCharging(boolean requiresCharging) { this.requiresCharging = requiresCharging; }

    public VehicleType getRequiredSlotType() {
        if (accessibilityNeeded) return VehicleType.ACCESSIBLE_CAR;
        if (requiresCharging && vehicleType == VehicleType.EV_CAR) return VehicleType.EV_CAR;
        return vehicleType;
    }

    public double calculateBaseHourlyRate() {
        return vehicleType != null ? vehicleType.getFirstHourRate() : 30.0;
    }

    @Override
    public String toString() {
        return vehicleNumber + " (" + (vehicleType != null ? vehicleType.getDisplayName() : "Unknown") + ")";
    }
}