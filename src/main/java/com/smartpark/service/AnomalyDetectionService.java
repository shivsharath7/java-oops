package com.smartpark.service;

import com.smartpark.dao.*;
import com.smartpark.enums.AnomalySeverity;
import com.smartpark.enums.PaymentStatus;
import com.smartpark.enums.ReservationStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service performing deep consistency scans across actual persisted database records.
 * Identifies 8 key structural and operational anomalies in real-time.
 */
public class AnomalyDetectionService {

    private final AnomalyDAO anomalyDAO = new AnomalyDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final ChargingDAO chargingDAO = new ChargingDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    public List<Anomaly> getAllAnomalies() {
        return anomalyDAO.getAllAnomalies();
    }

    public List<Anomaly> getOpenAnomalies() {
        return anomalyDAO.getOpenAnomalies();
    }

    /**
     * Executes full comprehensive consistency scan across MySQL records.
     * Returns count of new anomalies discovered.
     */
    public synchronized int runConsistencyScan() {
        int detectedCount = 0;
        List<Anomaly> existingOpen = anomalyDAO.getOpenAnomalies();
        Set<String> existingSignatures = new HashSet<>();
        for (Anomaly a : existingOpen) {
            existingSignatures.add(a.getAnomalyType() + ":" + a.getVehicleNumber() + ":" + a.getSlotId());
        }

        List<ParkingSlot> allSlots = slotDAO.getAllSlots();
        List<ParkingRecord> activeRecords = recordDAO.getAllActiveRecords();
        List<Reservation> allReservations = reservationDAO.getAllReservations();
        List<ChargingSession> activeCharging = chargingDAO.getAllActiveSessions();

        Map<Integer, ParkingSlot> slotMap = new HashMap<>();
        for (ParkingSlot s : allSlots) slotMap.put(s.getId(), s);

        Map<Integer, List<ParkingRecord>> slotToActiveRecords = new HashMap<>();
        Map<String, List<ParkingRecord>> vehicleToActiveRecords = new HashMap<>();

        for (ParkingRecord r : activeRecords) {
            slotToActiveRecords.computeIfAbsent(r.getSlotId(), k -> new ArrayList<>()).add(r);
            vehicleToActiveRecords.computeIfAbsent(r.getVehicleNumber(), k -> new ArrayList<>()).add(r);
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Check for Duplicate Active Sessions for one vehicle
        for (Map.Entry<String, List<ParkingRecord>> entry : vehicleToActiveRecords.entrySet()) {
            if (entry.getValue().size() > 1) {
                String sig = "DUPLICATE_ACTIVE_SESSION:" + entry.getKey() + ":null";
                if (!existingSignatures.contains(sig)) {
                    Anomaly a = new Anomaly();
                    a.setAnomalyType("DUPLICATE_ACTIVE_SESSION");
                    a.setSeverity(AnomalySeverity.CRITICAL);
                    a.setVehicleNumber(entry.getKey());
                    a.setDescription("Vehicle " + entry.getKey() + " has " + entry.getValue().size() + " active parking records concurrently.");
                    a.setSuggestedResolution("Inspect vehicle gate logs and mark the duplicate/stale session as COMPLETED or DISPUTED.");
                    a.setStatus("OPEN");
                    a.setDetectedAt(now);
                    if (anomalyDAO.createAnomaly(a)) detectedCount++;
                }
            }
        }

        // 2. Check for Slot Marked OCCUPIED Without an Active Parking Record
        for (ParkingSlot s : allSlots) {
            if (s.getStatus() == SlotStatus.OCCUPIED || s.getStatus() == SlotStatus.EV_CHARGING) {
                List<ParkingRecord> recs = slotToActiveRecords.get(s.getId());
                if (recs == null || recs.isEmpty()) {
                    String sig = "SLOT_OCCUPIED_NO_RECORD:null:" + s.getId();
                    if (!existingSignatures.contains(sig)) {
                        Anomaly a = new Anomaly();
                        a.setAnomalyType("SLOT_OCCUPIED_NO_RECORD");
                        a.setSeverity(AnomalySeverity.WARNING);
                        a.setSlotId(s.getId());
                        a.setDescription("Slot " + s.getSlotNumber() + " (" + s.getFloor() + ") is marked " + s.getStatus().name() + " but has no active parking record.");
                        a.setSuggestedResolution("Verify physical occupancy. If empty, reset slot status to AVAILABLE. If occupied, generate missing entry ticket.");
                        a.setStatus("OPEN");
                        a.setDetectedAt(now);
                        if (anomalyDAO.createAnomaly(a)) detectedCount++;
                    }
                }
            }
        }

        // 3. Check for Active Parking Record Associated With Slot Marked AVAILABLE
        for (ParkingRecord r : activeRecords) {
            ParkingSlot s = slotMap.get(r.getSlotId());
            if (s != null && s.getStatus() == SlotStatus.AVAILABLE) {
                String sig = "RECORD_ACTIVE_SLOT_AVAILABLE:" + r.getVehicleNumber() + ":" + s.getId();
                if (!existingSignatures.contains(sig)) {
                    Anomaly a = new Anomaly();
                    a.setAnomalyType("RECORD_ACTIVE_SLOT_AVAILABLE");
                    a.setSeverity(AnomalySeverity.CRITICAL);
                    a.setVehicleNumber(r.getVehicleNumber());
                    a.setSlotId(s.getId());
                    a.setDescription("Active parking ticket " + r.getTicketId() + " for vehicle " + r.getVehicleNumber() +
                            " points to slot " + s.getSlotNumber() + ", but the slot is flagged as AVAILABLE.");
                    a.setSuggestedResolution("Lock slot " + s.getSlotNumber() + " to OCCUPIED or complete checkout for vehicle " + r.getVehicleNumber() + ".");
                    a.setStatus("OPEN");
                    a.setDetectedAt(now);
                    if (anomalyDAO.createAnomaly(a)) detectedCount++;
                }
            }
        }

        // 4. Check for Reservation That Has Expired But Remains Active
        for (Reservation res : allReservations) {
            if (res.getStatus() == ReservationStatus.PENDING && res.getExpiresAt().isBefore(now)) {
                String sig = "EXPIRED_RESERVATION_ACTIVE:" + res.getVehicleNumber() + ":" + res.getSlotId();
                if (!existingSignatures.contains(sig)) {
                    Anomaly a = new Anomaly();
                    a.setAnomalyType("EXPIRED_RESERVATION_ACTIVE");
                    a.setSeverity(AnomalySeverity.WARNING);
                    a.setVehicleNumber(res.getVehicleNumber());
                    a.setSlotId(res.getSlotId());
                    a.setDescription("Reservation " + res.getReservationCode() + " for slot " + res.getSlotNumber() +
                            " expired at " + res.getExpiresAt() + " but is still listed as PENDING.");
                    a.setSuggestedResolution("Trigger automated expiry cleanup to mark EXPIRED and release slot " + res.getSlotNumber() + ".");
                    a.setStatus("OPEN");
                    a.setDetectedAt(now);
                    if (anomalyDAO.createAnomaly(a)) detectedCount++;
                }
            }
        }

        // 5. Check for Unusually Long Parking Session
        int maxStayHours = 24;
        try {
            maxStayHours = Integer.parseInt(settingsDAO.getSetting("max_allowed_stay_hours", "24"));
        } catch (Exception ignored) {}

        for (ParkingRecord r : activeRecords) {
            long hoursParked = Duration.between(r.getEntryTime(), now).toHours();
            if (hoursParked >= maxStayHours) {
                String sig = "OVERSTAY_PARKING_RECORD:" + r.getVehicleNumber() + ":" + r.getSlotId();
                if (!existingSignatures.contains(sig)) {
                    Anomaly a = new Anomaly();
                    a.setAnomalyType("OVERSTAY_PARKING_RECORD");
                    a.setSeverity(AnomalySeverity.INFO);
                    a.setVehicleNumber(r.getVehicleNumber());
                    a.setSlotId(r.getSlotId());
                    a.setDescription("Vehicle " + r.getVehicleNumber() + " in slot " + r.getSlotNumber() +
                            " has been continuously parked for " + hoursParked + " hours (exceeds " + maxStayHours + "h threshold).");
                    a.setSuggestedResolution("Check security logs or contact driver at registered contact number.");
                    a.setStatus("OPEN");
                    a.setDetectedAt(now);
                    if (anomalyDAO.createAnomaly(a)) detectedCount++;
                }
            }
        }

        // 6. Check for Charging Bay Occupied Beyond Planned Completion
        for (ChargingSession cs : activeCharging) {
            if (now.isAfter(cs.getEstimatedEndTime())) {
                String sig = "CHARGING_BAY_OVERDUE:" + cs.getVehicleNumber() + ":" + cs.getSlotId();
                if (!existingSignatures.contains(sig)) {
                    long overstayMins = Duration.between(cs.getEstimatedEndTime(), now).toMinutes();
                    Anomaly a = new Anomaly();
                    a.setAnomalyType("CHARGING_BAY_OVERDUE");
                    a.setSeverity(AnomalySeverity.WARNING);
                    a.setVehicleNumber(cs.getVehicleNumber());
                    a.setSlotId(cs.getSlotId());
                    a.setDescription("Vehicle " + cs.getVehicleNumber() + " in EV bay " + cs.getChargerCode() +
                            " has completed scheduled charging time (" + overstayMins + " mins overdue).");
                    a.setSuggestedResolution("Notify EV owner to vacate bay or apply idle parking surcharges.");
                    a.setStatus("OPEN");
                    a.setDetectedAt(now);
                    if (anomalyDAO.createAnomaly(a)) detectedCount++;
                }
            }
        }

        // 7. Check for Invalid or Inconsistent Fee / Payment State
        List<ParkingRecord> completedRecords = recordDAO.getRecentRecords(100);
        for (ParkingRecord r : completedRecords) {
            if ("COMPLETED".equals(r.getStatus()) && r.getParkingFee() > 0 && r.getPaymentStatus() == PaymentStatus.PENDING) {
                String sig = "COMPLETED_SESSION_PAYMENT_PENDING:" + r.getVehicleNumber() + ":" + r.getSlotId();
                if (!existingSignatures.contains(sig)) {
                    Anomaly a = new Anomaly();
                    a.setAnomalyType("COMPLETED_SESSION_PAYMENT_PENDING");
                    a.setSeverity(AnomalySeverity.CRITICAL);
                    a.setVehicleNumber(r.getVehicleNumber());
                    a.setSlotId(r.getSlotId());
                    a.setDescription("Parking session " + r.getTicketId() + " is COMPLETED but payment status is PENDING (Fee: ₹" + r.getParkingFee() + ").");
                    a.setSuggestedResolution("Verify payment gateway receipt and update payment status to PAID or record dispute.");
                    a.setStatus("OPEN");
                    a.setDetectedAt(now);
                    if (anomalyDAO.createAnomaly(a)) detectedCount++;
                }
            }
        }

        // 8. Check for Conflicting Slot Assignment (multiple active records on same slot)
        for (Map.Entry<Integer, List<ParkingRecord>> entry : slotToActiveRecords.entrySet()) {
            if (entry.getValue().size() > 1) {
                String sig = "CONFLICTING_SLOT_ASSIGNMENT:null:" + entry.getKey();
                if (!existingSignatures.contains(sig)) {
                    ParkingSlot s = slotMap.get(entry.getKey());
                    String sNum = s != null ? s.getSlotNumber() : String.valueOf(entry.getKey());
                    Anomaly a = new Anomaly();
                    a.setAnomalyType("CONFLICTING_SLOT_ASSIGNMENT");
                    a.setSeverity(AnomalySeverity.CRITICAL);
                    a.setSlotId(entry.getKey());
                    a.setDescription("Multiple active records assigned to the same slot " + sNum + " (" + entry.getValue().size() + " active tickets).");
                    a.setSuggestedResolution("Reassign one vehicle to an alternative empty slot immediately.");
                    a.setStatus("OPEN");
                    a.setDetectedAt(now);
                    if (anomalyDAO.createAnomaly(a)) detectedCount++;
                }
            }
        }

        if (detectedCount > 0) {
            auditDAO.logEvent(new AuditEvent("ANOMALY_SCAN", "System",
                    "Consistency scan completed. Discovered " + detectedCount + " new anomaly alerts.", "AnomalyScanner"));
        }

        return detectedCount;
    }

    public boolean updateAnomalyStatus(int anomalyId, String newStatus, String actor, String notes) {
        boolean updated = anomalyDAO.updateStatus(anomalyId, newStatus, actor, notes);
        if (updated) {
            auditDAO.logEvent(new AuditEvent("ANOMALY_UPDATED", "Anomaly-" + anomalyId,
                    "Anomaly status updated to " + newStatus + ". Resolution: " + notes, actor));
        }
        return updated;
    }
}
