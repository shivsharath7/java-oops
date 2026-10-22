package com.smartpark.service;

import com.smartpark.dao.AuditDAO;
import com.smartpark.dao.ChargingDAO;
import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.enums.ChargerStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.model.AuditEvent;
import com.smartpark.model.ChargingSession;
import com.smartpark.model.EvCharger;
import com.smartpark.model.ParkingSlot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing EV charging bays, charging sessions, and overstay alerts.
 */
public class ChargingService {

    private final ChargingDAO chargingDAO = new ChargingDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    public static final double COST_PER_KWH = 7.50; // ₹7.50 per kWh

    public List<EvCharger> getAllChargers() {
        return chargingDAO.getAllChargers();
    }

    public List<ChargingSession> getActiveSessions() {
        return chargingDAO.getAllActiveSessions();
    }

    public List<ChargingSession> getRecentSessions(int limit) {
        return chargingDAO.getRecentSessions(limit);
    }

    public boolean startCharging(int chargerId, String vehicleNumber, int durationMinutes, String operator) {
        EvCharger charger = chargingDAO.getAllChargers().stream()
                .filter(c -> c.getId() == chargerId).findFirst().orElse(null);
        if (charger == null || charger.getStatus() != ChargerStatus.AVAILABLE) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime estEnd = now.plusMinutes(durationMinutes);

        // Estimate energy based on power rating and duration
        double hours = durationMinutes / 60.0;
        double estimatedKwh = Math.round((charger.getPowerKw() * hours * 0.85) * 10.0) / 10.0;
        double estFee = Math.round(estimatedKwh * COST_PER_KWH * 100.0) / 100.0;

        ChargingSession session = new ChargingSession();
        session.setChargerId(chargerId);
        session.setVehicleNumber(vehicleNumber.trim().toUpperCase());
        session.setSlotId(charger.getSlotId());
        session.setStartTime(now);
        session.setEstimatedEndTime(estEnd);
        session.setKwhConsumed(estimatedKwh);
        session.setStatus("IN_PROGRESS");
        session.setChargingFee(estFee);

        boolean started = chargingDAO.startChargingSession(session);
        if (started) {
            // Update slot status to EV_CHARGING
            slotDAO.updateStatus(charger.getSlotId(), SlotStatus.EV_CHARGING);

            auditDAO.logEvent(new AuditEvent("EV_CHARGING_START", charger.getChargerCode(),
                    "Charging started for vehicle " + vehicleNumber + " on bay " + charger.getChargerCode() +
                    " (" + charger.getPowerKw() + "kW). Duration: " + durationMinutes + " mins.", operator));
            return true;
        }
        return false;
    }

    public boolean stopCharging(int sessionId, String operator) {
        List<ChargingSession> active = chargingDAO.getAllActiveSessions();
        ChargingSession session = active.stream().filter(s -> s.getId() == sessionId).findFirst().orElse(null);
        if (session == null) return false;

        LocalDateTime now = LocalDateTime.now();
        Duration dur = Duration.between(session.getStartTime(), now);
        double actualHours = Math.max(0.1, dur.toMinutes() / 60.0);

        EvCharger charger = chargingDAO.getAllChargers().stream()
                .filter(c -> c.getId() == session.getChargerId()).findFirst().orElse(null);
        double power = (charger != null) ? charger.getPowerKw() : 50.0;
        double actualKwh = Math.round((power * actualHours * 0.85) * 10.0) / 10.0;
        double finalFee = Math.round(actualKwh * COST_PER_KWH * 100.0) / 100.0;

        boolean completed = chargingDAO.completeChargingSession(sessionId, now, actualKwh, finalFee);
        if (completed) {
            chargingDAO.updateChargerStatus(session.getChargerId(), ChargerStatus.AVAILABLE);

            // Revert slot status to OCCUPIED if vehicle is still parked
            slotDAO.updateStatus(session.getSlotId(), SlotStatus.OCCUPIED);

            auditDAO.logEvent(new AuditEvent("EV_CHARGING_STOP", session.getChargerCode(),
                    "Charging completed for " + session.getVehicleNumber() + ". Delivered " +
                    actualKwh + " kWh. Fee: ₹" + finalFee, operator));
            return true;
        }
        return false;
    }

    public boolean updateChargerMaintenance(int chargerId, boolean underMaintenance) {
        ChargerStatus status = underMaintenance ? ChargerStatus.MAINTENANCE : ChargerStatus.AVAILABLE;
        return chargingDAO.updateChargerStatus(chargerId, status);
    }
}
