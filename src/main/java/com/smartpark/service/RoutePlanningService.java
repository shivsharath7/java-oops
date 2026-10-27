package com.smartpark.service;

import com.smartpark.algorithm.DijkstraRoutePlanner;
import com.smartpark.algorithm.ExitCongestionEstimator;
import com.smartpark.dao.ParkingRecordDAO;
import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.dao.RouteDAO;
import com.smartpark.dao.SettingsDAO;
import com.smartpark.model.*;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

/**
 * Service managing indoor navigation and smart exit routing.
 */
public class RoutePlanningService {

    private final RouteDAO routeDAO = new RouteDAO();
    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    private DijkstraRoutePlanner planner;
    private ExitCongestionEstimator exitEstimator;

    public RoutePlanningService() {
        initPlanner();
    }

    private void initPlanner() {
        List<RouteNode> nodes = routeDAO.getAllNodes();
        List<RouteEdge> edges = routeDAO.getAllEdges();
        this.planner = new DijkstraRoutePlanner(nodes, edges);
        this.exitEstimator = new ExitCongestionEstimator(planner);
    }

    public static class VehicleLocationResult {
        private final boolean found;
        private final ParkingRecord record;
        private final ParkingSlot slot;
        private final RouteNode startNode;
        private final List<RouteNode> availableExits;
        private final String message;

        public VehicleLocationResult(boolean found, ParkingRecord record, ParkingSlot slot,
                                     RouteNode startNode, List<RouteNode> availableExits, String message) {
            this.found = found;
            this.record = record;
            this.slot = slot;
            this.startNode = startNode;
            this.availableExits = availableExits != null ? availableExits : Collections.emptyList();
            this.message = message;
        }

        public boolean isFound() { return found; }
        public ParkingRecord getRecord() { return record; }
        public ParkingSlot getSlot() { return slot; }
        public RouteNode getStartNode() { return startNode; }
        public List<RouteNode> getAvailableExits() { return availableExits; }
        public String getMessage() { return message; }
    }

    public VehicleLocationResult locateVehicle(String ticketOrPlate) {
        ParkingRecord record = recordDAO.findRecordByTicketOrPlate(ticketOrPlate);
        if (record == null) {
            return new VehicleLocationResult(false, null, null, null, null,
                    "No parking record found for '" + ticketOrPlate + "'. Verify registration plate or ticket ID.");
        }

        if (!"ACTIVE".equalsIgnoreCase(record.getStatus())) {
            String exitTimeStr = record.getExitTime() != null 
                    ? record.getExitTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) 
                    : "earlier";
            return new VehicleLocationResult(false, record, null, null, null,
                    "Vehicle " + record.getVehicleNumber() + " checked out at " + exitTimeStr + 
                    " (Ticket: " + record.getTicketId() + "). Vehicle is not currently parked.");
        }

        ParkingSlot slot = slotDAO.getSlotById(record.getSlotId());
        List<RouteNode> allNodes = routeDAO.getAllNodes();
        List<RouteNode> exits = routeDAO.getExitNodes();

        // Match slot to route node (by slot number or nearest aisle)
        RouteNode matchedNode = null;
        if (slot != null) {
            String slotTarget = "N_SLOT_" + slot.getSlotNumber().replace("-", "").toUpperCase();
            for (RouteNode n : allNodes) {
                if (n.getNodeId().equalsIgnoreCase(slotTarget)) {
                    matchedNode = n;
                    break;
                }
            }
            if (matchedNode == null) {
                // Fallback to zone aisle
                String aisleTarget = slot.getZone().contains("B") ? "N_AISLE_GB" : "N_AISLE_GA";
                for (RouteNode n : allNodes) {
                    if (n.getNodeId().equalsIgnoreCase(aisleTarget)) {
                        matchedNode = n;
                        break;
                    }
                }
            }
        }

        if (matchedNode == null && !allNodes.isEmpty()) {
            matchedNode = allNodes.get(0);
        }

        return new VehicleLocationResult(true, record, slot, matchedNode, exits, "Vehicle located in Slot " + record.getSlotNumber());
    }

    public PathResult calculateRouteToExit(String startNodeId, String exitNodeId) {
        initPlanner(); // Refresh graph
        return planner.findShortestRoute(startNodeId, exitNodeId);
    }

    public List<ExitCongestionEstimator.ExitRecommendation> evaluateAllExits(String startNodeId) {
        initPlanner();
        List<RouteNode> exits = routeDAO.getExitNodes();

        LocalTime amStart = LocalTime.of(8, 30);
        LocalTime amEnd = LocalTime.of(10, 30);
        LocalTime pmStart = LocalTime.of(17, 0);
        LocalTime pmEnd = LocalTime.of(20, 0);

        try {
            amStart = LocalTime.parse(settingsDAO.getSetting("peak_hour_start_am", "08:30"));
            amEnd = LocalTime.parse(settingsDAO.getSetting("peak_hour_end_am", "10:30"));
            pmStart = LocalTime.parse(settingsDAO.getSetting("peak_hour_start_pm", "17:00"));
            pmEnd = LocalTime.parse(settingsDAO.getSetting("peak_hour_end_pm", "20:00"));
        } catch (Exception ignored) {}

        return exitEstimator.evaluateExits(startNodeId, exits, Collections.emptyMap(), amStart, amEnd, pmStart, pmEnd);
    }
}
