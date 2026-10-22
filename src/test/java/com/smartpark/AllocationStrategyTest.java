package com.smartpark;

import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Vehicle;
import com.smartpark.strategy.AllocationDecision;
import com.smartpark.strategy.CongestionAwareAllocationStrategy;
import com.smartpark.strategy.StandardAllocationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Slot Allocation Strategy Tests")
public class AllocationStrategyTest {

    private List<ParkingSlot> sampleSlots;

    @BeforeEach
    void setup() {
        sampleSlots = new ArrayList<>();
        // Ground Floor - Zone A (High congestion zone, 90% full)
        sampleSlots.add(new ParkingSlot(1, "G-A01", "Ground Floor", "GF-A", VehicleType.ACCESSIBLE_CAR, SlotStatus.AVAILABLE, true, false, false, true, 5, 40));
        sampleSlots.add(new ParkingSlot(2, "G-A02", "Ground Floor", "GF-A", VehicleType.EV_CAR, SlotStatus.AVAILABLE, true, false, true, false, 10, 38));
        sampleSlots.add(new ParkingSlot(3, "G-A03", "Ground Floor", "GF-A", VehicleType.CAR, SlotStatus.AVAILABLE, true, false, false, false, 12, 35));
        sampleSlots.add(new ParkingSlot(4, "G-A04", "Ground Floor", "GF-A", VehicleType.CAR, SlotStatus.OCCUPIED, false, false, false, false, 15, 30));

        // First Floor - Zone A (Low congestion zone, 10% full, slightly farther entrance)
        sampleSlots.add(new ParkingSlot(5, "F-A01", "First Floor", "FF-A", VehicleType.CAR, SlotStatus.AVAILABLE, false, true, false, false, 28, 12));
        sampleSlots.add(new ParkingSlot(6, "F-A02", "First Floor", "FF-A", VehicleType.CAR, SlotStatus.AVAILABLE, false, true, false, false, 30, 15));
        sampleSlots.add(new ParkingSlot(7, "F-A03", "First Floor", "FF-A", VehicleType.BIKE, SlotStatus.AVAILABLE, false, false, false, false, 20, 25));
    }

    @Test
    @DisplayName("Standard strategy should pick closest available compatible slot to entrance")
    void testStandardStrategyClosestEntrance() {
        StandardAllocationStrategy standard = new StandardAllocationStrategy();
        Vehicle car = new Vehicle("KA01AA1111", VehicleType.CAR, "Test Driver", "9876543210");

        AllocationDecision decision = standard.allocate(car, sampleSlots, Collections.emptyMap(), null);
        assertTrue(decision.hasRecommendation());
        assertEquals("G-A03", decision.getRecommendedSlot().getSlotNumber());
    }

    @Test
    @DisplayName("Congestion-aware strategy should favor low-congestion zone when Ground Floor is crowded")
    void testCongestionAwareStrategyFavorsLowCongestionZone() {
        CongestionAwareAllocationStrategy congestionStrat = new CongestionAwareAllocationStrategy(0.50, 0.25, 0.25);
        Vehicle car = new Vehicle("KA01AA1111", VehicleType.CAR, "Test Driver", "9876543210");

        Map<String, Double> zoneCongestion = new HashMap<>();
        zoneCongestion.put("Ground Floor:GF-A", 88.0); // 88% congested
        zoneCongestion.put("First Floor:FF-A", 10.0);  // 10% congested (smooth)

        AllocationDecision decision = congestionStrat.allocate(car, sampleSlots, zoneCongestion, null);
        assertTrue(decision.hasRecommendation());
        // Should recommend First Floor slot F-A01 because of lower congestion and exit proximity
        assertEquals("F-A01", decision.getRecommendedSlot().getSlotNumber());
        assertTrue(decision.getExplanation().contains("First Floor"));
    }

    @Test
    @DisplayName("Mandatory Accessibility Requirement: Must only allocate accessible designated slot")
    void testAccessibilityRequirementEnforcement() {
        StandardAllocationStrategy standard = new StandardAllocationStrategy();
        Vehicle accessibleVehicle = new Vehicle("DL01AC9999", VehicleType.CAR, "Accessible Driver", "9811223344");
        accessibleVehicle.setAccessibilityNeeded(true);

        AllocationDecision decision = standard.allocate(accessibleVehicle, sampleSlots, Collections.emptyMap(), null);
        assertTrue(decision.hasRecommendation());
        assertEquals("G-A01", decision.getRecommendedSlot().getSlotNumber());
        assertTrue(decision.getRecommendedSlot().isAccessible());
    }

    @Test
    @DisplayName("EV Fast Charging: Must allocate EV slot with charger when charging is required")
    void testEvChargingRequirementEnforcement() {
        StandardAllocationStrategy standard = new StandardAllocationStrategy();
        Vehicle evCar = new Vehicle("KA05EV8821", VehicleType.EV_CAR, "EV Driver", "9123456780");
        evCar.setRequiresCharging(true);

        AllocationDecision decision = standard.allocate(evCar, sampleSlots, Collections.emptyMap(), null);
        assertTrue(decision.hasRecommendation());
        assertEquals("G-A02", decision.getRecommendedSlot().getSlotNumber());
        assertTrue(decision.getRecommendedSlot().hasChargingStation());
    }

    @Test
    @DisplayName("Bike compatibility: Bike vehicle should only receive BIKE designated bay")
    void testBikeCompatibility() {
        StandardAllocationStrategy standard = new StandardAllocationStrategy();
        Vehicle bike = new Vehicle("MH12BK5555", VehicleType.BIKE, "Biker", "9988776655");

        AllocationDecision decision = standard.allocate(bike, sampleSlots, Collections.emptyMap(), null);
        assertTrue(decision.hasRecommendation());
        assertEquals("F-A03", decision.getRecommendedSlot().getSlotNumber());
        assertEquals(VehicleType.BIKE, decision.getRecommendedSlot().getVehicleType());
    }

    @Test
    @DisplayName("Facility Full: Returns clean explanation when no compatible slots remain")
    void testNoEligibleSlotHandling() {
        StandardAllocationStrategy standard = new StandardAllocationStrategy();
        // Mark all car slots occupied
        for (ParkingSlot s : sampleSlots) {
            if (s.getVehicleType() == VehicleType.CAR) s.setStatus(SlotStatus.OCCUPIED);
        }

        Vehicle car = new Vehicle("KA01AA1111", VehicleType.CAR, "Test Driver", "9876543210");
        AllocationDecision decision = standard.allocate(car, sampleSlots, Collections.emptyMap(), null);
        assertFalse(decision.hasRecommendation());
        assertNotNull(decision.getExplanation());
    }
}
