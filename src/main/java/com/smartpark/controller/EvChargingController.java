package com.smartpark.controller;

import com.smartpark.enums.ChargerStatus;
import com.smartpark.model.ChargingSession;
import com.smartpark.model.EvCharger;
import com.smartpark.service.AuthenticationService;
import com.smartpark.service.ChargingService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class EvChargingController {

    @FXML private ComboBox<EvCharger> comboChargers;
    @FXML private TextField txtEvPlate;
    @FXML private ComboBox<Integer> comboDuration;
    @FXML private Label lblChargingFormStatus;

    @FXML private TableView<ChargingSession> tableSessions;
    @FXML private TableColumn<ChargingSession, String> colChargerCode;
    @FXML private TableColumn<ChargingSession, String> colVehicle;
    @FXML private TableColumn<ChargingSession, String> colSlot;
    @FXML private TableColumn<ChargingSession, String> colStartTime;
    @FXML private TableColumn<ChargingSession, String> colEstEnd;
    @FXML private TableColumn<ChargingSession, String> colKwh;
    @FXML private TableColumn<ChargingSession, String> colFee;
    @FXML private TableColumn<ChargingSession, String> colStatus;

    @FXML private Button btnStopCharging;
    @FXML private Label lblSessionActionFeedback;
    @FXML private FlowPane flowChargerBays;

    private final ChargingService chargingService = new ChargingService();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        comboDuration.getItems().setAll(15, 30, 45, 60, 90, 120, 180);
        comboDuration.setValue(60);

        setupTable();
        loadData();

        tableSessions.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
                btnStopCharging.setDisable(newV == null));
        btnStopCharging.setDisable(true);
    }

    private void setupTable() {
        colChargerCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getChargerCode()));
        colVehicle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleNumber()));
        colSlot.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlotNumber()));
        colStartTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStartTime().format(TIME_FMT)));
        colEstEnd.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstimatedEndTime().format(TIME_FMT)));
        colKwh.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getKwhConsumed())));
        colFee.setCellValueFactory(c -> new SimpleStringProperty("₹" + String.format("%.2f", c.getValue().getChargingFee())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isOverdue() ? "⚠️ OVERDUE" : c.getValue().getStatus()
        ));
    }

    @FXML
    public void loadData() {
        List<EvCharger> chargers = chargingService.getAllChargers();
        comboChargers.getItems().setAll(chargers.stream().filter(c -> c.getStatus() == ChargerStatus.AVAILABLE).toList());
        if (!comboChargers.getItems().isEmpty()) comboChargers.setValue(comboChargers.getItems().get(0));

        List<ChargingSession> active = chargingService.getActiveSessions();
        tableSessions.getItems().setAll(active);

        renderChargerCards(chargers);
    }

    private void renderChargerCards(List<EvCharger> chargers) {
        flowChargerBays.getChildren().clear();

        for (EvCharger c : chargers) {
            VBox card = new VBox(4);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color: #0f172a; -fx-padding: 10 14; -fx-background-radius: 8; -fx-border-color: " +
                    c.getStatus().getColorHex() + "; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-pref-width: 180;");

            Label lblCode = new Label(c.getChargerCode());
            lblCode.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-font-size: 13px;");

            Label lblType = new Label(c.getChargerType());
            lblType.getStyleClass().add("text-muted");
            lblType.setStyle("-fx-font-size: 11px;");

            Label lblSlot = new Label("Slot: " + c.getSlotNumber());
            lblSlot.getStyleClass().add("text-sky");
            lblSlot.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

            Label lblStatus = new Label(c.getStatus().getLabel());
            lblStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + c.getStatus().getColorHex() + ";");

            card.getChildren().addAll(lblCode, lblType, lblSlot, lblStatus);
            flowChargerBays.getChildren().add(card);
        }
    }

    @FXML
    public void handleStartCharging() {
        lblChargingFormStatus.setText("");
        EvCharger charger = comboChargers.getValue();
        String plate = txtEvPlate.getText();
        if (charger == null || plate == null || plate.isBlank()) {
            lblChargingFormStatus.setText("Please select a charger and enter vehicle registration plate.");
            lblChargingFormStatus.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        int duration = comboDuration.getValue() != null ? comboDuration.getValue() : 60;
        String operator = AuthenticationService.getInstance().isLoggedIn()
                ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                : "Operator";

        boolean started = chargingService.startCharging(charger.getId(), plate, duration, operator);
        if (started) {
            lblChargingFormStatus.setText("✓ Charging started at " + charger.getChargerCode() + " for " + plate.toUpperCase());
            lblChargingFormStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            txtEvPlate.clear();
            loadData();
        } else {
            lblChargingFormStatus.setText("Failed to start charging session. Charger might be in use.");
            lblChargingFormStatus.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void handleStopCharging() {
        ChargingSession sel = tableSessions.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String operator = AuthenticationService.getInstance().isLoggedIn()
                ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                : "Operator";

        boolean stopped = chargingService.stopCharging(sel.getId(), operator);
        if (stopped) {
            lblSessionActionFeedback.setText("✓ Session stopped for " + sel.getVehicleNumber() + ". Delivered " + sel.getKwhConsumed() + " kWh.");
            lblSessionActionFeedback.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            loadData();
        } else {
            lblSessionActionFeedback.setText("Failed to stop charging session.");
            lblSessionActionFeedback.setStyle("-fx-text-fill: #ef4444;");
        }
    }
}
