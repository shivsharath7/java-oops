package com.smartpark.model;

public class ParkingFloor {
    private int id;
    private String floorName;
    private int floorLevel;
    private int totalSlots;

    public ParkingFloor() {}

    public ParkingFloor(int id, String floorName, int floorLevel, int totalSlots) {
        this.id = id;
        this.floorName = floorName;
        this.floorLevel = floorLevel;
        this.totalSlots = totalSlots;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }

    public int getFloorLevel() { return floorLevel; }
    public void setFloorLevel(int floorLevel) { this.floorLevel = floorLevel; }

    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }

    @Override
    public String toString() { return floorName; }
}
