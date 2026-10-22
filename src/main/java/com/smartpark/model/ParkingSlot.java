package com.smartpark.model;

import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;

/**
 * Model representing a physical Parking Slot.
 */
public class ParkingSlot {
    private int id;
    private String slotNumber;
    private String floor;
    private String zone;
    private VehicleType vehicleType;
    private SlotStatus status;
    private boolean nearEntrance;
    private boolean nearExit;
    private boolean hasChargingStation;
    private boolean accessible;
    private int distanceToEntrance;
    private int distanceToExit;

    public ParkingSlot() {
        this.status = SlotStatus.AVAILABLE;
        this.vehicleType = VehicleType.CAR;
    }

    public ParkingSlot(int id, String slotNumber, String floor, String zone, VehicleType vehicleType,
                       SlotStatus status, boolean nearEntrance, boolean hasChargingStation) {
        this(id, slotNumber, floor, zone, vehicleType, status, nearEntrance, false, hasChargingStation, false, 20, 25);
    }

    public ParkingSlot(int id, String slotNumber, String floor, String zone, VehicleType vehicleType,
                       SlotStatus status, boolean nearEntrance, boolean nearExit, boolean hasChargingStation,
                       boolean accessible, int distanceToEntrance, int distanceToExit) {
        this.id = id;
        this.slotNumber = slotNumber;
        this.floor = floor;
        this.zone = zone;
        this.vehicleType = vehicleType;
        this.status = status;
        this.nearEntrance = nearEntrance;
        this.nearExit = nearExit;
        this.hasChargingStation = hasChargingStation;
        this.accessible = accessible;
        this.distanceToEntrance = distanceToEntrance;
        this.distanceToExit = distanceToExit;
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

    public boolean isNearExit() { return nearExit; }
    public void setNearExit(boolean nearExit) { this.nearExit = nearExit; }

    public boolean hasChargingStation() { return hasChargingStation; }
    public void setHasChargingStation(boolean hasChargingStation) { this.hasChargingStation = hasChargingStation; }

    public boolean isAccessible() { return accessible; }
    public void setAccessible(boolean accessible) { this.accessible = accessible; }

    public int getDistanceToEntrance() { return distanceToEntrance; }
    public void setDistanceToEntrance(int distanceToEntrance) { this.distanceToEntrance = distanceToEntrance; }

    public int getDistanceToExit() { return distanceToExit; }
    public void setDistanceToExit(int distanceToExit) { this.distanceToExit = distanceToExit; }

    public boolean isAvailable() {
        return this.status == SlotStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return slotNumber + " [" + floor + " - " + zone + " (" + (vehicleType != null ? vehicleType.name() : "") + ")]";
    }
}