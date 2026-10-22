package com.smartpark.model;

import java.time.LocalDateTime;

public class ChargingSession {
    private int id;
    private int chargerId;
    private String chargerCode;
    private String vehicleNumber;
    private int slotId;
    private String slotNumber;
    private LocalDateTime startTime;
    private LocalDateTime estimatedEndTime;
    private LocalDateTime actualEndTime;
    private double kwhConsumed;
    private String status; // IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED
    private double chargingFee;

    public ChargingSession() {
        this.status = "IN_PROGRESS";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChargerId() { return chargerId; }
    public void setChargerId(int chargerId) { this.chargerId = chargerId; }

    public String getChargerCode() { return chargerCode; }
    public void setChargerCode(String chargerCode) { this.chargerCode = chargerCode; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEstimatedEndTime() { return estimatedEndTime; }
    public void setEstimatedEndTime(LocalDateTime estimatedEndTime) { this.estimatedEndTime = estimatedEndTime; }

    public LocalDateTime getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(LocalDateTime actualEndTime) { this.actualEndTime = actualEndTime; }

    public double getKwhConsumed() { return kwhConsumed; }
    public void setKwhConsumed(double kwhConsumed) { this.kwhConsumed = kwhConsumed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getChargingFee() { return chargingFee; }
    public void setChargingFee(double chargingFee) { this.chargingFee = chargingFee; }

    public boolean isOverdue() {
        return "IN_PROGRESS".equals(status) && LocalDateTime.now().isAfter(estimatedEndTime);
    }
}
