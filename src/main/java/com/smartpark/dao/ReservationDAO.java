package com.smartpark.dao;

import com.smartpark.enums.ReservationStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.Reservation;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    public boolean createReservation(Reservation res) {
        String sql = "INSERT INTO reservations (reservation_code, vehicle_number, vehicle_type, slot_id, customer_name, contact_number, reserved_at, expires_at, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, res.getReservationCode());
            ps.setString(2, res.getVehicleNumber().trim().toUpperCase());
            ps.setString(3, res.getVehicleType().name());
            ps.setInt(4, res.getSlotId());
            ps.setString(5, res.getCustomerName());
            ps.setString(6, res.getContactNumber());
            ps.setTimestamp(7, Timestamp.valueOf(res.getReservedAt()));
            ps.setTimestamp(8, Timestamp.valueOf(res.getExpiresAt()));
            ps.setString(9, res.getStatus().name());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) res.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ReservationDAO Error] createReservation: " + e.getMessage());
        }
        return false;
    }

    public Reservation findActiveByVehicleNumber(String vehicleNumber) {
        String sql = "SELECT r.*, ps.slot_number, ps.floor, ps.zone FROM reservations r " +
                     "JOIN parking_slots ps ON r.slot_id = ps.id " +
                     "WHERE r.vehicle_number = ? AND r.status = 'PENDING' AND r.expires_at > NOW() LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicleNumber.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToReservation(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ReservationDAO Error] findActiveByVehicleNumber: " + e.getMessage());
        }
        return null;
    }

    public Reservation findByCode(String code) {
        String sql = "SELECT r.*, ps.slot_number, ps.floor, ps.zone FROM reservations r " +
                     "JOIN parking_slots ps ON r.slot_id = ps.id WHERE r.reservation_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToReservation(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ReservationDAO Error] findByCode: " + e.getMessage());
        }
        return null;
    }

    public List<Reservation> getAllReservations() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, ps.slot_number, ps.floor, ps.zone FROM reservations r " +
                     "JOIN parking_slots ps ON r.slot_id = ps.id ORDER BY r.id DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ReservationDAO Error] getAllReservations: " + e.getMessage());
        }
        return list;
    }

    public List<Reservation> getExpiredActiveReservations() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, ps.slot_number, ps.floor, ps.zone FROM reservations r " +
                     "JOIN parking_slots ps ON r.slot_id = ps.id " +
                     "WHERE r.status = 'PENDING' AND r.expires_at <= NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ReservationDAO Error] getExpiredActiveReservations: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int reservationId, ReservationStatus status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, reservationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReservationDAO Error] updateStatus: " + e.getMessage());
            return false;
        }
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setReservationCode(rs.getString("reservation_code"));
        r.setVehicleNumber(rs.getString("vehicle_number"));
        r.setVehicleType(VehicleType.valueOf(rs.getString("vehicle_type")));
        r.setSlotId(rs.getInt("slot_id"));
        r.setSlotNumber(rs.getString("slot_number"));
        r.setFloor(rs.getString("floor"));
        r.setZone(rs.getString("zone"));
        r.setCustomerName(rs.getString("customer_name"));
        r.setContactNumber(rs.getString("contact_number"));
        r.setReservedAt(rs.getTimestamp("reserved_at").toLocalDateTime());
        r.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        return r;
    }
}
