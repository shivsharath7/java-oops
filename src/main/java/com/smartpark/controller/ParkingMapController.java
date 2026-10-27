package com.smartpark.controller;

import com.smartpark.dao.AuditDAO;
import com.smartpark.dao.ParkingRecordDAO;
import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.AuditEvent;
import com.smartpark.model.ParkingFloor;
import com.smartpark.model.ParkingRecord;
import com.smartpark.model.ParkingSlot;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ParkingMapController {

    @FXML private ComboBox<String> comboFloorFilter;
    @FXML private ComboBox<String> comboTypeFilter;
    @FXML private ComboBox<String> comboStatusFilter;
    @FXML private CheckBox chkChargingOnly;
    @FXML private CheckBox chkAccessibleOnly;

    @FXML private Label lblGridSummary;
    @FXML private FlowPane slotFlowPane;

    @FXML private Label lblSlotBadge;
    @FXML private Label lblDetailSlotNumber;
    @FXML private Label lblDetailFloorZone;
    @FXML private Label lblDetailType;
    @FXML private Label lblDetailProximity;
    @FXML private Label lblDetailCharger;
    @FXML private Label lblDetailAccessible;

    @FXML private VBox boxVehicleSession;
    @FXML private Label lblSessionVehicle;
    @FXML private Label lblSessionTicket;
    @FXML private Label lblSessionDuration;

    @FXML private Button btnParkHere;
    @FXML private Button btnReserveSlot;
    @FXML private Button btnToggleMaintenance;
    @FXML private Button btnFreeSlot;
    @FXML private Label lblActionFeedback;

    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    private List<ParkingSlot> allSlots = new ArrayList<>();
    private ParkingSlot selectedSlot = null;
    private MainAppController mainController;

    public void setMainController(MainAppController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        initFilterDropdowns();
        loadSlots();
    }

    private void initFilterDropdowns() {
        updateFloorFilterOptions();

        comboTypeFilter.getItems().setAll("All Types", "CAR", "BIKE", "EV_CAR", "ACCESSIBLE_CAR");
        comboTypeFilter.setValue("All Types");

        comboStatusFilter.getItems().setAll("All Statuses", "AVAILABLE", "OCCUPIED", "RESERVED", "EV_CHARGING", "MAINTENANCE");
        comboStatusFilter.setValue("All Statuses");
    }

    private void updateFloorFilterOptions() {
        String current = comboFloorFilter.getValue();
        java.util.Set<String> floors = new java.util.LinkedHashSet<>();
        floors.add("All Floors");

        try {
            for (ParkingFloor pf : slotDAO.getAllFloors()) {
                floors.add(pf.getFloorName());
            }
        } catch (Exception ignored) {}

        for (ParkingSlot s : allSlots) {
            if (s.getFloor() != null && !s.getFloor().isBlank()) {
                floors.add(s.getFloor());
            }
        }

        comboFloorFilter.getItems().setAll(floors);
        if (current != null && floors.contains(current)) {
            comboFloorFilter.setValue(current);
        } else {
            comboFloorFilter.setValue("All Floors");
        }
    }

    @FXML
    public void loadSlots() {
        Task<List<ParkingSlot>> task = new Task<>() {
            @Override
            protected List<ParkingSlot> call() {
                return slotDAO.getAllSlots();
            }
        };

        task.setOnSucceeded(e -> {
            allSlots = task.getValue();
            updateFloorFilterOptions();
            applyFilters();
        });

        Thread t = new Thread(task, "MapLoadSlotsThread");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void applyFilters() {
        String floor = comboFloorFilter.getValue();
        String type = comboTypeFilter.getValue();
        String status = comboStatusFilter.getValue();
        boolean chargingOnly = chkChargingOnly.isSelected();
        boolean accessibleOnly = chkAccessibleOnly.isSelected();

        List<ParkingSlot> filtered = allSlots.stream()
                .filter(s -> floor == null || "All Floors".equals(floor) || s.getFloor().equalsIgnoreCase(floor))
                .filter(s -> type == null || "All Types".equals(type) || s.getVehicleType().name().equalsIgnoreCase(type))
                .filter(s -> status == null || "All Statuses".equals(status) || s.getStatus().name().equalsIgnoreCase(status))
                .filter(s -> !chargingOnly || s.hasChargingStation())
                .filter(s -> !accessibleOnly || s.isAccessible())
                .collect(Collectors.toList());

        lblGridSummary.setText("Showing " + filtered.size() + " of " + allSlots.size() + " Parking Slots");
        renderGrid(filtered);

        if (!filtered.isEmpty()) {
            if (selectedSlot == null || !filtered.contains(selectedSlot)) {
                selectSlot(filtered.get(0));
            }
        } else {
            clearDetailSelection();
        }
    }

    private void clearDetailSelection() {
        this.selectedSlot = null;
        lblDetailSlotNumber.setText("--");
        lblDetailFloorZone.setText("--");
        lblDetailType.setText("--");
        lblDetailProximity.setText("--");
        lblDetailCharger.setText("--");
        lblDetailAccessible.setText("--");
        lblSlotBadge.setText("NONE");
        lblSlotBadge.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8;");
        lblSessionVehicle.setText("No Slot Selected");
        lblSessionTicket.setText("Ticket: --");
        lblSessionDuration.setText("Entry: --");
        btnParkHere.setDisable(true);
        btnReserveSlot.setDisable(true);
        btnFreeSlot.setDisable(true);
        btnToggleMaintenance.setDisable(true);
    }

    private void renderGrid(List<ParkingSlot> slots) {
        slotFlowPane.getChildren().clear();

        if (slots.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 40;");
            Label lblEmpty = new Label("No parking bays match the selected filter criteria.");
            lblEmpty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: bold;");
            Label lblSub = new Label("Try resetting floor, vehicle type, or status filters.");
            lblSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            emptyBox.getChildren().addAll(lblEmpty, lblSub);
            slotFlowPane.getChildren().add(emptyBox);
            return;
        }

        for (ParkingSlot s : slots) {
            VBox tile = createSlotTile(s);
            slotFlowPane.getChildren().add(tile);
        }
    }

    private VBox createSlotTile(ParkingSlot slot) {
        VBox tile = new VBox(4);
        tile.setAlignment(Pos.CENTER);
        tile.getStyleClass().add("slot-tile");

        // Color coding classes
        switch (slot.getStatus()) {
            case AVAILABLE -> tile.getStyleClass().add("slot-tile-available");
            case OCCUPIED -> tile.getStyleClass().add("slot-tile-occupied");
            case RESERVED -> tile.getStyleClass().add("slot-tile-reserved");
            case EV_CHARGING -> tile.getStyleClass().add("slot-tile-charging");
            case MAINTENANCE -> tile.getStyleClass().add("slot-tile-maintenance");
        }

        Label lblNum = new Label(slot.getSlotNumber());
        lblNum.getStyleClass().add("slot-number-label");

        String icons = "";
        if (slot.isAccessible()) icons += "♿ ";
        if (slot.hasChargingStation()) icons += "⚡ ";
        if (slot.isNearEntrance()) icons += "🚪 ";

        Label lblIcons = new Label(icons.isEmpty() ? slot.getVehicleType().name() : icons);
        lblIcons.getStyleClass().add("slot-type-label");

        Label lblStatus = new Label(slot.getStatus().getLabel());
        lblStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + slot.getStatus().getColorHex() + ";");

        tile.getChildren().addAll(lblNum, lblIcons, lblStatus);
        tile.setOnMouseClicked(e -> selectSlot(slot));

        // Tooltip
        Tooltip tt = new Tooltip(slot.getSlotNumber() + " (" + slot.getFloor() + " - Zone " + slot.getZone() + ")\n" +
                "Status: " + slot.getStatus().getLabel() + "\nType: " + slot.getVehicleType().getDisplayName());
        Tooltip.install(tile, tt);

        return tile;
    }

    private void selectSlot(ParkingSlot slot) {
        this.selectedSlot = slot;
        lblActionFeedback.setText("");

        lblDetailSlotNumber.setText(slot.getSlotNumber());
        lblDetailFloorZone.setText(slot.getFloor() + " (Zone " + slot.getZone() + ")");
        lblDetailType.setText(slot.getVehicleType().getDisplayName());
        lblDetailProximity.setText("Entrance: " + slot.getDistanceToEntrance() + "m | Exit: " + slot.getDistanceToExit() + "m");
        lblDetailCharger.setText(slot.hasChargingStation() ? "Yes (Fast Charger)" : "No");
        lblDetailAccessible.setText(slot.isAccessible() ? "Yes (Dedicated Accessible)" : "Standard");

        lblSlotBadge.setText(slot.getStatus().getLabel());
        lblSlotBadge.setStyle("-fx-background-color: " + slot.getStatus().getColorHex() + "33; -fx-text-fill: " + slot.getStatus().getColorHex() + ";");

        // Action button states
        btnParkHere.setDisable(slot.getStatus() != SlotStatus.AVAILABLE);
        btnReserveSlot.setDisable(slot.getStatus() != SlotStatus.AVAILABLE);
        btnFreeSlot.setDisable(slot.getStatus() == SlotStatus.AVAILABLE);
        btnToggleMaintenance.setText(slot.getStatus() == SlotStatus.MAINTENANCE ? "✅ Restore from Maintenance" : "🛠️ Set to Maintenance");

        // Load active parking record details if occupied
        if (slot.getStatus() == SlotStatus.OCCUPIED || slot.getStatus() == SlotStatus.EV_CHARGING) {
            ParkingRecord rec = recordDAO.getActiveRecordForSlot(slot.getId());
            if (rec != null) {
                lblSessionVehicle.setText(rec.getVehicleNumber());
                lblSessionTicket.setText("Ticket: " + rec.getTicketId());
                Duration dur = Duration.between(rec.getEntryTime(), LocalDateTime.now());
                lblSessionDuration.setText("Parked: " + dur.toHours() + "h " + (dur.toMinutes() % 60) + "m (" +
                        rec.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
            } else {
                lblSessionVehicle.setText("Unknown / Ghost Session");
                lblSessionTicket.setText("No active record linked (Anomaly)");
                lblSessionDuration.setText("Check anomalies tab");
            }
        } else if (slot.getStatus() == SlotStatus.RESERVED) {
            lblSessionVehicle.setText("Temporarily Reserved");
            lblSessionTicket.setText("Hold active");
            lblSessionDuration.setText("Awaiting driver arrival");
        } else {
            lblSessionVehicle.setText("No Active Session");
            lblSessionTicket.setText("Ticket: --");
            lblSessionDuration.setText("Entry: --");
        }
    }

    @FXML
    public void handleActionParkHere() {
        if (selectedSlot == null || selectedSlot.getStatus() != SlotStatus.AVAILABLE) return;
        if (mainController != null) {
            mainController.navigateToEntryWithSlot(selectedSlot);
        } else {
            lblActionFeedback.setText("Navigate to Vehicle Entry to assign slot " + selectedSlot.getSlotNumber());
            lblActionFeedback.setStyle("-fx-text-fill: #38bdf8;");
        }
    }

    @FXML
    public void handleActionReserveSlot() {
        if (selectedSlot == null || selectedSlot.getStatus() != SlotStatus.AVAILABLE) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Temporary Reservation Hold");
        dialog.setHeaderText("Reserve Slot " + selectedSlot.getSlotNumber());
        dialog.setContentText("Enter Vehicle Registration Number:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(plate -> {
            if (!plate.isBlank()) {
                boolean claimed = slotDAO.atomicClaimSlot(selectedSlot.getId(), SlotStatus.AVAILABLE, SlotStatus.RESERVED);
                if (claimed) {
                    auditDAO.logEvent(new AuditEvent("MANUAL_HOLD", selectedSlot.getSlotNumber(),
                            "Operator held slot " + selectedSlot.getSlotNumber() + " for " + plate.toUpperCase(), "Operator"));
                    lblActionFeedback.setText("Slot " + selectedSlot.getSlotNumber() + " held for " + plate.toUpperCase());
                    lblActionFeedback.setStyle("-fx-text-fill: #10b981;");
                    loadSlots();
                } else {
                    lblActionFeedback.setText("Failed to hold slot. It may have been allocated.");
                    lblActionFeedback.setStyle("-fx-text-fill: #ef4444;");
                }
            }
        });
    }

    @FXML
    public void handleActionToggleMaintenance() {
        if (selectedSlot == null) return;
        SlotStatus newStatus = (selectedSlot.getStatus() == SlotStatus.MAINTENANCE) ? SlotStatus.AVAILABLE : SlotStatus.MAINTENANCE;
        boolean ok = slotDAO.updateStatus(selectedSlot.getId(), newStatus);
        if (ok) {
            auditDAO.logEvent(new AuditEvent("SLOT_MAINTENANCE", selectedSlot.getSlotNumber(),
                    "Slot " + selectedSlot.getSlotNumber() + " status changed to " + newStatus.name(), "Operator"));
            lblActionFeedback.setText("Slot " + selectedSlot.getSlotNumber() + " is now " + newStatus.getLabel());
            lblActionFeedback.setStyle("-fx-text-fill: #38bdf8;");
            loadSlots();
        }
    }

    @FXML
    public void handleActionFreeSlot() {
        if (selectedSlot == null || selectedSlot.getStatus() == SlotStatus.AVAILABLE) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Slot Release");
        confirm.setHeaderText("Reset Slot " + selectedSlot.getSlotNumber() + " to AVAILABLE?");
        confirm.setContentText("This will mark the slot empty. Ensure any parked vehicle has departed.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            slotDAO.updateStatus(selectedSlot.getId(), SlotStatus.AVAILABLE);
            auditDAO.logEvent(new AuditEvent("MANUAL_RELEASE", selectedSlot.getSlotNumber(),
                    "Slot " + selectedSlot.getSlotNumber() + " forcibly reset to AVAILABLE by operator.", "Operator"));
            lblActionFeedback.setText("Slot " + selectedSlot.getSlotNumber() + " marked AVAILABLE.");
            lblActionFeedback.setStyle("-fx-text-fill: #10b981;");
            loadSlots();
        }
    }
}
