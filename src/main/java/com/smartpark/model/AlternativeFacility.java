package com.smartpark.model;

public class AlternativeFacility {
    private int id;
    private String name;
    private String address;
    private int totalCapacity;
    private int availableEstimate;
    private String vehicleTypes;
    private double distanceKm;
    private String contactInfo;

    public AlternativeFacility() {}

    public AlternativeFacility(int id, String name, String address, int totalCapacity, int availableEstimate,
                               String vehicleTypes, double distanceKm, String contactInfo) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.totalCapacity = totalCapacity;
        this.availableEstimate = availableEstimate;
        this.vehicleTypes = vehicleTypes;
        this.distanceKm = distanceKm;
        this.contactInfo = contactInfo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }

    public int getAvailableEstimate() { return availableEstimate; }
    public void setAvailableEstimate(int availableEstimate) { this.availableEstimate = availableEstimate; }

    public String getVehicleTypes() { return vehicleTypes; }
    public void setVehicleTypes(String vehicleTypes) { this.vehicleTypes = vehicleTypes; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
}
