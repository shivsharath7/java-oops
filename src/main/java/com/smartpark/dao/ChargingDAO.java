package com.smartpark.dao;

import com.smartpark.enums.ChargerStatus;
import com.smartpark.model.ChargingSession;
import com.smartpark.model.EvCharger;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChargingDAO {

    public List<EvCharger> getAllChargers() {
        List<EvCharger> list = new ArrayList<>();
        String sql = "SELECT c.*, ps.slot_number FROM ev_chargers c " +
                     "JOIN parking_slots ps ON c.slot_id = ps.id ORDER BY c.id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new EvCharger(
                        rs.getInt("id"),
                        rs.getString("charger_code"),
                        rs.getInt("slot_id"),
                        rs.getString("slot_number"),
                        rs.getString("charger_type"),
                        rs.getDouble("power_kw"),
                        ChargerStatus.valueOf(rs.getString("status"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("[ChargingDAO Error] getAllChargers: " + e.getMessage());
        }
        return list;
    }

    public EvCharger getChargerBySlotId(int slotId) {
        String sql = "SELECT c.*, ps.slot_number FROM ev_chargers c " +
                     "JOIN parking_slots ps ON c.slot_id = ps.id WHERE c.slot_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new EvCharger(
                            rs.getInt("id"),
                            rs.getString("charger_code"),
                            rs.getInt("slot_id"),
                            rs.getString("slot_number"),
                            rs.getString("charger_type"),
                            rs.getDouble("power_kw"),
                            ChargerStatus.valueOf(rs.getString("status"))
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ChargingDAO Error] getChargerBySlotId: " + e.getMessage());
        }
        return null;
    }

    public boolean updateChargerStatus(int chargerId, ChargerStatus status) {
        String sql = "UPDATE ev_chargers SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, chargerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ChargingDAO Error] updateChargerStatus: " + e.getMessage());
            return false;
        }
    }

    public boolean startChargingSession(ChargingSession session) {
        String sql = "INSERT INTO charging_sessions (charger_id, vehicle_number, slot_id, start_time, estimated_end_time, kwh_consumed, status, charging_fee) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, session.getChargerId());
            ps.setString(2, session.getVehicleNumber());
            ps.setInt(3, session.getSlotId());
            ps.setTimestamp(4, Timestamp.valueOf(session.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(session.getEstimatedEndTime()));
            ps.setDouble(6, session.getKwhConsumed());
            ps.setString(7, session.getStatus());
            ps.setDouble(8, session.getChargingFee());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) session.setId(keys.getInt(1));
                }
                updateChargerStatus(session.getChargerId(), ChargerStatus.IN_USE);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ChargingDAO Error] startChargingSession: " + e.getMessage());
        }
        return false;
    }

    public boolean completeChargingSession(int sessionId, LocalDateTime actualEndTime, double kwh, double fee) {
        String sql = "UPDATE charging_sessions SET actual_end_time = ?, kwh_consumed = ?, charging_fee = ?, status = 'COMPLETED' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(actualEndTime));
            ps.setDouble(2, kwh);
            ps.setDouble(3, fee);
            ps.setInt(4, sessionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ChargingDAO Error] completeChargingSession: " + e.getMessage());
            return false;
        }
    }

    public ChargingSession getActiveSessionBySlotId(int slotId) {
        String sql = "SELECT cs.*, c.charger_code, ps.slot_number FROM charging_sessions cs " +
                     "JOIN ev_chargers c ON cs.charger_id = c.id " +
                     "JOIN parking_slots ps ON cs.slot_id = ps.id " +
                     "WHERE cs.slot_id = ? AND cs.status = 'IN_PROGRESS' LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToSession(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ChargingDAO Error] getActiveSessionBySlotId: " + e.getMessage());
        }
        return null;
    }

    public List<ChargingSession> getAllActiveSessions() {
        List<ChargingSession> list = new ArrayList<>();
        String sql = "SELECT cs.*, c.charger_code, ps.slot_number FROM charging_sessions cs " +
                     "JOIN ev_chargers c ON cs.charger_id = c.id " +
                     "JOIN parking_slots ps ON cs.slot_id = ps.id " +
                     "WHERE cs.status = 'IN_PROGRESS' ORDER BY cs.start_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToSession(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ChargingDAO Error] getAllActiveSessions: " + e.getMessage());
        }
        return list;
    }

    public List<ChargingSession> getRecentSessions(int limit) {
        List<ChargingSession> list = new ArrayList<>();
        String sql = "SELECT cs.*, c.charger_code, ps.slot_number FROM charging_sessions cs " +
                     "JOIN ev_chargers c ON cs.charger_id = c.id " +
                     "JOIN parking_slots ps ON cs.slot_id = ps.id " +
                     "ORDER BY cs.id DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ChargingDAO Error] getRecentSessions: " + e.getMessage());
        }
        return list;
    }

    private ChargingSession mapResultSetToSession(ResultSet rs) throws SQLException {
        ChargingSession s = new ChargingSession();
        s.setId(rs.getInt("id"));
        s.setChargerId(rs.getInt("charger_id"));
        s.setChargerCode(rs.getString("charger_code"));
        s.setVehicleNumber(rs.getString("vehicle_number"));
        s.setSlotId(rs.getInt("slot_id"));
        s.setSlotNumber(rs.getString("slot_number"));
        s.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        s.setEstimatedEndTime(rs.getTimestamp("estimated_end_time").toLocalDateTime());
        Timestamp actual = rs.getTimestamp("actual_end_time");
        if (actual != null) s.setActualEndTime(actual.toLocalDateTime());
        s.setKwhConsumed(rs.getDouble("kwh_consumed"));
        s.setStatus(rs.getString("status"));
        s.setChargingFee(rs.getDouble("charging_fee"));
        return s;
    }
}
