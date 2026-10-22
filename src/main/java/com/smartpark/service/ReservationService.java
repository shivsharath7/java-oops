package com.smartpark.service;

import com.smartpark.dao.AuditDAO;
import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.dao.ReservationDAO;
import com.smartpark.dao.SettingsDAO;
import com.smartpark.enums.ReservationStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.AuditEvent;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Reservation;
import com.smartpark.util.ValidationUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service managing temporary slot holds and automated expiry cleanup.
 */
public class ReservationService {

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    private ScheduledExecutorService scheduler;

    public ReservationService() {
        startBackgroundExpiryCleaner();
    }

    public static class ReservationResult {
        private final boolean success;
        private final String message;
        private final Reservation reservation;

        public ReservationResult(boolean success, String message, Reservation reservation) {
            this.success = success;
            this.message = message;
            this.reservation = reservation;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Reservation getReservation() { return reservation; }
    }

    public ReservationResult createHold(String vehicleNumber, VehicleType vehicleType, int slotId,
                                        String customerName, String contactNumber, int holdMinutes) {
        String plate = ValidationUtil.cleanPlate(vehicleNumber);
        if (!ValidationUtil.isValidVehicleNumber(plate)) {
            return new ReservationResult(false, "Invalid registration number for reservation.", null);
        }

        // Check if vehicle already has an active reservation
        Reservation existing = reservationDAO.findActiveByVehicleNumber(plate);
        if (existing != null) {
            return new ReservationResult(false, "Vehicle " + plate + " already has active hold code " +
                    existing.getReservationCode() + " on slot " + existing.getSlotNumber(), null);
        }

        // Check slot availability and atomically claim it
        ParkingSlot slot = slotDAO.getSlotById(slotId);
        if (slot == null || slot.getStatus() != SlotStatus.AVAILABLE) {
            return new ReservationResult(false, "Slot is not currently available for reservation.", null);
        }

        boolean claimed = slotDAO.atomicClaimSlot(slotId, SlotStatus.AVAILABLE, SlotStatus.RESERVED);
        if (!claimed) {
            return new ReservationResult(false, "Slot was claimed by a concurrent operation. Please choose another.", null);
        }

        if (holdMinutes <= 0) {
            String defaultHoldStr = settingsDAO.getSetting("reservation_hold_minutes", "15");
            try {
                holdMinutes = Integer.parseInt(defaultHoldStr);
            } catch (Exception e) {
                holdMinutes = 15;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(holdMinutes);
        String code = "RES-" + now.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + ((int)(Math.random() * 9000) + 1000);

        Reservation r = new Reservation();
        r.setReservationCode(code);
        r.setVehicleNumber(plate);
        r.setVehicleType(vehicleType != null ? vehicleType : VehicleType.CAR);
        r.setSlotId(slotId);
        r.setSlotNumber(slot.getSlotNumber());
        r.setFloor(slot.getFloor());
        r.setZone(slot.getZone());
        r.setCustomerName(customerName);
        r.setContactNumber(contactNumber);
        r.setReservedAt(now);
        r.setExpiresAt(expiresAt);
        r.setStatus(ReservationStatus.PENDING);

        boolean created = reservationDAO.createReservation(r);
        if (!created) {
            // Rollback slot
            slotDAO.updateStatus(slotId, SlotStatus.AVAILABLE);
            return new ReservationResult(false, "Database error saving reservation.", null);
        }

        auditDAO.logEvent(new AuditEvent("RESERVATION_CREATED", code,
                "Temporary hold on slot " + slot.getSlotNumber() + " created for " + plate +
                ". Expires at: " + expiresAt.format(DateTimeFormatter.ofPattern("HH:mm:ss")), "System"));

        return new ReservationResult(true, "Slot " + slot.getSlotNumber() + " reserved until " +
                expiresAt.format(DateTimeFormatter.ofPattern("HH:mm")), r);
    }

    public boolean cancelReservation(int reservationId, String actor) {
        Reservation r = reservationDAO.getAllReservations().stream()
                .filter(res -> res.getId() == reservationId)
                .findFirst().orElse(null);

        if (r != null && r.getStatus() == ReservationStatus.PENDING) {
            boolean updated = reservationDAO.updateStatus(reservationId, ReservationStatus.CANCELLED);
            if (updated) {
                slotDAO.updateStatus(r.getSlotId(), SlotStatus.AVAILABLE);
                auditDAO.logEvent(new AuditEvent("RESERVATION_CANCELLED", r.getReservationCode(),
                        "Reservation for " + r.getVehicleNumber() + " cancelled. Slot " + r.getSlotNumber() + " released.", actor));
                return true;
            }
        }
        return false;
    }

    /**
     * Scans for expired reservations and automatically releases the held slots.
     */
    public int releaseExpiredReservations() {
        List<Reservation> expired = reservationDAO.getExpiredActiveReservations();
        int count = 0;
        for (Reservation r : expired) {
            boolean marked = reservationDAO.updateStatus(r.getId(), ReservationStatus.EXPIRED);
            if (marked) {
                // Free slot back to AVAILABLE
                slotDAO.updateStatus(r.getSlotId(), SlotStatus.AVAILABLE);
                auditDAO.logEvent(new AuditEvent("RESERVATION_EXPIRED", r.getReservationCode(),
                        "Hold expired for vehicle " + r.getVehicleNumber() + ". Slot " + r.getSlotNumber() + " automatically released.", "AutoCleaner"));
                count++;
            }
        }
        return count;
    }

    public List<Reservation> getAllReservations() {
        // Run expiry check to ensure fresh status
        releaseExpiredReservations();
        return reservationDAO.getAllReservations();
    }

    private void startBackgroundExpiryCleaner() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SmartPark-ReservationCleaner");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::releaseExpiredReservations, 30, 30, TimeUnit.SECONDS);
    }

    public void stopCleaner() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
