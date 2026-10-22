package com.smartpark.dao;

import com.smartpark.enums.DisputeStatus;
import com.smartpark.model.ParkingDispute;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DisputeDAO {

    public boolean createDispute(ParkingDispute dispute) {
        String sql = "INSERT INTO parking_disputes (record_id, ticket_id, vehicle_number, category, description, disputed_fee, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (dispute.getRecordId() != null) ps.setInt(1, dispute.getRecordId());
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, dispute.getTicketId());
            ps.setString(3, dispute.getVehicleNumber());
            ps.setString(4, dispute.getCategory());
            ps.setString(5, dispute.getDescription());
            ps.setDouble(6, dispute.getDisputedFee());
            ps.setString(7, dispute.getStatus().name());
            ps.setTimestamp(8, Timestamp.valueOf(dispute.getCreatedAt() != null ? dispute.getCreatedAt() : LocalDateTime.now()));
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) dispute.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DisputeDAO Error] createDispute: " + e.getMessage());
        }
        return false;
    }

    public List<ParkingDispute> getAllDisputes() {
        List<ParkingDispute> list = new ArrayList<>();
        String sql = "SELECT * FROM parking_disputes ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToDispute(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DisputeDAO Error] getAllDisputes: " + e.getMessage());
        }
        return list;
    }

    public boolean updateDisputeStatus(int id, DisputeStatus status, String resolutionNotes, String resolvedBy) {
        String sql = "UPDATE parking_disputes SET status = ?, resolved_at = NOW(), resolution_notes = ?, resolved_by = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, resolutionNotes);
            ps.setString(3, resolvedBy);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DisputeDAO Error] updateDisputeStatus: " + e.getMessage());
            return false;
        }
    }

    private ParkingDispute mapResultSetToDispute(ResultSet rs) throws SQLException {
        ParkingDispute d = new ParkingDispute();
        d.setId(rs.getInt("id"));
        int recId = rs.getInt("record_id");
        if (!rs.wasNull()) d.setRecordId(recId);
        d.setTicketId(rs.getString("ticket_id"));
        d.setVehicleNumber(rs.getString("vehicle_number"));
        d.setCategory(rs.getString("category"));
        d.setDescription(rs.getString("description"));
        d.setDisputedFee(rs.getDouble("disputed_fee"));
        d.setStatus(DisputeStatus.valueOf(rs.getString("status")));
        d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp resAt = rs.getTimestamp("resolved_at");
        if (resAt != null) d.setResolvedAt(resAt.toLocalDateTime());
        d.setResolutionNotes(rs.getString("resolution_notes"));
        d.setResolvedBy(rs.getString("resolved_by"));
        return d;
    }
}
