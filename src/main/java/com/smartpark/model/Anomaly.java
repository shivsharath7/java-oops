package com.smartpark.model;

import com.smartpark.enums.AnomalySeverity;
import java.time.LocalDateTime;

public class Anomaly {
    private int id;
    private String anomalyType;
    private AnomalySeverity severity;
    private String vehicleNumber;
    private Integer slotId;
    private String slotNumber;
    private String description;
    private String suggestedResolution;
    private String status; // OPEN, ACKNOWLEDGED, RESOLVED, IGNORED
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;

    public Anomaly() {
        this.status = "OPEN";
        this.severity = AnomalySeverity.WARNING;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }

    public AnomalySeverity getSeverity() { return severity; }
    public void setSeverity(AnomalySeverity severity) { this.severity = severity; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public Integer getSlotId() { return slotId; }
    public void setSlotId(Integer slotId) { this.slotId = slotId; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSuggestedResolution() { return suggestedResolution; }
    public void setSuggestedResolution(String suggestedResolution) { this.suggestedResolution = suggestedResolution; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
