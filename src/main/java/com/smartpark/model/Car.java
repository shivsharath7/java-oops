package com.smartpark.model;

import com.smartpark.enums.VehicleType;

/**
 * Car model extending Vehicle.
 */
public class Car extends Vehicle {
    private int seatingCapacity = 5;

    public Car() {
        super();
        setVehicleType(VehicleType.CAR);
    }

    public Car(String vehicleNumber, String customerName, String contactNumber) {
        super(0, vehicleNumber, VehicleType.CAR, customerName, contactNumber);
    }

    public Car(int id, String vehicleNumber, String customerName, String contactNumber, int seatingCapacity) {
        super(id, vehicleNumber, VehicleType.CAR, customerName, contactNumber);
        this.seatingCapacity = seatingCapacity;
    }

    public int getSeatingCapacity() { return seatingCapacity; }
    public void setSeatingCapacity(int seatingCapacity) { this.seatingCapacity = seatingCapacity; }
}
