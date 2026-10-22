package com.smartpark.dao;

import com.smartpark.enums.VehicleType;
import com.smartpark.model.Vehicle;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {

    public Vehicle findByNumber(String vehicleNumber) {
        if (vehicleNumber == null) return null;
        String sql = "SELECT * FROM vehicles WHERE vehicle_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicleNumber.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Vehicle(
                            rs.getInt("id"),
                            rs.getString("vehicle_number"),
                            VehicleType.valueOf(rs.getString("vehicle_type")),
                            rs.getString("customer_name"),
                            rs.getString("contact_number"),
                            rs.getBoolean("accessibility_needed"),
                            rs.getBoolean("requires_charging")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[VehicleDAO Error] findByNumber: " + e.getMessage());
        }
        return null;
    }

    public int saveOrGetVehicle(Vehicle vehicle) {
        Vehicle existing = findByNumber(vehicle.getVehicleNumber());
        if (existing != null) {
            // Update preferences if changed
            updateVehicle(vehicle);
            return existing.getId();
        }

        String sql = "INSERT INTO vehicles (vehicle_number, vehicle_type, customer_name, contact_number, accessibility_needed, requires_charging) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, vehicle.getVehicleNumber().toUpperCase().trim());
            ps.setString(2, vehicle.getVehicleType().name());
            ps.setString(3, vehicle.getCustomerName());
            ps.setString(4, vehicle.getContactNumber());
            ps.setBoolean(5, vehicle.isAccessibilityNeeded());
            ps.setBoolean(6, vehicle.isRequiresCharging());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    vehicle.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("[VehicleDAO Error] saveOrGetVehicle: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateVehicle(Vehicle vehicle) {
        String sql = "UPDATE vehicles SET vehicle_type = ?, customer_name = ?, contact_number = ?, accessibility_needed = ?, requires_charging = ? WHERE vehicle_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicle.getVehicleType().name());
            ps.setString(2, vehicle.getCustomerName());
            ps.setString(3, vehicle.getContactNumber());
            ps.setBoolean(4, vehicle.isAccessibilityNeeded());
            ps.setBoolean(5, vehicle.isRequiresCharging());
            ps.setString(6, vehicle.getVehicleNumber().toUpperCase().trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[VehicleDAO Error] updateVehicle: " + e.getMessage());
            return false;
        }
    }

    public List<Vehicle> getAllVehicles() {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT * FROM vehicles ORDER BY created_at DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Vehicle(
                        rs.getInt("id"),
                        rs.getString("vehicle_number"),
                        VehicleType.valueOf(rs.getString("vehicle_type")),
                        rs.getString("customer_name"),
                        rs.getString("contact_number"),
                        rs.getBoolean("accessibility_needed"),
                        rs.getBoolean("requires_charging")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[VehicleDAO Error] getAllVehicles: " + e.getMessage());
        }
        return list;
    }
}