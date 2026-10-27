package com.smartpark.dao;

import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingFloor;
import com.smartpark.model.ParkingSlot;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for ParkingSlot operations.
 */
public class ParkingSlotDAO {

    public List<ParkingSlot> getAllSlots() {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT * FROM parking_slots ORDER BY floor, zone, slot_number";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToSlot(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] getAllSlots: " + e.getMessage());
        }
        return list;
    }

    public List<ParkingSlot> getAvailableSlotsByType(VehicleType type) {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT * FROM parking_slots WHERE status = 'AVAILABLE' AND vehicle_type = ? ORDER BY is_near_entrance DESC, distance_to_entrance ASC, slot_number ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSlot(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] getAvailableSlotsByType: " + e.getMessage());
        }
        return list;
    }

    public List<ParkingSlot> getAllAvailableSlots() {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT * FROM parking_slots WHERE status = 'AVAILABLE' ORDER BY floor, zone, slot_number";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToSlot(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] getAllAvailableSlots: " + e.getMessage());
        }
        return list;
    }

    public ParkingSlot getSlotById(int slotId) {
        String sql = "SELECT * FROM parking_slots WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToSlot(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] getSlotById: " + e.getMessage());
        }
        return null;
    }

    public ParkingSlot getSlotByNumber(String slotNumber) {
        String sql = "SELECT * FROM parking_slots WHERE slot_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slotNumber.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToSlot(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] getSlotByNumber: " + e.getMessage());
        }
        return null;
    }

    public boolean updateStatus(int slotId, SlotStatus status) {
        String sql = "UPDATE parking_slots SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, slotId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] updateStatus: " + e.getMessage());
            return false;
        }
    }

    /**
     * Atomically changes slot status only if current status matches expected.
     * Prevents race conditions and double bookings.
     */
    public boolean atomicClaimSlot(int slotId, SlotStatus expectedStatus, SlotStatus newStatus) {
        String sql = "UPDATE parking_slots SET status = ? WHERE id = ? AND status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, slotId);
            ps.setString(3, expectedStatus.name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] atomicClaimSlot: " + e.getMessage());
            return false;
        }
    }

    public boolean createSlot(ParkingSlot slot) {
        String sql = "INSERT INTO parking_slots (slot_number, floor, zone, vehicle_type, status, is_near_entrance, is_near_exit, has_charging_station, is_accessible, distance_to_entrance, distance_to_exit) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, slot.getSlotNumber().trim().toUpperCase());
            ps.setString(2, slot.getFloor());
            ps.setString(3, slot.getZone());
            ps.setString(4, slot.getVehicleType().name());
            ps.setString(5, slot.getStatus().name());
            ps.setBoolean(6, slot.isNearEntrance());
            ps.setBoolean(7, slot.isNearExit());
            ps.setBoolean(8, slot.hasChargingStation());
            ps.setBoolean(9, slot.isAccessible());
            ps.setInt(10, slot.getDistanceToEntrance());
            ps.setInt(11, slot.getDistanceToExit());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) slot.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] createSlot: " + e.getMessage());
        }
        return false;
    }

    public boolean updateSlot(ParkingSlot slot) {
        String sql = "UPDATE parking_slots SET slot_number = ?, floor = ?, zone = ?, vehicle_type = ?, status = ?, is_near_entrance = ?, is_near_exit = ?, has_charging_station = ?, is_accessible = ?, distance_to_entrance = ?, distance_to_exit = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slot.getSlotNumber().trim().toUpperCase());
            ps.setString(2, slot.getFloor());
            ps.setString(3, slot.getZone());
            ps.setString(4, slot.getVehicleType().name());
            ps.setString(5, slot.getStatus().name());
            ps.setBoolean(6, slot.isNearEntrance());
            ps.setBoolean(7, slot.isNearExit());
            ps.setBoolean(8, slot.hasChargingStation());
            ps.setBoolean(9, slot.isAccessible());
            ps.setInt(10, slot.getDistanceToEntrance());
            ps.setInt(11, slot.getDistanceToExit());
            ps.setInt(12, slot.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] updateSlot: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteSlot(int slotId) {
        String sql = "DELETE FROM parking_slots WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] deleteSlot: " + e.getMessage());
            return false;
        }
    }

    public List<ParkingFloor> getAllFloors() {
        List<ParkingFloor> list = new ArrayList<>();
        String sql = "SELECT * FROM parking_floors ORDER BY floor_level ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ParkingFloor(
                        rs.getInt("id"),
                        rs.getString("floor_name"),
                        rs.getInt("floor_level"),
                        rs.getInt("total_slots")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[ParkingSlotDAO Error] getAllFloors: " + e.getMessage());
        }
        return list;
    }

    private ParkingSlot mapResultSetToSlot(ResultSet rs) throws SQLException {
        return new ParkingSlot(
                rs.getInt("id"),
                rs.getString("slot_number"),
                rs.getString("floor"),
                rs.getString("zone"),
                VehicleType.valueOf(rs.getString("vehicle_type")),
                SlotStatus.valueOf(rs.getString("status")),
                rs.getBoolean("is_near_entrance"),
                rs.getBoolean("is_near_exit"),
                rs.getBoolean("has_charging_station"),
                rs.getBoolean("is_accessible"),
                rs.getInt("distance_to_entrance"),
                rs.getInt("distance_to_exit")
        );
    }
}