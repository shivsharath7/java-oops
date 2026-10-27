package com.smartpark.service;

import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingSlot;

import java.util.List;

/**
 * SlotAllocationService performs rule-based smart parking allocation:
 * 1. Matches compatible vehicle type (CAR, BIKE, EV_CAR).
 * 2. Prioritizes Ground Floor entrance proximity.
 * 3. Returns primary recommendation + alternatives.
 */
public class SlotAllocationService {
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();

    public static class AllocationResult {
        private final ParkingSlot recommendedSlot;
        private final String reason;
        private final List<ParkingSlot> alternatives;

        public AllocationResult(ParkingSlot recommendedSlot, String reason, List<ParkingSlot> alternatives) {
            this.recommendedSlot = recommendedSlot;
            this.reason = reason;
            this.alternatives = alternatives;
        }

        public ParkingSlot getRecommendedSlot() { return recommendedSlot; }
        public String getReason() { return reason; }
        public List<ParkingSlot> getAlternatives() { return alternatives; }
    }

    public AllocationResult findBestSlot(VehicleType type) {
        List<ParkingSlot> available = slotDAO.getAvailableSlotsByType(type);

        // Fallback for EV if no EV spot available
        if (available.isEmpty() && type == VehicleType.EV_CAR) {
            available = slotDAO.getAvailableSlotsByType(VehicleType.CAR);
        }

        if (available.isEmpty()) {
            return new AllocationResult(null, "No available slots for " + type.getDisplayName(), List.of());
        }

        ParkingSlot best = available.get(0);
        String reason = "Closest available " + type.getDisplayName() + " slot on " + best.getFloor() + " (Zone " + best.getZone() + ")";
        if (best.isNearEntrance()) reason += " – Near Entrance";
        if (best.hasChargingStation()) reason += " with Fast Charger";

        List<ParkingSlot> altList = available.size() > 1 ? available.subList(1, Math.min(4, available.size())) : List.of();

        return new AllocationResult(best, reason, altList);
    }
}