package com.smartpark.dao;

import com.smartpark.model.Admin;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;

public class AdminDAO {

    public Admin findByUsername(String username) {
        String sql = "SELECT * FROM admins WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Admin a = new Admin(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name"),
                            rs.getString("role")
                    );
                    a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return a;
                }
            }
        } catch (SQLException e) {
            System.err.println("[AdminDAO Error] findByUsername: " + e.getMessage());
        }
        return null;
    }

    public boolean updatePassword(int adminId, String newHash) {
        String sql = "UPDATE admins SET password_hash = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, adminId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AdminDAO Error] updatePassword: " + e.getMessage());
            return false;
        }
    }
}
