package com.smartpark.model;

public class ParkingZone {
    private int id;
    private String floorName;
    private String zoneName;
    private int congestionThreshold;

    public ParkingZone() {}

    public ParkingZone(int id, String floorName, String zoneName, int congestionThreshold) {
        this.id = id;
        this.floorName = floorName;
        this.zoneName = zoneName;
        this.congestionThreshold = congestionThreshold;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }

    public int getCongestionThreshold() { return congestionThreshold; }
    public void setCongestionThreshold(int congestionThreshold) { this.congestionThreshold = congestionThreshold; }

    @Override
    public String toString() { return zoneName; }
}
