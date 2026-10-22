package com.smartpark.dao;

import com.smartpark.model.RouteEdge;
import com.smartpark.model.RouteNode;
import com.smartpark.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RouteDAO {

    public List<RouteNode> getAllNodes() {
        List<RouteNode> list = new ArrayList<>();
        String sql = "SELECT * FROM route_nodes";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RouteNode n = new RouteNode(
                        rs.getString("node_id"),
                        rs.getString("floor"),
                        rs.getString("name"),
                        rs.getString("node_type"),
                        rs.getInt("x_coord"),
                        rs.getInt("y_coord")
                );
                n.setId(rs.getInt("id"));
                list.add(n);
            }
        } catch (SQLException e) {
            System.err.println("[RouteDAO Error] getAllNodes: " + e.getMessage());
        }
        return list;
    }

    public List<RouteEdge> getAllEdges() {
        List<RouteEdge> list = new ArrayList<>();
        String sql = "SELECT * FROM route_edges";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RouteEdge e = new RouteEdge(
                        rs.getString("from_node"),
                        rs.getString("to_node"),
                        rs.getInt("distance_weight"),
                        rs.getString("direction_info")
                );
                e.setId(rs.getInt("id"));
                list.add(e);
            }
        } catch (SQLException e) {
            System.err.println("[RouteDAO Error] getAllEdges: " + e.getMessage());
        }
        return list;
    }

    public List<RouteNode> getExitNodes() {
        List<RouteNode> list = new ArrayList<>();
        String sql = "SELECT * FROM route_nodes WHERE node_type = 'EXIT'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RouteNode n = new RouteNode(
                        rs.getString("node_id"),
                        rs.getString("floor"),
                        rs.getString("name"),
                        rs.getString("node_type"),
                        rs.getInt("x_coord"),
                        rs.getInt("y_coord")
                );
                n.setId(rs.getInt("id"));
                list.add(n);
            }
        } catch (SQLException e) {
            System.err.println("[RouteDAO Error] getExitNodes: " + e.getMessage());
        }
        return list;
    }
}
