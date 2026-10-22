package com.smartpark.model;

import com.smartpark.enums.PaymentStatus;
import com.smartpark.enums.VehicleType;
import java.time.LocalDateTime;

/**
 * Model representing a Parking Session / Record.
 */
public class ParkingRecord {
    private int id;
    private String ticketId;
    private int vehicleId;
    private int slotId;
    private String vehicleNumber;
    private VehicleType vehicleType;
    private String slotNumber;
    private String floor;
    private String zone;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private int durationMinutes;
    private double parkingFee;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private String status; // ACTIVE, COMPLETED, DISPUTED
    private String entryOperator;
    private String exitOperator;
    private String notes;

    public ParkingRecord() {
        this.paymentStatus = PaymentStatus.PENDING;
        this.paymentMethod = "NONE";
        this.status = "ACTIVE";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getParkingFee() { return parkingFee; }
    public void setParkingFee(double parkingFee) { this.parkingFee = parkingFee; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEntryOperator() { return entryOperator; }
    public void setEntryOperator(String entryOperator) { this.entryOperator = entryOperator; }

    public String getExitOperator() { return exitOperator; }
    public void setExitOperator(String exitOperator) { this.exitOperator = exitOperator; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }
}