package com.smartpark.controller;

import com.smartpark.enums.AnomalySeverity;
import com.smartpark.model.Anomaly;
import com.smartpark.service.AnomalyDetectionService;
import com.smartpark.service.AuthenticationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AnomaliesController {

    @FXML private ComboBox<String> comboStatusFilter;
    @FXML private Label lblScanSummary;

    @FXML private TableView<Anomaly> tableAnomalies;
    @FXML private TableColumn<Anomaly, String> colId;
    @FXML private TableColumn<Anomaly, String> colSeverity;
    @FXML private TableColumn<Anomaly, String> colType;
    @FXML private TableColumn<Anomaly, String> colVehicle;
    @FXML private TableColumn<Anomaly, String> colSlot;
    @FXML private TableColumn<Anomaly, String> colDesc;
    @FXML private TableColumn<Anomaly, String> colResolution;
    @FXML private TableColumn<Anomaly, String> colStatus;
    @FXML private TableColumn<Anomaly, String> colDetectedAt;

    @FXML private Button btnAcknowledge;
    @FXML private Button btnResolve;
    @FXML private Button btnIgnore;
    @FXML private Label lblActionStatus;

    private final AnomalyDetectionService anomalyService = new AnomalyDetectionService();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        comboStatusFilter.getItems().setAll("Open / Action Required", "All Statuses", "Resolved Only");
        comboStatusFilter.setValue("Open / Action Required");

        setupTable();
        loadAnomalies();

        tableAnomalies.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean canAct = newV != null && !"RESOLVED".equals(newV.getStatus()) && !"IGNORED".equals(newV.getStatus());
            btnAcknowledge.setDisable(!canAct);
            btnResolve.setDisable(!canAct);
            btnIgnore.setDisable(!canAct);
        });

        btnAcknowledge.setDisable(true);
        btnResolve.setDisable(true);
        btnIgnore.setDisable(true);
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colSeverity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSeverity().getLabel()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAnomalyType()));
        colVehicle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleNumber() != null ? c.getValue().getVehicleNumber() : "--"));
        colSlot.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlotNumber() != null ? c.getValue().getSlotNumber() : "--"));
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colResolution.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSuggestedResolution()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colDetectedAt.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetectedAt().format(DTF)));

        colSeverity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Critical".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else if ("Warning".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    @FXML
    public void loadAnomalies() {
        String filter = comboStatusFilter.getValue();
        List<Anomaly> all = anomalyService.getAllAnomalies();
        List<Anomaly> filtered;

        if ("Open / Action Required".equals(filter)) {
            filtered = all.stream().filter(a -> "OPEN".equals(a.getStatus()) || "ACKNOWLEDGED".equals(a.getStatus())).collect(Collectors.toList());
        } else if ("Resolved Only".equals(filter)) {
            filtered = all.stream().filter(a -> "RESOLVED".equals(a.getStatus())).collect(Collectors.toList());
        } else {
            filtered = all;
        }

        tableAnomalies.getItems().setAll(filtered);
        long openCount = all.stream().filter(a -> "OPEN".equals(a.getStatus())).count();
        lblScanSummary.setText("Total Detected: " + all.size() + " | Unresolved Alerts: " + openCount);
    }

    @FXML
    public void handleRunScan() {
        int newFound = anomalyService.runConsistencyScan();
        loadAnomalies();
        lblActionStatus.setText("✓ Scan completed. " + (newFound > 0 ? newFound + " new anomalies identified!" : "No new anomalies found."));
        lblActionStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
    }

    @FXML
    public void handleAcknowledge() {
        Anomaly sel = tableAnomalies.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String operator = AuthenticationService.getInstance().isLoggedIn()
                ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                : "Operator";

        boolean ok = anomalyService.updateAnomalyStatus(sel.getId(), "ACKNOWLEDGED", operator, "Acknowledged by operator.");
        if (ok) {
            lblActionStatus.setText("Alert #" + sel.getId() + " marked as ACKNOWLEDGED.");
            lblActionStatus.setStyle("-fx-text-fill: #38bdf8;");
            loadAnomalies();
        }
    }

    @FXML
    public void handleResolve() {
        Anomaly sel = tableAnomalies.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        TextInputDialog dialog = new TextInputDialog("Verified and corrected by operator.");
        dialog.setTitle("Resolve Anomaly Alert");
        dialog.setHeaderText("Resolve Anomaly #" + sel.getId() + ": " + sel.getAnomalyType());
        dialog.setContentText("Enter resolution notes / audit justification:");

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(notes -> {
            String operator = AuthenticationService.getInstance().isLoggedIn()
                    ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                    : "Operator";

            boolean ok = anomalyService.updateAnomalyStatus(sel.getId(), "RESOLVED", operator, notes);
            if (ok) {
                lblActionStatus.setText("Alert #" + sel.getId() + " marked RESOLVED.");
                lblActionStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                loadAnomalies();
            }
        });
    }

    @FXML
    public void handleIgnore() {
        Anomaly sel = tableAnomalies.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String operator = AuthenticationService.getInstance().isLoggedIn()
                ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                : "Operator";

        boolean ok = anomalyService.updateAnomalyStatus(sel.getId(), "IGNORED", operator, "Operator marked as benign / expected state.");
        if (ok) {
            lblActionStatus.setText("Alert #" + sel.getId() + " marked IGNORED.");
            lblActionStatus.setStyle("-fx-text-fill: #94a3b8;");
            loadAnomalies();
        }
    }
}
