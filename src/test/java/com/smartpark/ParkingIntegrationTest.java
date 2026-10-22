package com.smartpark;

import com.smartpark.dao.ParkingRecordDAO;
import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingRecord;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Vehicle;
import com.smartpark.service.ParkingService;
import com.smartpark.service.ParkingService.EntryResult;
import com.smartpark.service.ParkingService.ExitDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("End-to-End Parking Workflow Integration Tests")
public class ParkingIntegrationTest {

    private ParkingService parkingService;
    private ParkingSlotDAO slotDAO;
    private ParkingRecordDAO recordDAO;
    private int testSlotId;
    private final String testPlate = "KA09TEST9999";

    @BeforeEach
    void setup() {
        parkingService = new ParkingService();
        slotDAO = new ParkingSlotDAO();
        recordDAO = new ParkingRecordDAO();

        cleanTestData();

        ParkingSlot s = new ParkingSlot(0, "TEST-INT-01", "Ground Floor", "GF-A", VehicleType.CAR, SlotStatus.AVAILABLE, true, false);
        slotDAO.createSlot(s);
        testSlotId = s.getId();
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
        if (testSlotId > 0) {
            slotDAO.deleteSlot(testSlotId);
        }
    }

    private void cleanTestData() {
        try (java.sql.Connection conn = com.smartpark.util.DatabaseConnection.getConnection()) {
            if (conn != null) {
                try (java.sql.PreparedStatement ps = conn.prepareStatement("DELETE pr FROM parking_records pr JOIN parking_slots ps ON pr.slot_id = ps.id WHERE ps.slot_number = 'TEST-INT-01'")) {
                    ps.executeUpdate();
                }
                if (testSlotId > 0) {
                    try (java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM parking_records WHERE slot_id = ?")) {
                        ps.setInt(1, testSlotId);
                        ps.executeUpdate();
                    }
                }
                try (java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM parking_slots WHERE slot_number = 'TEST-INT-01'")) {
                    ps.executeUpdate();
                }
                try (java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM vehicles WHERE vehicle_number = ?")) {
                    ps.setString(1, testPlate);
                    ps.executeUpdate();
                }
            }
        } catch (java.sql.SQLException ignored) {}
    }

    @Test
    @DisplayName("Complete Lifecycle: Check-in, duplicate prevention, fee calculation, checkout, and slot release")
    void testCompleteParkingLifecycle() {
        Vehicle vehicle = new Vehicle(testPlate, VehicleType.CAR, "Integration Driver", "9845012345");

        // 1. Vehicle Check-In
        EntryResult entry = parkingService.parkVehicle(vehicle, testSlotId, "TestCashier", "Integration test entry");
        assertTrue(entry.isSuccess());
        assertNotNull(entry.getRecord());
        assertEquals(testPlate, entry.getRecord().getVehicleNumber());
        assertEquals("ACTIVE", entry.getRecord().getStatus());

        // Verify DB slot is now OCCUPIED
        ParkingSlot occupiedSlot = slotDAO.getSlotById(testSlotId);
        assertEquals(SlotStatus.OCCUPIED, occupiedSlot.getStatus());

        // 2. Prevent Duplicate Entry while active
        EntryResult duplicateEntry = parkingService.parkVehicle(vehicle, null, "TestCashier", "Duplicate attempt");
        assertFalse(duplicateEntry.isSuccess());
        assertTrue(duplicateEntry.getMessage().contains("already has an ACTIVE parking session"));

        // 3. Calculate Exit Details
        ExitDetails exitDetails = parkingService.calculateExitDetails(testPlate);
        assertNotNull(exitDetails);
        assertEquals(entry.getRecord().getTicketId(), exitDetails.getRecord().getTicketId());
        assertTrue(exitDetails.getBreakdown().getTotalFee() >= 0.0);

        // 4. Process Exit Payment
        boolean checkout = parkingService.processExitPayment(
                exitDetails.getRecord(), LocalDateTime.now(), (int) exitDetails.getBreakdown().getActualMinutes(),
                exitDetails.getBreakdown().getTotalFee(), "UPI", "TestCashier"
        );
        assertTrue(checkout);

        // 5. Verify Slot is released back to AVAILABLE
        ParkingSlot releasedSlot = slotDAO.getSlotById(testSlotId);
        assertEquals(SlotStatus.AVAILABLE, releasedSlot.getStatus());

        // 6. Verify Active Session is cleared
        ParkingRecord activeNow = recordDAO.findActiveRecordByVehicleNumber(testPlate);
        assertNull(activeNow);
    }
}
