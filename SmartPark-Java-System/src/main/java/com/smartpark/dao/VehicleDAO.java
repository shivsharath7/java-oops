package com.smartpark.dao;

import com.smartpark.enums.VehicleType;
import com.smartpark.model.Vehicle;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;

public class VehicleDAO {

    public Vehicle findByNumber(String vehicleNumber) {
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
                            rs.getString("contact_number")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int saveOrGetVehicle(Vehicle vehicle) {
        Vehicle existing = findByNumber(vehicle.getVehicleNumber());
        if (existing != null) {
            return existing.getId();
        }

        String sql = "INSERT INTO vehicles (vehicle_number, vehicle_type, customer_name, contact_number) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, vehicle.getVehicleNumber().toUpperCase().trim());
            ps.setString(2, vehicle.getVehicleType().name());
            ps.setString(3, vehicle.getCustomerName());
            ps.setString(4, vehicle.getContactNumber());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}