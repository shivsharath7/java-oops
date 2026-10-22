package com.smartpark.dao;

import com.smartpark.enums.PaymentStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingRecord;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ParkingRecordDAO {

    public boolean createRecord(ParkingRecord record) {
        String sql = "INSERT INTO parking_records (ticket_id, vehicle_id, slot_id, entry_time, payment_status, payment_method, status, entry_operator, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getTicketId());
            ps.setInt(2, record.getVehicleId());
            ps.setInt(3, record.getSlotId());
            ps.setTimestamp(4, Timestamp.valueOf(record.getEntryTime()));
            ps.setString(5, record.getPaymentStatus() != null ? record.getPaymentStatus().name() : PaymentStatus.PENDING.name());
            ps.setString(6, record.getPaymentMethod() != null ? record.getPaymentMethod() : "NONE");
            ps.setString(7, record.getStatus() != null ? record.getStatus() : "ACTIVE");
            ps.setString(8, record.getEntryOperator() != null ? record.getEntryOperator() : "System");
            ps.setString(9, record.getNotes());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) record.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ParkingRecordDAO Error] createRecord: " + e.getMessage());
        }
        return false;
    }

    public ParkingRecord findActiveRecordByVehicleNumber(String vehicleNumber) {
        if (vehicleNumber == null) return null;
        String sql = "SELECT pr.*, v.vehicle_number, v.vehicle_type, ps.slot_number, ps.floor, ps.zone " +
                     "FROM parking_records pr " +
                     "JOIN vehicles v ON pr.vehicle_id = v.id " +
                     "JOIN parking_slots ps ON pr.slot_id = ps.id " +
                     "WHERE v.vehicle_number = ? AND pr.status = 'ACTIVE' LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicleNumber.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToRecord(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ParkingRecordDAO Error] findActiveRecordByVehicleNumber: " + e.getMessage());
        }
        return null;
    }

    public ParkingRecord findActiveRecordByTicketId(String ticketId) {
        if (ticketId == null) return null;
        String sql = "SELECT pr.*, v.vehicle_number, v.vehicle_type, ps.slot_number, ps.floor, ps.zone " +
                     "FROM parking_records pr " +
                     "JOIN vehicles v ON pr.vehicle_id = v.id " +
                     "JOIN parking_slots ps ON pr.slot_id = ps.id " +
                     "WHERE pr.ticket_id = ? AND pr.status = 'ACTIVE' LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToRecord(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ParkingRecordDAO Error] findActiveRecordByTicketId: " + e.getMessage());
        }
        return null;
    }

    public ParkingRecord findRecordByTicketOrPlate(String query) {
        if (query == null || query.isBlank()) return null;
        String clean = query.trim().toUpperCase();
        // First try ticket
        ParkingRecord r = findActiveRecordByTicketId(clean);
        if (r != null) return r;
        // Then try plate
        r = findActiveRecordByVehicleNumber(clean);
        if (r != null) return r;

        // Try completed search as fallback
        String sql = "SELECT pr.*, v.vehicle_number, v.vehicle_type, ps.slot_number, ps.floor, ps.zone " +
                     "FROM parking_records pr " +
                     "JOIN vehicles v ON pr.vehicle_id = v.id " +
                     "JOIN parking_slots ps ON pr.slot_id = ps.id " +
                     "WHERE (pr.ticket_id = ? OR v.vehicle_number = ?) ORDER BY pr.id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clean);
            ps.setString(2, clean);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToRecord(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ParkingRecordDAO Error] findRecordByTicketOrPlate: " + e.getMessage());
        }
        return null;
    }

    public ParkingRecord getActiveRecordForSlot(int slotId) {
        String sql = "SELECT pr.*, v.vehicle_number, v.vehicle_type, ps.slot_number, ps.floor, ps.zone " +
                     "FROM parking_records pr " +
                     "JOIN vehicles v ON pr.vehicle_id = v.id " +
                     "JOIN parking_slots ps ON pr.slot_id = ps.id " +
                     "WHERE pr.slot_id = ? AND pr.status = 'ACTIVE' LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToRecord(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ParkingRecordDAO Error] getActiveRecordForSlot: " + e.getMessage());
        }
        return null;
    }

    public boolean completeExit(int recordId, LocalDateTime exitTime, int durationMinutes, double fee,
                                String paymentMethod, String exitOperator) {
        String sql = "UPDATE parking_records SET exit_time = ?, duration_minutes = ?, parking_fee = ?, " +
                     "payment_status = 'PAID', payment_method = ?, status = 'COMPLETED', exit_operator = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(exitTime));
            ps.setInt(2, durationMinutes);
            ps.setDouble(3, fee);
            ps.setString(4, paymentMethod != null ? paymentMethod : "CASH");
            ps.setString(5, exitOperator != null ? exitOperator : "System");
            ps.setInt(6, recordId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ParkingRecordDAO Error] completeExit: " + e.getMessage());
            return false;
        }
    }

    public List<ParkingRecord> getAllActiveRecords() {
        List<ParkingRecord> list = new ArrayList<>();
        String sql = "SELECT pr.*, v.vehicle_number, v.vehicle_type, ps.slot_number, ps.floor, ps.zone " +
                     "FROM parking_records pr " +
                     "JOIN vehicles v ON pr.vehicle_id = v.id " +
                     "JOIN parking_slots ps ON pr.slot_id = ps.id " +
                     "WHERE pr.status = 'ACTIVE' ORDER BY pr.entry_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToRecord(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ParkingRecordDAO Error] getAllActiveRecords: " + e.getMessage());
        }
        return list;
    }

    public List<ParkingRecord> getRecentRecords(int limit) {
        List<ParkingRecord> list = new ArrayList<>();
        String sql = "SELECT pr.*, v.vehicle_number, v.vehicle_type, ps.slot_number, ps.floor, ps.zone " +
                     "FROM parking_records pr " +
                     "JOIN vehicles v ON pr.vehicle_id = v.id " +
                     "JOIN parking_slots ps ON pr.slot_id = ps.id " +
                     "ORDER BY pr.id DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRecord(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ParkingRecordDAO Error] getRecentRecords: " + e.getMessage());
        }
        return list;
    }

    public List<ParkingRecord> getRecordsByFilter(String search, String status, String vehicleType,
                                                  LocalDateTime fromDate, LocalDateTime toDate) {
        List<ParkingRecord> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT pr.*, v.vehicle_number, v.vehicle_type, ps.slot_number, ps.floor, ps.zone ")
                .append("FROM parking_records pr ")
                .append("JOIN vehicles v ON pr.vehicle_id = v.id ")
                .append("JOIN parking_slots ps ON pr.slot_id = ps.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append("AND (pr.ticket_id LIKE ? OR v.vehicle_number LIKE ? OR ps.slot_number LIKE ?) ");
            String q = "%" + search.trim() + "%";
            params.add(q); params.add(q); params.add(q);
        }
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            sql.append("AND pr.status = ? ");
            params.add(status);
        }
        if (vehicleType != null && !vehicleType.equalsIgnoreCase("ALL")) {
            sql.append("AND v.vehicle_type = ? ");
            params.add(vehicleType);
        }
        if (fromDate != null) {
            sql.append("AND pr.entry_time >= ? ");
            params.add(Timestamp.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append("AND pr.entry_time <= ? ");
            params.add(Timestamp.valueOf(toDate));
        }

        sql.append("ORDER BY pr.id DESC LIMIT 500");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRecord(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ParkingRecordDAO Error] getRecordsByFilter: " + e.getMessage());
        }
        return list;
    }

    private ParkingRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        ParkingRecord r = new ParkingRecord();
        r.setId(rs.getInt("id"));
        r.setTicketId(rs.getString("ticket_id"));
        r.setVehicleId(rs.getInt("vehicle_id"));
        r.setSlotId(rs.getInt("slot_id"));
        r.setVehicleNumber(rs.getString("vehicle_number"));
        try {
            r.setVehicleType(VehicleType.valueOf(rs.getString("vehicle_type")));
        } catch (Exception ignored) {}
        r.setSlotNumber(rs.getString("slot_number"));
        r.setFloor(rs.getString("floor"));
        r.setZone(rs.getString("zone"));
        r.setEntryTime(rs.getTimestamp("entry_time").toLocalDateTime());
        Timestamp exitTs = rs.getTimestamp("exit_time");
        if (exitTs != null) {
            r.setExitTime(exitTs.toLocalDateTime());
        }
        r.setDurationMinutes(rs.getInt("duration_minutes"));
        r.setParkingFee(rs.getDouble("parking_fee"));
        r.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        r.setPaymentMethod(rs.getString("payment_method"));
        r.setStatus(rs.getString("status"));
        r.setEntryOperator(rs.getString("entry_operator"));
        r.setExitOperator(rs.getString("exit_operator"));
        r.setNotes(rs.getString("notes"));
        return r;
    }
}