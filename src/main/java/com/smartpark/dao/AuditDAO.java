package com.smartpark.dao;

import com.smartpark.model.AuditEvent;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO {

    public boolean logEvent(AuditEvent event) {
        String sql = "INSERT INTO audit_logs (timestamp, event_type, target_ref, description, actor) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now()));
            ps.setString(2, event.getEventType());
            ps.setString(3, event.getTargetRef() != null ? event.getTargetRef() : "-");
            ps.setString(4, event.getDescription());
            ps.setString(5, event.getActor() != null ? event.getActor() : "System");
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) event.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[AuditDAO Error] logEvent: " + e.getMessage());
        }
        return false;
    }

    public List<AuditEvent> getRecentEvents(int limit) {
        List<AuditEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AuditEvent(
                            rs.getInt("id"),
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("event_type"),
                            rs.getString("target_ref"),
                            rs.getString("description"),
                            rs.getString("actor")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuditDAO Error] getRecentEvents: " + e.getMessage());
        }
        return list;
    }

    public List<AuditEvent> getEventsByTarget(String targetRef) {
        List<AuditEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs WHERE target_ref = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetRef);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AuditEvent(
                            rs.getInt("id"),
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("event_type"),
                            rs.getString("target_ref"),
                            rs.getString("description"),
                            rs.getString("actor")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuditDAO Error] getEventsByTarget: " + e.getMessage());
        }
        return list;
    }
}
