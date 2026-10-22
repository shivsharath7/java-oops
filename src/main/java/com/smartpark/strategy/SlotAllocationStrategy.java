package com.smartpark.strategy;

import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Vehicle;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for pluggable slot allocation algorithms.
 * Demonstrates Strategy Pattern (OOP Polymorphism and Abstraction).
 */
public interface SlotAllocationStrategy {
    String getName();
    String getDescription();

    AllocationDecision allocate(Vehicle vehicle, List<ParkingSlot> allSlots,
                                Map<String, Double> zoneOccupancyMap, String preferredExit);
}
