package com.smartpark.controller;

import com.smartpark.algorithm.ExitCongestionEstimator.ExitRecommendation;
import com.smartpark.model.ParkingRecord;
import com.smartpark.model.PathResult;
import com.smartpark.model.RouteNode;
import com.smartpark.service.RoutePlanningService;
import com.smartpark.service.RoutePlanningService.VehicleLocationResult;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FindVehicleController {

    @FXML private TextField txtLocatorQuery;
    @FXML private Label lblLocatorStatus;

    @FXML private Label lblLocPlate;
    @FXML private Label lblLocSlot;
    @FXML private Label lblLocFloorZone;
    @FXML private Label lblLocTicket;
    @FXML private Label lblLocTime;
    @FXML private Label lblLocDuration;

    @FXML private ComboBox<RouteNode> comboExits;
    @FXML private Label lblExitCongestionAdvice;

    @FXML private Label lblRouteDistance;
    @FXML private Label lblRouteWalkTime;
    @FXML private ListView<String> listRouteSteps;

    private final RoutePlanningService routeService = new RoutePlanningService();
    private VehicleLocationResult currentLocation = null;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        listRouteSteps.setPlaceholder(new Label("Locate vehicle to generate turn-by-turn navigation instructions."));
    }

    @FXML
    public void handleLocateVehicle() {
        String query = txtLocatorQuery.getText();
        if (query == null || query.isBlank()) {
            lblLocatorStatus.setText("Please enter a registration plate or ticket ID.");
            lblLocatorStatus.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        currentLocation = routeService.locateVehicle(query);
        if (!currentLocation.isFound()) {
            lblLocatorStatus.setText(currentLocation.getMessage());
            lblLocatorStatus.setStyle("-fx-text-fill: #ef4444;");
            clearUI();
            return;
        }

        lblLocatorStatus.setText("Vehicle found successfully!");
        lblLocatorStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

        ParkingRecord r = currentLocation.getRecord();
        lblLocPlate.setText(r.getVehicleNumber());
        lblLocSlot.setText(r.getSlotNumber());
        lblLocFloorZone.setText(r.getFloor() + " - " + r.getZone());
        lblLocTicket.setText(r.getTicketId());
        lblLocTime.setText(r.getEntryTime().format(DTF));

        Duration dur = Duration.between(r.getEntryTime(), LocalDateTime.now());
        lblLocDuration.setText(dur.toHours() + "h " + (dur.toMinutes() % 60) + "m");

        // Populate exit nodes
        List<RouteNode> exits = currentLocation.getAvailableExits();
        comboExits.getItems().setAll(exits);
        if (!exits.isEmpty()) {
            comboExits.setValue(exits.get(0));
        }

        // Evaluate exit congestion
        if (currentLocation.getStartNode() != null) {
            List<ExitRecommendation> recs = routeService.evaluateAllExits(currentLocation.getStartNode().getNodeId());
            if (!recs.isEmpty()) {
                ExitRecommendation best = recs.get(0);
                lblExitCongestionAdvice.setText("★ Recommended: " + best.getRationale());
                // Pre-select best exit in combo
                comboExits.setValue(best.getExitNode());
            }
        }

        calculateAndDisplayRoute();
    }

    @FXML
    public void handleRecalculateRoute() {
        calculateAndDisplayRoute();
    }

    private void calculateAndDisplayRoute() {
        if (currentLocation == null || currentLocation.getStartNode() == null) return;
        RouteNode chosenExit = comboExits.getValue();
        if (chosenExit == null) return;

        PathResult path = routeService.calculateRouteToExit(
                currentLocation.getStartNode().getNodeId(), chosenExit.getNodeId()
        );

        if (path.isFound()) {
            lblRouteDistance.setText(path.getTotalDistanceMeters() + " meters");
            lblRouteWalkTime.setText("Estimated walking time: ~" + path.getEstimatedWalkingMinutes() + " mins");

            listRouteSteps.getItems().clear();
            int stepNum = 1;
            for (String instruction : path.getTurnByTurnInstructions()) {
                listRouteSteps.getItems().add(stepNum++ + ". " + instruction);
            }
        } else {
            lblRouteDistance.setText("No route");
            lblRouteWalkTime.setText("--");
            listRouteSteps.getItems().setAll("No usable path found to " + chosenExit.getName());
        }
    }

    private void clearUI() {
        lblLocPlate.setText("--");
        lblLocSlot.setText("--");
        lblLocFloorZone.setText("--");
        lblLocTicket.setText("--");
        lblLocTime.setText("--");
        lblLocDuration.setText("--");
        lblRouteDistance.setText("-- meters");
        lblRouteWalkTime.setText("Estimated walking time: -- mins");
        listRouteSteps.getItems().clear();
        comboExits.getItems().clear();
        lblExitCongestionAdvice.setText("Locate vehicle to evaluate peak hours and exit gate queue delays.");
    }
}
