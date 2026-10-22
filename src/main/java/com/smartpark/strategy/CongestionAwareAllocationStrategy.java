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
 * Congestion-Aware Smart Allocation Strategy.
 * Calculates transparent multi-factor weighted score considering:
 * - Zone and floor software-estimated occupancy/congestion (40%)
 * - Proximity to complex entrance (35%)
 * - Proximity to exit (25%)
 * - Accessibility and EV fast-charging feature matching bonuses
 */
public class CongestionAwareAllocationStrategy implements SlotAllocationStrategy {

    private double congestionWeight = 0.40;
    private double entranceWeight = 0.35;
    private double exitWeight = 0.25;

    public CongestionAwareAllocationStrategy() {}

    public CongestionAwareAllocationStrategy(double congestionWeight, double entranceWeight, double exitWeight) {
        this.congestionWeight = congestionWeight;
        this.entranceWeight = entranceWeight;
        this.exitWeight = exitWeight;
    }

    @Override
    public String getName() {
        return "CONGESTION_AWARE";
    }

    @Override
    public String getDescription() {
        return "Balances zone congestion, entrance walking distance, and exit proximity using weighted scoring.";
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

        // 2. Strict eligibility check
        List<ParkingSlot> eligible = new ArrayList<>();
        for (ParkingSlot s : available) {
            if (isEligible(vehicle, s)) {
                eligible.add(s);
            }
        }

        // Fallback for EV vehicle without dedicated charger bay if user allows standard CAR slot
        if (eligible.isEmpty() && vehicle.getVehicleType() == VehicleType.EV_CAR) {
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

        // 3. Compute score for each eligible candidate
        List<AllocationCandidate> scoredCandidates = new ArrayList<>();
        for (ParkingSlot slot : eligible) {
            String zoneKey = slot.getFloor() + ":" + slot.getZone();
            double zoneOccupancyPct = (zoneOccupancyMap != null && zoneOccupancyMap.containsKey(zoneKey))
                    ? zoneOccupancyMap.get(zoneKey)
                    : calculateFallbackZoneOccupancy(slot.getFloor(), slot.getZone(), allSlots);

            // Invert occupancy: 0% occupied -> 100 congestion score; 80% occupied -> 20 congestion score
            double congestionScore = Math.max(0.0, 100.0 - zoneOccupancyPct);

            // Normalize distance to entrance (0-100)
            double distanceScore = Math.max(0.0, 100.0 - (slot.getDistanceToEntrance() * 1.5));
            if (slot.isNearEntrance()) distanceScore = Math.min(100.0, distanceScore + 15.0);

            // Normalize distance to exit (0-100)
            double exitDistScore = Math.max(0.0, 100.0 - (slot.getDistanceToExit() * 1.5));
            if (slot.isNearExit()) exitDistScore = Math.min(100.0, exitDistScore + 15.0);

            // Special feature bonus
            double preferenceScore = 50.0;
            if (vehicle.isAccessibilityNeeded() && slot.isAccessible()) preferenceScore += 30.0;
            if (vehicle.isRequiresCharging() && slot.hasChargingStation()) preferenceScore += 25.0;

            // Weighted composite score (Scale ~ 0-100)
            double totalScore = (congestionWeight * congestionScore) +
                                (entranceWeight * distanceScore) +
                                (exitWeight * exitDistScore) +
                                (preferenceScore * 0.1);

            // Build detailed explanation
            StringBuilder reason = new StringBuilder();
            reason.append("Floor: ").append(slot.getFloor()).append(" (Zone ").append(slot.getZone()).append("). ");
            reason.append("Zone Occupancy: ").append(String.format("%.0f%%", zoneOccupancyPct));
            if (zoneOccupancyPct < 40) reason.append(" (Low Congestion)");
            else if (zoneOccupancyPct < 75) reason.append(" (Moderate Congestion)");
            else reason.append(" (High Congestion)");

            if (slot.isNearEntrance()) reason.append(", Near Entrance (").append(slot.getDistanceToEntrance()).append("m)");
            if (slot.isNearExit()) reason.append(", Near Exit (").append(slot.getDistanceToExit()).append("m)");
            if (slot.hasChargingStation()) reason.append(", Fast Charger Ready");
            if (slot.isAccessible()) reason.append(", Step-Free Access");

            scoredCandidates.add(new AllocationCandidate(slot, totalScore, congestionScore, distanceScore, preferenceScore, reason.toString()));
        }

        // Sort descending by total score
        scoredCandidates.sort(Comparator.comparingDouble(AllocationCandidate::getTotalScore).reversed());

        AllocationCandidate topCandidate = scoredCandidates.get(0);
        ParkingSlot topSlot = topCandidate.getSlot();

        List<AllocationCandidate> topThree = scoredCandidates.subList(0, Math.min(3, scoredCandidates.size()));

        String explanation = "Recommended: " + topSlot.getSlotNumber() + " on " + topSlot.getFloor() +
                             " (Zone " + topSlot.getZone() + "). " +
                             "This slot balances lower software-estimated zone congestion (" +
                             String.format("%.0f%%", 100.0 - topCandidate.getCongestionScore()) + " occupancy), " +
                             "entrance distance (" + topSlot.getDistanceToEntrance() + "m), and exit convenience (" +
                             topSlot.getDistanceToExit() + "m) with an optimization score of " +
                             topCandidate.getTotalScore() + "/100.";

        return new AllocationDecision(topSlot, explanation, topThree, getName());
    }

    private double calculateFallbackZoneOccupancy(String floor, String zone, List<ParkingSlot> allSlots) {
        long totalInZone = allSlots.stream()
                .filter(s -> s.getFloor().equalsIgnoreCase(floor) && s.getZone().equalsIgnoreCase(zone))
                .count();
        if (totalInZone == 0) return 0.0;

        long occupiedInZone = allSlots.stream()
                .filter(s -> s.getFloor().equalsIgnoreCase(floor) && s.getZone().equalsIgnoreCase(zone) && s.getStatus() != SlotStatus.AVAILABLE)
                .count();

        return (occupiedInZone * 100.0) / totalInZone;
    }

    private boolean isEligible(Vehicle vehicle, ParkingSlot slot) {
        if (vehicle.isAccessibilityNeeded() && !slot.isAccessible()) return false;
        if (slot.isAccessible() && !vehicle.isAccessibilityNeeded() && vehicle.getVehicleType() != VehicleType.ACCESSIBLE_CAR) return false;
        if (vehicle.isRequiresCharging() && vehicle.getVehicleType() == VehicleType.EV_CAR && !slot.hasChargingStation()) return false;
        if (slot.hasChargingStation() && vehicle.getVehicleType() != VehicleType.EV_CAR) return false;

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

    public double getCongestionWeight() { return congestionWeight; }
    public void setCongestionWeight(double congestionWeight) { this.congestionWeight = congestionWeight; }
    public double getEntranceWeight() { return entranceWeight; }
    public void setEntranceWeight(double entranceWeight) { this.entranceWeight = entranceWeight; }
    public double getExitWeight() { return exitWeight; }
    public void setExitWeight(double exitWeight) { this.exitWeight = exitWeight; }
}
