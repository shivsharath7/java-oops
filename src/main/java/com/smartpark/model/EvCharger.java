package com.smartpark.model;

import com.smartpark.enums.ChargerStatus;

public class EvCharger {
    private int id;
    private String chargerCode;
    private int slotId;
    private String slotNumber;
    private String chargerType;
    private double powerKw;
    private ChargerStatus status;

    public EvCharger() {
        this.status = ChargerStatus.AVAILABLE;
    }

    public EvCharger(int id, String chargerCode, int slotId, String slotNumber, String chargerType, double powerKw, ChargerStatus status) {
        this.id = id;
        this.chargerCode = chargerCode;
        this.slotId = slotId;
        this.slotNumber = slotNumber;
        this.chargerType = chargerType;
        this.powerKw = powerKw;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getChargerCode() { return chargerCode; }
    public void setChargerCode(String chargerCode) { this.chargerCode = chargerCode; }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public String getChargerType() { return chargerType; }
    public void setChargerType(String chargerType) { this.chargerType = chargerType; }

    public double getPowerKw() { return powerKw; }
    public void setPowerKw(double powerKw) { this.powerKw = powerKw; }

    public ChargerStatus getStatus() { return status; }
    public void setStatus(ChargerStatus status) { this.status = status; }

    @Override
    public String toString() {
        return chargerCode + (slotNumber != null && !slotNumber.isBlank() ? " [Slot " + slotNumber + "]" : "") + " (" + powerKw + " kW)";
    }
}
