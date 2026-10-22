package com.smartpark.strategy;

import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Vehicle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Standard Allocation Strategy:
 * Prioritizes the nearest available compatible slot to the entrance.
 */
public class StandardAllocationStrategy implements SlotAllocationStrategy {

    @Override
    public String getName() {
        return "STANDARD";
    }

    @Override
    public String getDescription() {
        return "Assigns the closest available compatible slot to the entrance.";
    }

    @Override
    public AllocationDecision allocate(Vehicle vehicle, List<ParkingSlot> allSlots,
                                       Map<String, Double> zoneOccupancyMap, String preferredExit) {
        if (vehicle == null) {
            return AllocationDecision.noSlotAvailable("Vehicle information is missing.", getName());
        }

        // 1. Filter available slots
        List<ParkingSlot> available = allSlots.stream()
                .filter(s -> s.getStatus() == SlotStatus.AVAILABLE)
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            return AllocationDecision.noSlotAvailable("Parking complex is completely full.", getName());
        }

        // 2. Eligibility checks
        List<ParkingSlot> eligible = new ArrayList<>();
        for (ParkingSlot s : available) {
            if (isEligible(vehicle, s)) {
                eligible.add(s);
            }
        }

        // Fallback for EV vehicle to standard CAR bay if no EV charger bay is available
        if (eligible.isEmpty() && vehicle.getVehicleType() == VehicleType.EV_CAR && vehicle.isRequiresCharging()) {
            for (ParkingSlot s : available) {
                if (s.getVehicleType() == VehicleType.CAR && !s.isAccessible()) {
                    eligible.add(s);
                }
            }
        }

        if (eligible.isEmpty()) {
            return AllocationDecision.noSlotAvailable(
                    "No eligible available slots found for vehicle type " + vehicle.getVehicleType().getDisplayName() +
                    (vehicle.isAccessibilityNeeded() ? " with accessibility requirements." : "."), getName());
        }

        // 3. Sort by proximity to entrance
        eligible.sort(Comparator
                .comparing(ParkingSlot::isNearEntrance, Comparator.reverseOrder())
                .thenComparingInt(ParkingSlot::getDistanceToEntrance)
                .thenComparing(ParkingSlot::getSlotNumber));

        List<AllocationCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < Math.min(3, eligible.size()); i++) {
            ParkingSlot s = eligible.get(i);
            double distScore = Math.max(10.0, 100.0 - s.getDistanceToEntrance());
            String reason = "Nearest available compatible slot on " + s.getFloor() + " (" + s.getZone() + ")" +
                            (s.isNearEntrance() ? " [Near Entrance]" : "") +
                            (s.hasChargingStation() ? " [EV Charging]" : "") +
                            (s.isAccessible() ? " [Accessible]" : "");
            candidates.add(new AllocationCandidate(s, distScore, 50.0, distScore, 80.0, reason));
        }

        ParkingSlot top = eligible.get(0);
        String explanation = "Recommended " + top.getSlotNumber() + " on " + top.getFloor() +
                             " because it is the closest available compatible slot to the entrance (" +
                             top.getDistanceToEntrance() + "m).";

        return new AllocationDecision(top, explanation, candidates, getName());
    }

    private boolean isEligible(Vehicle vehicle, ParkingSlot slot) {
        // Accessibility requirement check
        if (vehicle.isAccessibilityNeeded() && !slot.isAccessible()) {
            return false;
        }

        // Dedicated Accessible slot policy: Do not allocate accessible slot to non-accessible vehicles
        if (slot.isAccessible() && !vehicle.isAccessibilityNeeded() && vehicle.getVehicleType() != VehicleType.ACCESSIBLE_CAR) {
            return false;
        }

        // EV charging requirement check
        if (vehicle.isRequiresCharging() && vehicle.getVehicleType() == VehicleType.EV_CAR) {
            if (!slot.hasChargingStation()) return false;
        }

        // EV bay policy: Do not allocate EV charger bay to standard petrol/diesel cars or bikes
        if (slot.hasChargingStation() && vehicle.getVehicleType() != VehicleType.EV_CAR) {
            return false;
        }

        // General compatibility
        if (vehicle.getVehicleType() == VehicleType.BIKE) {
            return slot.getVehicleType() == VehicleType.BIKE;
        } else if (vehicle.isAccessibilityNeeded() || vehicle.getVehicleType() == VehicleType.ACCESSIBLE_CAR) {
            return slot.isAccessible() && (slot.getVehicleType() == VehicleType.ACCESSIBLE_CAR || slot.getVehicleType() == VehicleType.CAR);
        } else if (vehicle.getVehicleType() == VehicleType.CAR) {
            return slot.getVehicleType() == VehicleType.CAR && !slot.isAccessible() && !slot.hasChargingStation();
        } else if (vehicle.getVehicleType() == VehicleType.EV_CAR) {
            return slot.getVehicleType() == VehicleType.EV_CAR || slot.hasChargingStation() || (slot.getVehicleType() == VehicleType.CAR && !slot.isAccessible());
        }

        return false;
    }
}
