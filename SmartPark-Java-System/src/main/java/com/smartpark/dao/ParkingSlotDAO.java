package com.smartpark.dao;

import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingSlot;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
            e.printStackTrace();
        }
        return list;
    }

    public List<ParkingSlot> getAvailableSlotsByType(VehicleType type) {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT * FROM parking_slots WHERE status = 'AVAILABLE' AND vehicle_type = ? ORDER BY floor, is_near_entrance DESC, zone, slot_number";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSlot(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateStatus(int slotId, SlotStatus status) {
        String sql = "UPDATE parking_slots SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, slotId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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
            e.printStackTrace();
        }
        return null;
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
                rs.getBoolean("has_charging_station")
        );
    }
}