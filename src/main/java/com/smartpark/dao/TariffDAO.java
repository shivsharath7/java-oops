package com.smartpark.dao;

import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingTariff;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class TariffDAO {

    public Map<VehicleType, ParkingTariff> getAllTariffs() {
        Map<VehicleType, ParkingTariff> map = new HashMap<>();
        String sql = "SELECT * FROM parking_tariffs";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                VehicleType vt = VehicleType.valueOf(rs.getString("vehicle_type"));
                ParkingTariff t = new ParkingTariff(
                        rs.getInt("id"),
                        vt,
                        rs.getDouble("first_hour_rate"),
                        rs.getDouble("additional_hour_rate"),
                        rs.getDouble("daily_maximum"),
                        rs.getDouble("ev_surcharge_per_hr"),
                        rs.getInt("grace_period_mins")
                );
                map.put(vt, t);
            }
        } catch (SQLException e) {
            System.err.println("[TariffDAO Error] getAllTariffs: " + e.getMessage());
        }
        return map;
    }

    public ParkingTariff getTariffForType(VehicleType type) {
        String sql = "SELECT * FROM parking_tariffs WHERE vehicle_type = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ParkingTariff(
                            rs.getInt("id"),
                            type,
                            rs.getDouble("first_hour_rate"),
                            rs.getDouble("additional_hour_rate"),
                            rs.getDouble("daily_maximum"),
                            rs.getDouble("ev_surcharge_per_hr"),
                            rs.getInt("grace_period_mins")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[TariffDAO Error] getTariffForType: " + e.getMessage());
        }
        // Fallback default
        return new ParkingTariff(0, type, type.getFirstHourRate(), type.getAdditionalHourRate(), 250.0, 15.0, 15);
    }

    public boolean updateTariff(ParkingTariff tariff) {
        String sql = "UPDATE parking_tariffs SET first_hour_rate = ?, additional_hour_rate = ?, daily_maximum = ?, " +
                     "ev_surcharge_per_hr = ?, grace_period_mins = ? WHERE vehicle_type = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, tariff.getFirstHourRate());
            ps.setDouble(2, tariff.getAdditionalHourRate());
            ps.setDouble(3, tariff.getDailyMaximum());
            ps.setDouble(4, tariff.getEvSurchargePerHr());
            ps.setInt(5, tariff.getGracePeriodMins());
            ps.setString(6, tariff.getVehicleType().name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TariffDAO Error] updateTariff: " + e.getMessage());
            return false;
        }
    }
}
