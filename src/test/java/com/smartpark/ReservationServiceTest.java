package com.smartpark;

import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.enums.ReservationStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Reservation;
import com.smartpark.service.ReservationService;
import com.smartpark.service.ReservationService.ReservationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Temporary Reservation Service Tests")
public class ReservationServiceTest {

    private ReservationService reservationService;
    private ParkingSlotDAO slotDAO;
    private int testSlotId;

    @BeforeEach
    void setup() {
        reservationService = new ReservationService();
        slotDAO = new ParkingSlotDAO();

        // Create a dedicated test slot
        ParkingSlot s = new ParkingSlot(0, "TEST-R01", "Ground Floor", "GF-A", VehicleType.CAR, SlotStatus.AVAILABLE, false, false);
        slotDAO.createSlot(s);
        testSlotId = s.getId();
    }

    @AfterEach
    void tearDown() {
        reservationService.stopCleaner();
        if (testSlotId > 0) {
            slotDAO.deleteSlot(testSlotId);
        }
    }

    @Test
    @DisplayName("Should successfully create temporary slot hold and mark slot RESERVED")
    void testCreateHold() {
        ReservationResult res = reservationService.createHold(
                "DL08TEST01", VehicleType.CAR, testSlotId, "Test Customer", "9999900000", 10
        );

        assertTrue(res.isSuccess());
        assertNotNull(res.getReservation());
        assertEquals(ReservationStatus.PENDING, res.getReservation().getStatus());

        // Check DB slot status
        ParkingSlot updatedSlot = slotDAO.getSlotById(testSlotId);
        assertEquals(SlotStatus.RESERVED, updatedSlot.getStatus());
    }

    @Test
    @DisplayName("Atomic Lock: Second concurrent reservation request for same slot must be rejected")
    void testConcurrentReservationRejected() {
        // First claim
        ReservationResult first = reservationService.createHold(
                "DL08TEST02", VehicleType.CAR, testSlotId, "Driver 1", "9999900001", 10
        );
        assertTrue(first.isSuccess());

        // Second competing claim
        ReservationResult second = reservationService.createHold(
                "DL08TEST03", VehicleType.CAR, testSlotId, "Driver 2", "9999900002", 10
        );
        assertFalse(second.isSuccess());
        assertTrue(second.getMessage().contains("not currently available") || second.getMessage().contains("claimed"));
    }

    @Test
    @DisplayName("Reservation cancellation should release slot back to AVAILABLE")
    void testCancelReservation() {
        ReservationResult res = reservationService.createHold(
                "DL08TEST04", VehicleType.CAR, testSlotId, "Driver 3", "9999900003", 10
        );
        assertTrue(res.isSuccess());

        boolean cancelled = reservationService.cancelReservation(res.getReservation().getId(), "TestOperator");
        assertTrue(cancelled);

        ParkingSlot updatedSlot = slotDAO.getSlotById(testSlotId);
        assertEquals(SlotStatus.AVAILABLE, updatedSlot.getStatus());
    }
}
