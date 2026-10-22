package com.smartpark.controller;

import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingRecord;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Vehicle;
import com.smartpark.service.AuthenticationService;
import com.smartpark.service.ParkingService;
import com.smartpark.strategy.AllocationCandidate;
import com.smartpark.strategy.AllocationDecision;
import com.smartpark.util.ValidationUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

public class VehicleEntryController {

    @FXML private TextField txtPlate;
    @FXML private Label lblPlateValidation;
    @FXML private ComboBox<VehicleType> comboCategory;
    @FXML private ComboBox<String> comboExit;
    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private CheckBox chkNeedsCharging;
    @FXML private CheckBox chkNeedsAccessible;
    @FXML private TextField txtNotes;
    @FXML private Label lblFormError;

    @FXML private VBox boxRecommendation;
    @FXML private Label lblRecommendedSlotBadge;
    @FXML private Label lblRecommendedSlotInfo;
    @FXML private Label lblRecommendationExplanation;

    @FXML private TableView<AllocationCandidate> tableCandidates;
    @FXML private TableColumn<AllocationCandidate, String> colSlot;
    @FXML private TableColumn<AllocationCandidate, String> colFloor;
    @FXML private TableColumn<AllocationCandidate, String> colScore;
    @FXML private TableColumn<AllocationCandidate, String> colCongestionScore;
    @FXML private TableColumn<AllocationCandidate, String> colDistanceScore;
    @FXML private TableColumn<AllocationCandidate, String> colReason;

    @FXML private VBox boxTicketCard;
    @FXML private Label lblTicketId;
    @FXML private Label lblTicketPlate;
    @FXML private Label lblTicketType;
    @FXML private Label lblTicketSlot;
    @FXML private Label lblTicketFloorZone;
    @FXML private Label lblTicketTime;
    @FXML private Label lblTicketOperator;

    private final ParkingService parkingService = new ParkingService();
    private AllocationDecision lastDecision = null;
    private Integer presetSlotId = null;

    @FXML
    public void initialize() {
        initControls();
        setupCandidateTable();
    }

    private void initControls() {
        comboCategory.getItems().setAll(VehicleType.values());
        comboCategory.setValue(VehicleType.CAR);

        comboExit.getItems().setAll("North Exit Gate (Express)", "South Exit Gate (Ring Road)");
        comboExit.setValue("North Exit Gate (Express)");
    }

    private void setupCandidateTable() {
        colSlot.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlot().getSlotNumber()));
        colFloor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlot().getFloor() + " (" + c.getValue().getSlot().getZone() + ")"));
        colScore.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getTotalScore())));
        colCongestionScore.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCongestionScore())));
        colDistanceScore.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDistanceScore())));
        colReason.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReason()));
    }

    @FXML
    public void handlePlateInput() {
        String plate = ValidationUtil.cleanPlate(txtPlate.getText());
        if (plate.isEmpty()) {
            lblPlateValidation.setText("Standard alphanumeric format (4-12 characters)");
            lblPlateValidation.setStyle("-fx-text-fill: #94a3b8;");
        } else if (ValidationUtil.isValidVehicleNumber(plate)) {
            lblPlateValidation.setText("✓ Valid Registration Plate: " + plate);
            lblPlateValidation.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else {
            lblPlateValidation.setText("Invalid format. Use 4-12 characters (e.g. KA05EV8821)");
            lblPlateValidation.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void handleCategoryChange() {
        VehicleType type = comboCategory.getValue();
        if (type == VehicleType.EV_CAR) {
            chkNeedsCharging.setSelected(true);
        } else if (type == VehicleType.ACCESSIBLE_CAR) {
            chkNeedsAccessible.setSelected(true);
        }
    }

    public void preselectSlot(ParkingSlot slot) {
        if (slot == null) return;
        this.presetSlotId = slot.getId();
        comboCategory.setValue(slot.getVehicleType());
        if (slot.hasChargingStation()) chkNeedsCharging.setSelected(true);
        if (slot.isAccessible()) chkNeedsAccessible.setSelected(true);

        lblRecommendedSlotBadge.setText("PRESELECTED: " + slot.getSlotNumber());
        lblRecommendedSlotInfo.setText("Slot " + slot.getSlotNumber() + " on " + slot.getFloor() + " (Zone " + slot.getZone() + ") manually chosen from Map.");
        lblRecommendationExplanation.setText("Operator manually assigned this specific bay. Direct vehicle to slot on arrival.");
    }

    @FXML
    public void handleRunAllocation() {
        lblFormError.setText("");
        Vehicle vehicle = buildVehicleFromForm();
        if (vehicle == null) return;

        lastDecision = parkingService.previewAllocation(vehicle, comboExit.getValue());
        if (lastDecision.hasRecommendation()) {
            ParkingSlot top = lastDecision.getRecommendedSlot();
            lblRecommendedSlotBadge.setText("RECOMMENDED: " + top.getSlotNumber());
            lblRecommendedSlotInfo.setText("Slot " + top.getSlotNumber() + " on " + top.getFloor() + " (Zone " + top.getZone() + ")");
            lblRecommendationExplanation.setText(lastDecision.getExplanation());
            tableCandidates.getItems().setAll(lastDecision.getTopCandidates());
        } else {
            lblRecommendedSlotBadge.setText("NO SLOT AVAILABLE");
            lblRecommendedSlotInfo.setText("Facility Full / Incompatible");
            lblRecommendationExplanation.setText(lastDecision.getExplanation());
            tableCandidates.getItems().clear();
        }
    }

    @FXML
    public void handleConfirmEntry() {
        lblFormError.setText("");
        Vehicle vehicle = buildVehicleFromForm();
        if (vehicle == null) return;

        String operator = AuthenticationService.getInstance().isLoggedIn()
                ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                : "Operator";

        ParkingService.EntryResult result = parkingService.parkVehicle(
                vehicle, presetSlotId, operator, txtNotes.getText()
        );

        if (!result.isSuccess()) {
            lblFormError.setText(result.getMessage());
            return;
        }

        // Success: Update Digital Ticket
        ParkingRecord record = result.getRecord();
        lblTicketId.setText(record.getTicketId());
        lblTicketPlate.setText(record.getVehicleNumber());
        lblTicketType.setText(vehicle.getVehicleType().getDisplayName());
        lblTicketSlot.setText(record.getSlotNumber());
        lblTicketFloorZone.setText(record.getFloor() + " - " + record.getZone());
        lblTicketTime.setText(record.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        lblTicketOperator.setText(operator);

        // Show success alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Vehicle Entry Confirmed");
        alert.setHeaderText("Ticket Issued: " + record.getTicketId());
        alert.setContentText("Vehicle " + record.getVehicleNumber() + " successfully assigned to Slot " +
                record.getSlotNumber() + " on " + record.getFloor() + ".\nDigital ticket generated.");
        alert.showAndWait();

        // Clear form for next vehicle
        txtPlate.clear();
        txtName.clear();
        txtPhone.clear();
        txtNotes.clear();
        presetSlotId = null;
        lastDecision = null;
    }

    private Vehicle buildVehicleFromForm() {
        String plate = ValidationUtil.cleanPlate(txtPlate.getText());
        if (!ValidationUtil.isValidVehicleNumber(plate)) {
            lblFormError.setText("Please enter a valid vehicle registration number.");
            return null;
        }

        VehicleType type = comboCategory.getValue() != null ? comboCategory.getValue() : VehicleType.CAR;
        Vehicle v = new Vehicle(plate, type, txtName.getText(), txtPhone.getText());
        v.setRequiresCharging(chkNeedsCharging.isSelected());
        v.setAccessibilityNeeded(chkNeedsAccessible.isSelected());
        return v;
    }
}
