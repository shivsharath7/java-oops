package com.smartpark.dao;

import com.smartpark.enums.PaymentStatus;
import com.smartpark.model.ParkingRecord;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ParkingRecordDAO {

    public boolean createRecord(ParkingRecord record) {
        String sql = "INSERT INTO parking_records (ticket_id, vehicle_id, slot_id, entry_time, payment_status, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getTicketId());
            ps.setInt(2, record.getVehicleId());
            ps.setInt(3, record.getSlotId());
            ps.setTimestamp(4, Timestamp.valueOf(record.getEntryTime()));
            ps.setString(5, record.getPaymentStatus().name());
            ps.setString(6, record.getStatus());
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ParkingRecord findActiveRecordByVehicleNumber(String vehicleNumber) {
        String sql = "SELECT pr.*, v.vehicle_number, ps.slot_number, ps.floor, ps.zone " +
                     "FROM parking_records pr " +
                     "JOIN vehicles v ON pr.vehicle_id = v.id " +
                     "JOIN parking_slots ps ON pr.slot_id = ps.id " +
                     "WHERE v.vehicle_number = ? AND pr.status = 'ACTIVE' LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicleNumber.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ParkingRecord r = new ParkingRecord();
                    r.setId(rs.getInt("id"));
                    r.setTicketId(rs.getString("ticket_id"));
                    r.setVehicleId(rs.getInt("vehicle_id"));
                    r.setSlotId(rs.getInt("slot_id"));
                    r.setVehicleNumber(rs.getString("vehicle_number"));
                    r.setSlotNumber(rs.getString("slot_number"));
                    r.setFloor(rs.getString("floor"));
                    r.setZone(rs.getString("zone"));
                    r.setEntryTime(rs.getTimestamp("entry_time").toLocalDateTime());
                    r.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
                    r.setStatus(rs.getString("status"));
                    return r;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean completeExit(int recordId, LocalDateTime exitTime, int durationMinutes, double fee) {
        String sql = "UPDATE parking_records SET exit_time = ?, duration_minutes = ?, parking_fee = ?, payment_status = 'PAID', status = 'COMPLETED' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(exitTime));
            ps.setInt(2, durationMinutes);
            ps.setDouble(3, fee);
            ps.setInt(4, recordId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}