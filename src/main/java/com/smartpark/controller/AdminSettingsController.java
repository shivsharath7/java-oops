package com.smartpark.controller;

import com.smartpark.dao.*;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.AlternativeFacility;
import com.smartpark.model.AuditEvent;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.ParkingTariff;
import com.smartpark.service.AuthenticationService;
import com.smartpark.util.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.*;

public class AdminSettingsController {

    @FXML private Label lblAdminFeedback;

    // Tariffs Table
    @FXML private TableView<ParkingTariff> tableTariffs;
    @FXML private TableColumn<ParkingTariff, String> colTariffType;
    @FXML private TableColumn<ParkingTariff, String> colTariffFirstHr;
    @FXML private TableColumn<ParkingTariff, String> colTariffExtraHr;
    @FXML private TableColumn<ParkingTariff, String> colTariffDailyMax;
    @FXML private TableColumn<ParkingTariff, String> colTariffEvSurcharge;
    @FXML private TableColumn<ParkingTariff, String> colTariffGrace;

    // Edit Tariff Form
    @FXML private ComboBox<VehicleType> comboTariffEditType;
    @FXML private TextField txtEditFirstHr;
    @FXML private TextField txtEditExtraHr;
    @FXML private TextField txtEditDailyCap;
    @FXML private TextField txtEditEvSurcharge;
    @FXML private TextField txtEditGraceMins;

    // Settings
    @FXML private ComboBox<String> comboActiveStrategy;
    @FXML private TextField txtSettingHoldMinutes;
    @FXML private TextField txtSettingLowCong;
    @FXML private TextField txtSettingHighCong;
    @FXML private TextField txtPeakAmStart;
    @FXML private TextField txtPeakAmEnd;
    @FXML private TextField txtPeakPmStart;
    @FXML private TextField txtPeakPmEnd;
    @FXML private TextField txtSettingMaxStay;

    // Slot Inventory
    @FXML private TableView<ParkingSlot> tableAdminSlots;
    @FXML private TableColumn<ParkingSlot, String> colAdminSlotNum;
    @FXML private TableColumn<ParkingSlot, String> colAdminFloor;
    @FXML private TableColumn<ParkingSlot, String> colAdminZone;
    @FXML private TableColumn<ParkingSlot, String> colAdminType;
    @FXML private TableColumn<ParkingSlot, String> colAdminStatus;
    @FXML private TableColumn<ParkingSlot, String> colAdminEntrance;
    @FXML private TableColumn<ParkingSlot, String> colAdminCharger;
    @FXML private TableColumn<ParkingSlot, String> colAdminAccessible;

    // Alternative Facilities
    @FXML private TableView<AlternativeFacility> tableFacilities;
    @FXML private TableColumn<AlternativeFacility, String> colFacName;
    @FXML private TableColumn<AlternativeFacility, String> colFacAddress;
    @FXML private TableColumn<AlternativeFacility, String> colFacCap;
    @FXML private TableColumn<AlternativeFacility, String> colFacAvail;
    @FXML private TableColumn<AlternativeFacility, String> colFacDist;
    @FXML private TableColumn<AlternativeFacility, String> colFacPhone;

    private final TariffDAO tariffDAO = new TariffDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final FacilityDAO facilityDAO = new FacilityDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    @FXML
    public void initialize() {
        setupTariffs();
        setupSettings();
        setupSlots();
        setupFacilities();

        loadAllSettings();
    }

    private void setupTariffs() {
        colTariffType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleType().getDisplayName()));
        colTariffFirstHr.setCellValueFactory(c -> new SimpleStringProperty("₹" + String.format("%.2f", c.getValue().getFirstHourRate())));
        colTariffExtraHr.setCellValueFactory(c -> new SimpleStringProperty("₹" + String.format("%.2f", c.getValue().getAdditionalHourRate())));
        colTariffDailyMax.setCellValueFactory(c -> new SimpleStringProperty("₹" + String.format("%.2f", c.getValue().getDailyMaximum())));
        colTariffEvSurcharge.setCellValueFactory(c -> new SimpleStringProperty("₹" + String.format("%.2f", c.getValue().getEvSurchargePerHr())));
        colTariffGrace.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGracePeriodMins() + " mins"));

        comboTariffEditType.getItems().setAll(VehicleType.values());
        comboTariffEditType.setValue(VehicleType.CAR);
    }

    private void setupSettings() {
        comboActiveStrategy.getItems().setAll("CONGESTION_AWARE", "STANDARD");
        comboActiveStrategy.setValue("CONGESTION_AWARE");
    }

    private void setupSlots() {
        colAdminSlotNum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlotNumber()));
        colAdminFloor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFloor()));
        colAdminZone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getZone()));
        colAdminType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleType().name()));
        colAdminStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getLabel()));
        colAdminEntrance.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isNearEntrance() ? "Yes (" + c.getValue().getDistanceToEntrance() + "m)" : "No"));
        colAdminCharger.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().hasChargingStation() ? "Yes" : "No"));
        colAdminAccessible.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isAccessible() ? "Yes" : "No"));
    }

    private void setupFacilities() {
        colFacName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colFacAddress.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));
        colFacCap.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getTotalCapacity())));
        colFacAvail.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getAvailableEstimate())));
        colFacDist.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDistanceKm() + " km"));
        colFacPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getContactInfo()));
    }

    @FXML
    public void loadAllSettings() {
        // Tariffs
        Map<VehicleType, ParkingTariff> tariffs = tariffDAO.getAllTariffs();
        tableTariffs.getItems().setAll(tariffs.values());
        handleSelectTariffCategory();

        // Settings
        Map<String, String> s = settingsDAO.getAllSettings();
        comboActiveStrategy.setValue(s.getOrDefault("active_strategy", "CONGESTION_AWARE"));
        txtSettingHoldMinutes.setText(s.getOrDefault("reservation_hold_minutes", "15"));
        txtSettingLowCong.setText(s.getOrDefault("low_congestion_threshold", "40"));
        txtSettingHighCong.setText(s.getOrDefault("high_congestion_threshold", "75"));
        txtPeakAmStart.setText(s.getOrDefault("peak_hour_start_am", "08:30"));
        txtPeakAmEnd.setText(s.getOrDefault("peak_hour_end_am", "10:30"));
        txtPeakPmStart.setText(s.getOrDefault("peak_hour_start_pm", "17:00"));
        txtPeakPmEnd.setText(s.getOrDefault("peak_hour_end_pm", "20:00"));
        txtSettingMaxStay.setText(s.getOrDefault("max_allowed_stay_hours", "24"));

        // Slots
        tableAdminSlots.getItems().setAll(slotDAO.getAllSlots());

        // Facilities
        tableFacilities.getItems().setAll(facilityDAO.getAllFacilities());
    }

    @FXML
    public void handleSelectTariffCategory() {
        VehicleType type = comboTariffEditType.getValue();
        if (type == null) return;
        ParkingTariff t = tariffDAO.getTariffForType(type);
        if (t != null) {
            txtEditFirstHr.setText(String.valueOf(t.getFirstHourRate()));
            txtEditExtraHr.setText(String.valueOf(t.getAdditionalHourRate()));
            txtEditDailyCap.setText(String.valueOf(t.getDailyMaximum()));
            txtEditEvSurcharge.setText(String.valueOf(t.getEvSurchargePerHr()));
            txtEditGraceMins.setText(String.valueOf(t.getGracePeriodMins()));
        }
    }

    @FXML
    public void handleSaveTariff() {
        VehicleType type = comboTariffEditType.getValue();
        if (type == null) return;

        try {
            double firstHr = Double.parseDouble(txtEditFirstHr.getText().trim());
            double extraHr = Double.parseDouble(txtEditExtraHr.getText().trim());
            double cap = Double.parseDouble(txtEditDailyCap.getText().trim());
            double evSur = Double.parseDouble(txtEditEvSurcharge.getText().trim());
            int grace = Integer.parseInt(txtEditGraceMins.getText().trim());

            ParkingTariff t = new ParkingTariff(0, type, firstHr, extraHr, cap, evSur, grace);
            boolean ok = tariffDAO.updateTariff(t);
            if (ok) {
                lblAdminFeedback.setText("✓ Tariff for " + type.getDisplayName() + " saved successfully.");
                lblAdminFeedback.setStyle("-fx-text-fill: #10b981;");
                tableTariffs.getItems().setAll(tariffDAO.getAllTariffs().values());
            }
        } catch (Exception ex) {
            lblAdminFeedback.setText("Invalid tariff numbers: " + ex.getMessage());
            lblAdminFeedback.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void handleSaveSettings() {
        try {
            settingsDAO.updateSetting("active_strategy", comboActiveStrategy.getValue());
            settingsDAO.updateSetting("reservation_hold_minutes", txtSettingHoldMinutes.getText().trim());
            settingsDAO.updateSetting("low_congestion_threshold", txtSettingLowCong.getText().trim());
            settingsDAO.updateSetting("high_congestion_threshold", txtSettingHighCong.getText().trim());
            settingsDAO.updateSetting("peak_hour_start_am", txtPeakAmStart.getText().trim());
            settingsDAO.updateSetting("peak_hour_end_am", txtPeakAmEnd.getText().trim());
            settingsDAO.updateSetting("peak_hour_start_pm", txtPeakPmStart.getText().trim());
            settingsDAO.updateSetting("peak_hour_end_pm", txtPeakPmEnd.getText().trim());
            settingsDAO.updateSetting("max_allowed_stay_hours", txtSettingMaxStay.getText().trim());

            String operator = AuthenticationService.getInstance().isLoggedIn()
                    ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                    : "Administrator";

            auditDAO.logEvent(new AuditEvent("SETTINGS_UPDATE", "System",
                    "Operational settings and strategy thresholds updated.", operator));

            lblAdminFeedback.setText("✓ System operational settings saved to database.");
            lblAdminFeedback.setStyle("-fx-text-fill: #10b981;");
        } catch (Exception ex) {
            lblAdminFeedback.setText("Error saving settings: " + ex.getMessage());
            lblAdminFeedback.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void handleAddSlot() {
        Dialog<ParkingSlot> dialog = new Dialog<>();
        dialog.setTitle("Add New Parking Bay");
        dialog.setHeaderText("Add New Slot to Multi-Storey Inventory");

        ButtonType addBtn = new ButtonType("Add Slot", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField txtNum = new TextField();
        txtNum.setPromptText("e.g. G-A13, B-B09");

        ComboBox<String> comboFloor = new ComboBox<>();
        comboFloor.getItems().setAll("Ground Floor", "Basement 1", "First Floor");
        comboFloor.setValue("Ground Floor");

        TextField txtZone = new TextField("GF-A");
        ComboBox<VehicleType> comboType = new ComboBox<>();
        comboType.getItems().setAll(VehicleType.values());
        comboType.setValue(VehicleType.CAR);

        CheckBox chkEnt = new CheckBox("Near Entrance");
        CheckBox chkEx = new CheckBox("Near Exit");
        CheckBox chkChg = new CheckBox("Has Fast EV Charger");
        CheckBox chkAcc = new CheckBox("Accessible Designation");

        grid.add(new Label("Slot Number:"), 0, 0); grid.add(txtNum, 1, 0);
        grid.add(new Label("Floor:"), 0, 1); grid.add(comboFloor, 1, 1);
        grid.add(new Label("Zone:"), 0, 2); grid.add(txtZone, 1, 2);
        grid.add(new Label("Vehicle Type:"), 0, 3); grid.add(comboType, 1, 3);
        grid.add(chkEnt, 0, 4); grid.add(chkEx, 1, 4);
        grid.add(chkChg, 0, 5); grid.add(chkAcc, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == addBtn && !txtNum.getText().isBlank()) {
                ParkingSlot slot = new ParkingSlot();
                slot.setSlotNumber(txtNum.getText().trim().toUpperCase());
                slot.setFloor(comboFloor.getValue());
                slot.setZone(txtZone.getText().trim().toUpperCase());
                slot.setVehicleType(comboType.getValue());
                slot.setStatus(SlotStatus.AVAILABLE);
                slot.setNearEntrance(chkEnt.isSelected());
                slot.setNearExit(chkEx.isSelected());
                slot.setHasChargingStation(chkChg.isSelected());
                slot.setAccessible(chkAcc.isSelected());
                slot.setDistanceToEntrance(chkEnt.isSelected() ? 10 : 35);
                slot.setDistanceToExit(chkEx.isSelected() ? 10 : 35);
                return slot;
            }
            return null;
        });

        Optional<ParkingSlot> res = dialog.showAndWait();
        res.ifPresent(slot -> {
            boolean created = slotDAO.createSlot(slot);
            if (created) {
                lblAdminFeedback.setText("✓ Slot " + slot.getSlotNumber() + " added to database.");
                lblAdminFeedback.setStyle("-fx-text-fill: #10b981;");
                tableAdminSlots.getItems().setAll(slotDAO.getAllSlots());
            } else {
                lblAdminFeedback.setText("Failed to create slot (number may already exist).");
                lblAdminFeedback.setStyle("-fx-text-fill: #ef4444;");
            }
        });
    }

    @FXML
    public void handleToggleSlotMaintenance() {
        ParkingSlot sel = tableAdminSlots.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        SlotStatus next = (sel.getStatus() == SlotStatus.MAINTENANCE) ? SlotStatus.AVAILABLE : SlotStatus.MAINTENANCE;
        boolean ok = slotDAO.updateStatus(sel.getId(), next);
        if (ok) {
            lblAdminFeedback.setText("Slot " + sel.getSlotNumber() + " status updated to " + next.getLabel());
            lblAdminFeedback.setStyle("-fx-text-fill: #38bdf8;");
            tableAdminSlots.getItems().setAll(slotDAO.getAllSlots());
        }
    }

    @FXML
    public void handleResetDemoData() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Demonstration Database");
        confirm.setHeaderText("Re-seed MySQL Database smartpark_db?");
        confirm.setContentText("This will reload initial floors, slots, test vehicles, and clean transactions from smartpark.sql.\n\nProceed?");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                File sqlFile = new File("database/smartpark.sql");
                if (!sqlFile.exists()) {
                    lblAdminFeedback.setText("smartpark.sql not found in database/ folder.");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("--") || trimmed.isEmpty()) continue;
                        sb.append(line).append("\n");
                    }
                }

                String[] statements = sb.toString().split(";");
                try (Connection conn = DatabaseConnection.getConnection();
                     Statement st = conn.createStatement()) {
                    for (String stmtSql : statements) {
                        if (!stmtSql.trim().isEmpty()) {
                            st.execute(stmtSql.trim());
                        }
                    }
                }

                lblAdminFeedback.setText("✓ Demonstration database successfully re-seeded from smartpark.sql!");
                lblAdminFeedback.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                loadAllSettings();
            } catch (Exception ex) {
                lblAdminFeedback.setText("Re-seed error: " + ex.getMessage());
                lblAdminFeedback.setStyle("-fx-text-fill: #ef4444;");
            }
        }
    }
}
