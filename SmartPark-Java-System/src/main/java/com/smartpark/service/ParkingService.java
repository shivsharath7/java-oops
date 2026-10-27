package com.smartpark.service;

import com.smartpark.dao.ParkingRecordDAO;
import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.dao.VehicleDAO;
import com.smartpark.enums.PaymentStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingRecord;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Vehicle;
import com.smartpark.util.ParkingFeeCalculator;
import com.smartpark.util.ParkingFeeCalculator.FeeBreakdown;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ParkingService {
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();
    private final SlotAllocationService allocationService = new SlotAllocationService();

    public static class EntryResponse {
        public boolean success;
        public String message;
        public ParkingRecord record;
        public ParkingSlot slot;

        public EntryResponse(boolean success, String message, ParkingRecord record, ParkingSlot slot) {
            this.success = success;
            this.message = message;
            this.record = record;
            this.slot = slot;
        }
    }

    public EntryResponse parkVehicle(String plate, VehicleType type, String customerName, String phone) {
        // 1. Check if vehicle is already parked actively
        ParkingRecord active = recordDAO.findActiveRecordByVehicleNumber(plate);
        if (active != null) {
            return new EntryResponse(false, "Vehicle " + plate + " already has an active parking record at Slot " + active.getSlotNumber(), null, null);
        }

        // 2. Find best available slot
        SlotAllocationService.AllocationResult alloc = allocationService.findBestSlot(type);
        if (alloc.getRecommendedSlot() == null) {
            return new EntryResponse(false, "Parking Full! No available slots for " + type.getDisplayName(), null, null);
        }

        ParkingSlot slot = alloc.getRecommendedSlot();

        // 3. Save or get vehicle
        Vehicle vehicle = new Vehicle(plate, type, customerName, phone);
        int vehicleId = vehicleDAO.saveOrGetVehicle(vehicle);

        // 4. Create parking record
        String ticketId = "SP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmmss"));
        ParkingRecord record = new ParkingRecord();
        record.setTicketId(ticketId);
        record.setVehicleId(vehicleId);
        record.setSlotId(slot.getId());
        record.setVehicleNumber(plate);
        record.setSlotNumber(slot.getSlotNumber());
        record.setFloor(slot.getFloor());
        record.setZone(slot.getZone());
        record.setEntryTime(LocalDateTime.now());
        record.setPaymentStatus(PaymentStatus.PENDING);
        record.setStatus("ACTIVE");

        boolean created = recordDAO.createRecord(record);
        if (!created) {
            return new EntryResponse(false, "Database error creating parking record.", null, null);
        }

        // 5. Flip slot status to OCCUPIED
        slotDAO.updateStatus(slot.getId(), SlotStatus.OCCUPIED);

        return new EntryResponse(true, "Vehicle Successfully Parked at " + slot.getSlotNumber(), record, slot);
    }

    public static class ExitCalculation {
        public ParkingRecord record;
        public FeeBreakdown breakdown;
        public LocalDateTime exitTime;
    }

    public ExitCalculation calculateExit(String plate, VehicleType type) {
        ParkingRecord record = recordDAO.findActiveRecordByVehicleNumber(plate);
        if (record == null) return null;

        LocalDateTime now = LocalDateTime.now();
        FeeBreakdown fee = ParkingFeeCalculator.calculate(record.getEntryTime(), now, type);

        ExitCalculation calc = new ExitCalculation();
        calc.record = record;
        calc.breakdown = fee;
        calc.exitTime = now;
        return calc;
    }

    public boolean processExitPayment(ParkingRecord record, LocalDateTime exitTime, int durationMinutes, double fee) {
        // Mark record complete
        boolean updated = recordDAO.completeExit(record.getId(), exitTime, durationMinutes, fee);
        if (updated) {
            // Free slot
            slotDAO.updateStatus(record.getSlotId(), SlotStatus.AVAILABLE);
            return true;
        }
        return false;
    }
}