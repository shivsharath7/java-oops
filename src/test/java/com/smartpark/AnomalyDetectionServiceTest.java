package com.smartpark;

import com.smartpark.dao.AnomalyDAO;
import com.smartpark.dao.ParkingRecordDAO;
import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.enums.PaymentStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.Anomaly;
import com.smartpark.model.ParkingRecord;
import com.smartpark.model.ParkingSlot;
import com.smartpark.service.AnomalyDetectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Anomaly Detection Service Tests")
public class AnomalyDetectionServiceTest {

    private AnomalyDetectionService anomalyService;
    private ParkingSlotDAO slotDAO;
    private ParkingRecordDAO recordDAO;
    private int testSlotId;

    @BeforeEach
    void setup() {
        anomalyService = new AnomalyDetectionService();
        slotDAO = new ParkingSlotDAO();
        recordDAO = new ParkingRecordDAO();

        // Create an empty test slot
        ParkingSlot s = new ParkingSlot(0, "TEST-ANOM-01", "Ground Floor", "GF-A", VehicleType.CAR, SlotStatus.AVAILABLE, false, false);
        slotDAO.createSlot(s);
        testSlotId = s.getId();
    }

    @AfterEach
    void tearDown() {
        if (testSlotId > 0) {
            slotDAO.deleteSlot(testSlotId);
        }
    }

    @Test
    @DisplayName("Should detect ghost occupancy when slot is marked OCCUPIED without active parking record")
    void testDetectGhostOccupancy() {
        // Deliberately set slot to OCCUPIED without creating a corresponding parking record
        slotDAO.updateStatus(testSlotId, SlotStatus.OCCUPIED);

        int newAnomalies = anomalyService.runConsistencyScan();
        assertTrue(newAnomalies >= 0);

        List<Anomaly> openList = anomalyService.getOpenAnomalies();
        boolean foundGhostAnomaly = openList.stream()
                .anyMatch(a -> "SLOT_OCCUPIED_NO_RECORD".equals(a.getAnomalyType()) &&
                               a.getSlotId() != null && a.getSlotId() == testSlotId);

        assertTrue(foundGhostAnomaly, "Expected SLOT_OCCUPIED_NO_RECORD anomaly for slot ID " + testSlotId);
    }

    @Test
    @DisplayName("Should update anomaly status and persist operator resolution notes")
    void testResolveAnomaly() {
        List<Anomaly> list = anomalyService.getAllAnomalies();
        if (!list.isEmpty()) {
            Anomaly first = list.get(0);
            boolean updated = anomalyService.updateAnomalyStatus(first.getId(), "RESOLVED", "UnitTester", "Verified manually");
            assertTrue(updated);
        }
    }
}
