package com.smartpark.service;

import com.smartpark.dao.ChargingDAO;
import com.smartpark.dao.ParkingRecordDAO;
import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.dao.ReservationDAO;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingRecord;
import com.smartpark.model.ParkingSlot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service aggregating business intelligence analytics and CSV report generation.
 */
public class ReportService {

    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final ChargingDAO chargingDAO = new ChargingDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    public static class AnalyticsSummary {
        public int totalSlots;
        public int availableSlots;
        public int occupiedSlots;
        public int reservedSlots;
        public int activeSessions;
        public double totalRevenue;
        public double averageDurationMinutes;
        public int completedSessionsCount;
        public int evChargingCount;
        public int totalReservations;
        public Map<String, Integer> occupancyByFloor = new LinkedHashMap<>();
        public Map<VehicleType, Integer> countByVehicleType = new LinkedHashMap<>();
        public Map<VehicleType, Double> revenueByVehicleType = new LinkedHashMap<>();
        public Map<LocalDate, Double> dailyRevenue = new TreeMap<>();
        public Map<LocalDate, Integer> dailyEntries = new TreeMap<>();
    }

    public AnalyticsSummary computeAnalytics(LocalDateTime fromDate, LocalDateTime toDate, String vehicleTypeFilter) {
        AnalyticsSummary summary = new AnalyticsSummary();

        List<ParkingSlot> allSlots = slotDAO.getAllSlots();
        summary.totalSlots = allSlots.size();

        for (ParkingSlot s : allSlots) {
            if (s.getStatus() == SlotStatus.AVAILABLE) summary.availableSlots++;
            else if (s.getStatus() == SlotStatus.OCCUPIED || s.getStatus() == SlotStatus.EV_CHARGING) summary.occupiedSlots++;
            else if (s.getStatus() == SlotStatus.RESERVED) summary.reservedSlots++;

            summary.occupancyByFloor.put(s.getFloor(), summary.occupancyByFloor.getOrDefault(s.getFloor(), 0) +
                    (s.getStatus() != SlotStatus.AVAILABLE ? 1 : 0));
        }

        List<ParkingRecord> records = recordDAO.getRecordsByFilter(null, "ALL", vehicleTypeFilter, fromDate, toDate);
        double totalDuration = 0.0;
        int completedCount = 0;

        for (ParkingRecord r : records) {
            if (r.isActive()) {
                summary.activeSessions++;
            } else if ("COMPLETED".equals(r.getStatus())) {
                completedCount++;
                summary.totalRevenue += r.getParkingFee();
                totalDuration += r.getDurationMinutes();

                if (r.getVehicleType() != null) {
                    summary.revenueByVehicleType.put(r.getVehicleType(),
                            summary.revenueByVehicleType.getOrDefault(r.getVehicleType(), 0.0) + r.getParkingFee());
                }

                if (r.getExitTime() != null) {
                    LocalDate date = r.getExitTime().toLocalDate();
                    summary.dailyRevenue.put(date, summary.dailyRevenue.getOrDefault(date, 0.0) + r.getParkingFee());
                }
            }

            if (r.getVehicleType() != null) {
                summary.countByVehicleType.put(r.getVehicleType(),
                        summary.countByVehicleType.getOrDefault(r.getVehicleType(), 0) + 1);
            }

            if (r.getEntryTime() != null) {
                LocalDate eDate = r.getEntryTime().toLocalDate();
                summary.dailyEntries.put(eDate, summary.dailyEntries.getOrDefault(eDate, 0) + 1);
            }
        }

        summary.completedSessionsCount = completedCount;
        summary.averageDurationMinutes = completedCount > 0 ? (totalDuration / completedCount) : 0.0;
        summary.evChargingCount = chargingDAO.getRecentSessions(100).size();
        summary.totalReservations = reservationDAO.getAllReservations().size();

        return summary;
    }

    /**
     * Exports filtered parking records to a standard CSV file.
     */
    public File exportRecordsToCsv(File targetFile, LocalDateTime fromDate, LocalDateTime toDate,
                                  String vehicleTypeFilter) throws IOException {
        List<ParkingRecord> records = recordDAO.getRecordsByFilter(null, "ALL", vehicleTypeFilter, fromDate, toDate);

        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write("Ticket ID,Vehicle Number,Vehicle Type,Slot Number,Floor,Zone,Entry Time,Exit Time,Duration (Mins),Fee (INR),Payment Status,Payment Method,Session Status,Operator\n");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (ParkingRecord r : records) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        r.getTicketId(),
                        r.getVehicleNumber(),
                        r.getVehicleType() != null ? r.getVehicleType().name() : "CAR",
                        r.getSlotNumber(),
                        r.getFloor(),
                        r.getZone(),
                        r.getEntryTime() != null ? r.getEntryTime().format(dtf) : "",
                        r.getExitTime() != null ? r.getExitTime().format(dtf) : "",
                        r.getDurationMinutes(),
                        r.getParkingFee(),
                        r.getPaymentStatus() != null ? r.getPaymentStatus().name() : "PENDING",
                        r.getPaymentMethod() != null ? r.getPaymentMethod() : "NONE",
                        r.getStatus(),
                        r.getEntryOperator() != null ? r.getEntryOperator() : "System"
                ));
            }
        }
        return targetFile;
    }
}
