package com.smartpark.dao;

import com.smartpark.enums.AnomalySeverity;
import com.smartpark.model.Anomaly;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnomalyDAO {

    public boolean createAnomaly(Anomaly anomaly) {
        String sql = "INSERT INTO anomalies (anomaly_type, severity, vehicle_number, slot_id, description, suggested_resolution, status, detected_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, anomaly.getAnomalyType());
            ps.setString(2, anomaly.getSeverity().name());
            ps.setString(3, anomaly.getVehicleNumber());
            if (anomaly.getSlotId() != null) ps.setInt(4, anomaly.getSlotId());
            else ps.setNull(4, Types.INTEGER);
            ps.setString(5, anomaly.getDescription());
            ps.setString(6, anomaly.getSuggestedResolution());
            ps.setString(7, anomaly.getStatus());
            ps.setTimestamp(8, Timestamp.valueOf(anomaly.getDetectedAt() != null ? anomaly.getDetectedAt() : LocalDateTime.now()));
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) anomaly.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[AnomalyDAO Error] createAnomaly: " + e.getMessage());
        }
        return false;
    }

    public List<Anomaly> getAllAnomalies() {
        List<Anomaly> list = new ArrayList<>();
        String sql = "SELECT a.*, ps.slot_number FROM anomalies a " +
                     "LEFT JOIN parking_slots ps ON a.slot_id = ps.id ORDER BY a.id DESC LIMIT 200";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAnomaly(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AnomalyDAO Error] getAllAnomalies: " + e.getMessage());
        }
        return list;
    }

    public List<Anomaly> getOpenAnomalies() {
        List<Anomaly> list = new ArrayList<>();
        String sql = "SELECT a.*, ps.slot_number FROM anomalies a " +
                     "LEFT JOIN parking_slots ps ON a.slot_id = ps.id WHERE a.status IN ('OPEN', 'ACKNOWLEDGED') ORDER BY a.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAnomaly(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AnomalyDAO Error] getOpenAnomalies: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int id, String status, String resolvedBy, String resolutionNotes) {
        String sql = "UPDATE anomalies SET status = ?, resolved_at = ?, resolved_by = ?, resolution_notes = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setTimestamp(2, "RESOLVED".equals(status) || "IGNORED".equals(status) ? Timestamp.valueOf(LocalDateTime.now()) : null);
            ps.setString(3, resolvedBy);
            ps.setString(4, resolutionNotes);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AnomalyDAO Error] updateStatus: " + e.getMessage());
            return false;
        }
    }

    private Anomaly mapResultSetToAnomaly(ResultSet rs) throws SQLException {
        Anomaly a = new Anomaly();
        a.setId(rs.getInt("id"));
        a.setAnomalyType(rs.getString("anomaly_type"));
        a.setSeverity(AnomalySeverity.valueOf(rs.getString("severity")));
        a.setVehicleNumber(rs.getString("vehicle_number"));
        int slotId = rs.getInt("slot_id");
        if (!rs.wasNull()) a.setSlotId(slotId);
        a.setSlotNumber(rs.getString("slot_number"));
        a.setDescription(rs.getString("description"));
        a.setSuggestedResolution(rs.getString("suggested_resolution"));
        a.setStatus(rs.getString("status"));
        a.setDetectedAt(rs.getTimestamp("detected_at").toLocalDateTime());
        Timestamp resAt = rs.getTimestamp("resolved_at");
        if (resAt != null) a.setResolvedAt(resAt.toLocalDateTime());
        a.setResolvedBy(rs.getString("resolved_by"));
        a.setResolutionNotes(rs.getString("resolution_notes"));
        return a;
    }
}
