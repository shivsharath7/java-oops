package com.smartpark.dao;

import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SettingsDAO {

    public Map<String, String> getAllSettings() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM system_settings";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            System.err.println("[SettingsDAO Error] getAllSettings: " + e.getMessage());
        }
        return map;
    }

    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("setting_value");
            }
        } catch (SQLException e) {
            System.err.println("[SettingsDAO Error] getSetting: " + e.getMessage());
        }
        return defaultValue;
    }

    public boolean updateSetting(String key, String value) {
        String sql = "INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE setting_value = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, value);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[SettingsDAO Error] updateSetting: " + e.getMessage());
            return false;
        }
    }
}
