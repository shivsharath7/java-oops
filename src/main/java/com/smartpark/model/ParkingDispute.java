package com.smartpark.model;

import com.smartpark.enums.DisputeStatus;
import java.time.LocalDateTime;

public class ParkingDispute {
    private int id;
    private Integer recordId;
    private String ticketId;
    private String vehicleNumber;
    private String category;
    private String description;
    private double disputedFee;
    private DisputeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    private String resolvedBy;

    public ParkingDispute() {
        this.status = DisputeStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getDisputedFee() { return disputedFee; }
    public void setDisputedFee(double disputedFee) { this.disputedFee = disputedFee; }

    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
}
