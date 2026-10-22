package com.smartpark.model;

import com.smartpark.enums.VehicleType;

/**
 * Electric Vehicle model extending Vehicle.
 */
public class ElectricCar extends Vehicle {
    private double batteryCapacityKwh = 50.0;
    private double currentBatteryPercent = 50.0;

    public ElectricCar() {
        super();
        setVehicleType(VehicleType.EV_CAR);
        setRequiresCharging(true);
    }

    public ElectricCar(String vehicleNumber, String customerName, String contactNumber) {
        super(0, vehicleNumber, VehicleType.EV_CAR, customerName, contactNumber, false, true);
    }

    public ElectricCar(int id, String vehicleNumber, String customerName, String contactNumber,
                       double batteryCapacityKwh, double currentBatteryPercent) {
        super(id, vehicleNumber, VehicleType.EV_CAR, customerName, contactNumber, false, true);
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.currentBatteryPercent = currentBatteryPercent;
    }

    public double getBatteryCapacityKwh() { return batteryCapacityKwh; }
    public void setBatteryCapacityKwh(double batteryCapacityKwh) { this.batteryCapacityKwh = batteryCapacityKwh; }

    public double getCurrentBatteryPercent() { return currentBatteryPercent; }
    public void setCurrentBatteryPercent(double currentBatteryPercent) { this.currentBatteryPercent = currentBatteryPercent; }
}
