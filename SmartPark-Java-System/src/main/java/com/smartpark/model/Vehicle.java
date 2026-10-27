package com.smartpark.model;

import com.smartpark.enums.VehicleType;

public class Vehicle {
    private int id;
    private String vehicleNumber;
    private VehicleType vehicleType;
    private String customerName;
    private String contactNumber;

    public Vehicle() {}

    public Vehicle(String vehicleNumber, VehicleType vehicleType, String customerName, String contactNumber) {
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.customerName = customerName;
        this.contactNumber = contactNumber;
    }

    public Vehicle(int id, String vehicleNumber, VehicleType vehicleType, String customerName, String contactNumber) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.customerName = customerName;
        this.contactNumber = contactNumber;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
}