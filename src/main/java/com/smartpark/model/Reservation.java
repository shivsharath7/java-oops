package com.smartpark.model;

import com.smartpark.enums.ReservationStatus;
import com.smartpark.enums.VehicleType;
import java.time.LocalDateTime;

public class Reservation {
    private int id;
    private String reservationCode;
    private String vehicleNumber;
    private VehicleType vehicleType;
    private int slotId;
    private String slotNumber;
    private String floor;
    private String zone;
    private String customerName;
    private String contactNumber;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private ReservationStatus status;

    public Reservation() {
        this.status = ReservationStatus.PENDING;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReservationCode() { return reservationCode; }
    public void setReservationCode(String reservationCode) { this.reservationCode = reservationCode; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public LocalDateTime getReservedAt() { return reservedAt; }
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
