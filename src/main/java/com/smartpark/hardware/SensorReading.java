package com.smartpark.hardware;

import java.time.LocalDateTime;

/**
 * Encapsulates an ultrasonic distance reading from an IoT or simulated sensor.
 */
public class SensorReading {
    private final String sensorId;
    private final String slotNumber;
    private final double distanceCm;
    private final boolean occupied;
    private final LocalDateTime timestamp;
    private final String status; // VALID, STALE, ERROR

    public SensorReading(String sensorId, String slotNumber, double distanceCm, boolean occupied,
                         LocalDateTime timestamp, String status) {
        this.sensorId = sensorId;
        this.slotNumber = slotNumber;
        this.distanceCm = distanceCm;
        this.occupied = occupied;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.status = status;
    }

    public String getSensorId() { return sensorId; }
    public String getSlotNumber() { return slotNumber; }
    public double getDistanceCm() { return distanceCm; }
    public boolean isOccupied() { return occupied; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return "[" + sensorId + " -> " + slotNumber + "] " + distanceCm + " cm (" +
               (occupied ? "OCCUPIED" : "AVAILABLE") + ") - " + status;
    }
}
