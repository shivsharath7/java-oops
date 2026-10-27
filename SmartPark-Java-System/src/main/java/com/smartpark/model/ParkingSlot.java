package com.smartpark.model;

import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;

public class ParkingSlot {
    private int id;
    private String slotNumber;
    private String floor;
    private String zone;
    private VehicleType vehicleType;
    private SlotStatus status;
    private boolean nearEntrance;
    private boolean hasChargingStation;

    public ParkingSlot() {}

    public ParkingSlot(int id, String slotNumber, String floor, String zone, VehicleType vehicleType, SlotStatus status, boolean nearEntrance, boolean hasChargingStation) {
        this.id = id;
        this.slotNumber = slotNumber;
        this.floor = floor;
        this.zone = zone;
        this.vehicleType = vehicleType;
        this.status = status;
        this.nearEntrance = nearEntrance;
        this.hasChargingStation = hasChargingStation;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public SlotStatus getStatus() { return status; }
    public void setStatus(SlotStatus status) { this.status = status; }

    public boolean isNearEntrance() { return nearEntrance; }
    public void setNearEntrance(boolean nearEntrance) { this.nearEntrance = nearEntrance; }

    public boolean hasChargingStation() { return hasChargingStation; }
    public void setHasChargingStation(boolean hasChargingStation) { this.hasChargingStation = hasChargingStation; }
}