package com.smartpark.dao;

import com.smartpark.model.AlternativeFacility;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacilityDAO {

    public List<AlternativeFacility> getAllFacilities() {
        List<AlternativeFacility> list = new ArrayList<>();
        String sql = "SELECT * FROM alternative_facilities ORDER BY distance_km ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new AlternativeFacility(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("address"),
                        rs.getInt("total_capacity"),
                        rs.getInt("available_estimate"),
                        rs.getString("vehicle_types"),
                        rs.getDouble("distance_km"),
                        rs.getString("contact_info")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[FacilityDAO Error] getAllFacilities: " + e.getMessage());
        }
        return list;
    }

    public boolean updateFacility(AlternativeFacility facility) {
        String sql = "UPDATE alternative_facilities SET name = ?, address = ?, total_capacity = ?, available_estimate = ?, " +
                     "vehicle_types = ?, distance_km = ?, contact_info = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, facility.getName());
            ps.setString(2, facility.getAddress());
            ps.setInt(3, facility.getTotalCapacity());
            ps.setInt(4, facility.getAvailableEstimate());
            ps.setString(5, facility.getVehicleTypes());
            ps.setDouble(6, facility.getDistanceKm());
            ps.setString(7, facility.getContactInfo());
            ps.setInt(8, facility.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[FacilityDAO Error] updateFacility: " + e.getMessage());
            return false;
        }
    }
}
