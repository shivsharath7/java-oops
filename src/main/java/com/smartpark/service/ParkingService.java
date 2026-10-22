package com.smartpark.service;

import com.smartpark.dao.AuditDAO;
import com.smartpark.dao.ParkingRecordDAO;
import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.dao.TariffDAO;
import com.smartpark.dao.VehicleDAO;
import com.smartpark.enums.PaymentStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.AuditEvent;
import com.smartpark.model.ParkingRecord;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.ParkingTariff;
import com.smartpark.model.Vehicle;
import com.smartpark.strategy.AllocationDecision;
import com.smartpark.util.DatabaseConnection;
import com.smartpark.util.ParkingFeeCalculator;
import com.smartpark.util.ParkingFeeCalculator.FeeBreakdown;
import com.smartpark.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Core ParkingService coordinating entry, exit, payment confirmation, and auditing.
 */
public class ParkingService {

    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();
    private final TariffDAO tariffDAO = new TariffDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    private final SlotAllocationService allocationService = new SlotAllocationService();

    public static class EntryResult {
        private final boolean success;
        private final String message;
        private final ParkingRecord record;
        private final ParkingSlot slot;
        private final AllocationDecision decision;

        public EntryResult(boolean success, String message, ParkingRecord record, ParkingSlot slot, AllocationDecision decision) {
            this.success = success;
            this.message = message;
            this.record = record;
            this.slot = slot;
            this.decision = decision;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public ParkingRecord getRecord() { return record; }
        public ParkingSlot getSlot() { return slot; }
        public AllocationDecision getDecision() { return decision; }
    }

    public static class ExitDetails {
        private final ParkingRecord record;
        private final FeeBreakdown breakdown;
        private final LocalDateTime exitTime;
        private final ParkingSlot slot;

        public ExitDetails(ParkingRecord record, FeeBreakdown breakdown, LocalDateTime exitTime, ParkingSlot slot) {
            this.record = record;
            this.breakdown = breakdown;
            this.exitTime = exitTime;
            this.slot = slot;
        }

        public ParkingRecord getRecord() { return record; }
        public FeeBreakdown getBreakdown() { return breakdown; }
        public LocalDateTime getExitTime() { return exitTime; }
        public ParkingSlot getSlot() { return slot; }
    }

    /**
     * Executes allocation preview before final confirmation.
     */
    public AllocationDecision previewAllocation(Vehicle vehicle, String preferredExit) {
        return allocationService.allocateSlot(vehicle, preferredExit);
    }

    /**
     * Complete entry workflow with input validation, duplicate check, and atomic slot claim.
     */
    public EntryResult parkVehicle(Vehicle vehicle, Integer designatedSlotId, String operatorName, String notes) {
        // 1. Validate vehicle registration plate
        String plate = ValidationUtil.cleanPlate(vehicle.getVehicleNumber());
        if (!ValidationUtil.isValidVehicleNumber(plate)) {
            return new EntryResult(false, "Invalid registration number '" + vehicle.getVehicleNumber() +
                    "'. Must be 4-12 alphanumeric characters.", null, null, null);
        }
        vehicle.setVehicleNumber(plate);

        // 2. Prevent duplicate active parking sessions for same vehicle
        ParkingRecord activeSession = recordDAO.findActiveRecordByVehicleNumber(plate);
        if (activeSession != null) {
            return new EntryResult(false, "Vehicle " + plate + " already has an ACTIVE parking session in Slot " +
                    activeSession.getSlotNumber() + " (Ticket: " + activeSession.getTicketId() + ").", null, null, null);
        }

        // 3. Determine slot
        ParkingSlot targetSlot;
        AllocationDecision decision = null;
        if (designatedSlotId != null && designatedSlotId > 0) {
            targetSlot = slotDAO.getSlotById(designatedSlotId);
            if (targetSlot == null || targetSlot.getStatus() != SlotStatus.AVAILABLE) {
                return new EntryResult(false, "Selected slot is no longer available.", null, null, null);
            }
        } else {
            decision = allocationService.allocateSlot(vehicle, null);
            if (!decision.hasRecommendation()) {
                return new EntryResult(false, decision.getExplanation(), null, null, decision);
            }
            targetSlot = decision.getRecommendedSlot();
        }

        // 4. Save or retrieve vehicle entity
        int vehicleId = vehicleDAO.saveOrGetVehicle(vehicle);
        if (vehicleId <= 0) {
            return new EntryResult(false, "Database error saving vehicle records.", null, null, null);
        }

        // 5. Generate unique ticket
        String ticketId = "SP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmmss")) +
                          "-" + ((int)(Math.random() * 900) + 100);

        ParkingRecord record = new ParkingRecord();
        record.setTicketId(ticketId);
        record.setVehicleId(vehicleId);
        record.setSlotId(targetSlot.getId());
        record.setVehicleNumber(plate);
        record.setVehicleType(vehicle.getVehicleType());
        record.setSlotNumber(targetSlot.getSlotNumber());
        record.setFloor(targetSlot.getFloor());
        record.setZone(targetSlot.getZone());
        record.setEntryTime(LocalDateTime.now());
        record.setPaymentStatus(PaymentStatus.PENDING);
        record.setStatus("ACTIVE");
        record.setEntryOperator(operatorName != null ? operatorName : "System");
        record.setNotes(notes);

        // 6. Atomic transaction for slot state update and record creation
        SlotStatus newStatus = targetSlot.hasChargingStation() && vehicle.isRequiresCharging()
                ? SlotStatus.EV_CHARGING : SlotStatus.OCCUPIED;

        boolean slotClaimed = slotDAO.atomicClaimSlot(targetSlot.getId(), SlotStatus.AVAILABLE, newStatus);
        if (!slotClaimed) {
            // Check if slot was reserved for this vehicle
            slotClaimed = slotDAO.atomicClaimSlot(targetSlot.getId(), SlotStatus.RESERVED, newStatus);
            if (!slotClaimed) {
                return new EntryResult(false, "Slot " + targetSlot.getSlotNumber() + " was claimed by another operation. Please re-allocate.", null, null, null);
            }
        }

        boolean recordCreated = recordDAO.createRecord(record);
        if (!recordCreated) {
            // Rollback slot status
            slotDAO.updateStatus(targetSlot.getId(), SlotStatus.AVAILABLE);
            return new EntryResult(false, "Database error creating parking record. Slot status rolled back.", null, null, null);
        }

        targetSlot.setStatus(newStatus);

        // 7. Audit log event
        auditDAO.logEvent(new AuditEvent("ENTRY", ticketId,
                "Vehicle " + plate + " (" + vehicle.getVehicleType().getDisplayName() + ") entered. Assigned slot " +
                targetSlot.getSlotNumber() + " on " + targetSlot.getFloor(), operatorName));

        return new EntryResult(true, "Vehicle successfully parked at " + targetSlot.getSlotNumber(), record, targetSlot, decision);
    }

    /**
     * Exit calculation and fee breakdown retrieval.
     */
    public ExitDetails calculateExitDetails(String searchTicketOrPlate) {
        ParkingRecord record = recordDAO.findRecordByTicketOrPlate(searchTicketOrPlate);
        if (record == null || !record.isActive()) return null;

        ParkingSlot slot = slotDAO.getSlotById(record.getSlotId());
        Vehicle vehicle = vehicleDAO.findByNumber(record.getVehicleNumber());
        VehicleType vType = (vehicle != null) ? vehicle.getVehicleType() : VehicleType.CAR;

        ParkingTariff tariff = tariffDAO.getTariffForType(vType);
        boolean isEvCharging = (slot != null && slot.getStatus() == SlotStatus.EV_CHARGING);

        LocalDateTime now = LocalDateTime.now();
        FeeBreakdown breakdown = ParkingFeeCalculator.calculate(record.getEntryTime(), now, vType, tariff, isEvCharging);

        return new ExitDetails(record, breakdown, now, slot);
    }

    /**
     * Completes exit payment confirmation and atomically frees parking slot.
     */
    public boolean processExitPayment(ParkingRecord record, LocalDateTime exitTime, int durationMinutes,
                                      double fee, String paymentMethod, String operatorName) {
        boolean completed = recordDAO.completeExit(record.getId(), exitTime, durationMinutes, fee, paymentMethod, operatorName);
        if (completed) {
            // Free slot
            slotDAO.updateStatus(record.getSlotId(), SlotStatus.AVAILABLE);

            // Audit
            auditDAO.logEvent(new AuditEvent("EXIT", record.getTicketId(),
                    "Vehicle " + record.getVehicleNumber() + " exited from slot " + record.getSlotNumber() +
                    ". Duration: " + durationMinutes + " mins. Fee: ₹" + String.format("%.2f", fee) + " via " + paymentMethod, operatorName));
            return true;
        }
        return false;
    }

    public SlotAllocationService getAllocationService() {
        return allocationService;
    }
}