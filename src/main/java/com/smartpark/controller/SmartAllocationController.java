package com.smartpark.controller;

import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Vehicle;
import com.smartpark.service.SlotAllocationService;
import com.smartpark.strategy.AllocationCandidate;
import com.smartpark.strategy.AllocationDecision;
import com.smartpark.strategy.CongestionAwareAllocationStrategy;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SmartAllocationController {

    @FXML private ComboBox<VehicleType> comboSimVehicleType;
    @FXML private CheckBox chkSimCharging;
    @FXML private CheckBox chkSimAccessible;

    @FXML private Slider sliderCongestion;
    @FXML private Slider sliderEntrance;
    @FXML private Slider sliderExit;
    @FXML private Label lblCongestionWeight;
    @FXML private Label lblEntranceWeight;
    @FXML private Label lblExitWeight;

    @FXML private Label lblStdSlot;
    @FXML private Label lblStdInfo;
    @FXML private Label lblStdCongestion;

    @FXML private Label lblCongSlot;
    @FXML private Label lblCongInfo;
    @FXML private Label lblCongDetails;

    @FXML private TableView<AllocationCandidate> tableRanking;
    @FXML private TableColumn<AllocationCandidate, String> colRankSlot;
    @FXML private TableColumn<AllocationCandidate, String> colRankFloor;
    @FXML private TableColumn<AllocationCandidate, String> colRankScore;
    @FXML private TableColumn<AllocationCandidate, String> colRankCong;
    @FXML private TableColumn<AllocationCandidate, String> colRankDist;
    @FXML private TableColumn<AllocationCandidate, String> colRankReason;

    private final SlotAllocationService allocationService = new SlotAllocationService();

    @FXML
    public void initialize() {
        comboSimVehicleType.getItems().setAll(VehicleType.values());
        comboSimVehicleType.setValue(VehicleType.CAR);

        setupSliders();
        setupTable();
        handleRunComparison();
    }

    private void setupSliders() {
        sliderCongestion.valueProperty().addListener((obs, oldVal, newVal) ->
                lblCongestionWeight.setText(String.format("%.0f%%", newVal.doubleValue())));
        sliderEntrance.valueProperty().addListener((obs, oldVal, newVal) ->
                lblEntranceWeight.setText(String.format("%.0f%%", newVal.doubleValue())));
        sliderExit.valueProperty().addListener((obs, oldVal, newVal) ->
                lblExitWeight.setText(String.format("%.0f%%", newVal.doubleValue())));
    }

    private void setupTable() {
        colRankSlot.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlot().getSlotNumber()));
        colRankFloor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlot().getFloor() + " (" + c.getValue().getSlot().getZone() + ")"));
        colRankScore.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getTotalScore())));
        colRankCong.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCongestionScore())));
        colRankDist.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlot().getDistanceToEntrance() + "m"));
        colRankReason.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReason()));
    }

    @FXML
    public void handleRunComparison() {
        VehicleType type = comboSimVehicleType.getValue() != null ? comboSimVehicleType.getValue() : VehicleType.CAR;
        Vehicle vehicle = new Vehicle("SIM-TEST-01", type, "Test Simulation", "9999999999");
        vehicle.setRequiresCharging(chkSimCharging.isSelected());
        vehicle.setAccessibilityNeeded(chkSimAccessible.isSelected());

        // 1. Run Standard Strategy
        AllocationDecision stdResult = allocationService.evaluateAlternativeStrategy("STANDARD", vehicle, null);
        if (stdResult.hasRecommendation()) {
            ParkingSlot s = stdResult.getRecommendedSlot();
            lblStdSlot.setText(s.getSlotNumber());
            lblStdInfo.setText(s.getFloor() + " (Zone " + s.getZone() + ") - " + s.getDistanceToEntrance() + "m to entrance.");
            lblStdCongestion.setText("Selected purely by entrance proximity.");
        } else {
            lblStdSlot.setText("NONE");
            lblStdInfo.setText(stdResult.getExplanation());
            lblStdCongestion.setText("No compatible slot.");
        }

        // 2. Run Congestion-Aware Strategy with customized slider weights
        double cWeight = sliderCongestion.getValue() / 100.0;
        double eWeight = sliderEntrance.getValue() / 100.0;
        double xWeight = sliderExit.getValue() / 100.0;
        double totalW = cWeight + eWeight + xWeight;
        if (totalW > 0) {
            cWeight /= totalW;
            eWeight /= totalW;
            xWeight /= totalW;
        }

        CongestionAwareAllocationStrategy customCongestion = new CongestionAwareAllocationStrategy(cWeight, eWeight, xWeight);
        AllocationDecision congResult = customCongestion.allocate(
                vehicle, new com.smartpark.dao.ParkingSlotDAO().getAllSlots(),
                allocationService.computeZoneOccupancyMap(new com.smartpark.dao.ParkingSlotDAO().getAllSlots()), null
        );

        if (congResult.hasRecommendation()) {
            ParkingSlot s = congResult.getRecommendedSlot();
            lblCongSlot.setText(s.getSlotNumber());
            lblCongInfo.setText(s.getFloor() + " (Zone " + s.getZone() + ")");
            lblCongDetails.setText("Score: " + (congResult.getTopCandidates().isEmpty() ? "--" : congResult.getTopCandidates().get(0).getTotalScore()) +
                    " | Entrance: " + s.getDistanceToEntrance() + "m | Exit: " + s.getDistanceToExit() + "m");
            tableRanking.getItems().setAll(congResult.getTopCandidates());
        } else {
            lblCongSlot.setText("NONE");
            lblCongInfo.setText(congResult.getExplanation());
            lblCongDetails.setText("No compatible slot.");
            tableRanking.getItems().clear();
        }
    }
}
