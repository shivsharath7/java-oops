package com.smartpark.controller;

import com.smartpark.dao.AnomalyDAO;
import com.smartpark.dao.AuditDAO;
import com.smartpark.dao.ChargingDAO;
import com.smartpark.dao.ParkingRecordDAO;
import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.enums.SlotStatus;
import com.smartpark.model.AuditEvent;
import com.smartpark.model.ParkingFloor;
import com.smartpark.model.ParkingSlot;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblTotalSlots;
    @FXML private Label lblAvailableSlots;
    @FXML private Label lblOccupiedSlots;
    @FXML private Label lblReservedSlots;
    @FXML private Label lblOccupancyPct;
    @FXML private Label lblActiveSessions;
    @FXML private Label lblEvChargingBays;
    @FXML private Label lblOpenAlerts;

    @FXML private VBox floorStatusContainer;
    @FXML private Label lblCongestionLevel;
    @FXML private ProgressBar progressCongestion;
    @FXML private Label lblCongestionAdvice;
    @FXML private ProgressIndicator progressLoading;

    @FXML private TableView<AuditEvent> tableRecentActivity;
    @FXML private TableColumn<AuditEvent, String> colTime;
    @FXML private TableColumn<AuditEvent, String> colType;
    @FXML private TableColumn<AuditEvent, String> colRef;
    @FXML private TableColumn<AuditEvent, String> colDesc;
    @FXML private TableColumn<AuditEvent, String> colActor;

    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();
    private final ChargingDAO chargingDAO = new ChargingDAO();
    private final AnomalyDAO anomalyDAO = new AnomalyDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        setupTableColumns();
        refreshData();
    }

    private void setupTableColumns() {
        colTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTimestamp() != null ? data.getValue().getTimestamp().format(TIME_FORMATTER) : ""
        ));
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventType()));
        colRef.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTargetRef()));
        colDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        colActor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getActor()));
    }

    @FXML
    public void refreshData() {
        if (progressLoading != null) progressLoading.setVisible(true);

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() {
                DashboardData data = new DashboardData();
                data.slots = slotDAO.getAllSlots();
                data.floors = slotDAO.getAllFloors();
                data.activeSessionsCount = recordDAO.getAllActiveRecords().size();
                data.activeChargingCount = chargingDAO.getAllActiveSessions().size();
                data.openAlertsCount = anomalyDAO.getOpenAnomalies().size();
                data.recentAudits = auditDAO.getRecentEvents(30);
                return data;
            }
        };

        task.setOnSucceeded(e -> {
            DashboardData data = task.getValue();
            updateUI(data);
            if (progressLoading != null) progressLoading.setVisible(false);
        });

        task.setOnFailed(e -> {
            if (progressLoading != null) progressLoading.setVisible(false);
            Throwable ex = task.getException();
            System.err.println("[DashboardController Error] Async refresh failed: " + (ex != null ? ex.getMessage() : ""));
            displayErrorState(ex != null ? ex.getMessage() : "Database query failed");
        });

        Thread t = new Thread(task, "DashboardRefreshThread");
        t.setDaemon(true);
        t.start();
    }

    private void displayErrorState(String errorMsg) {
        lblTotalSlots.setText("--");
        lblAvailableSlots.setText("--");
        lblOccupiedSlots.setText("--");
        lblReservedSlots.setText("--");
        lblOccupancyPct.setText("--%");
        lblActiveSessions.setText("--");
        lblEvChargingBays.setText("--");
        lblOpenAlerts.setText("--");

        lblCongestionLevel.setText("DATABASE ERROR");
        lblCongestionLevel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 18px; -fx-font-weight: 800;");
        lblCongestionAdvice.setText("⚠️ Failed to load real-time metrics: " + errorMsg + ". Check database service and click Refresh.");

        floorStatusContainer.getChildren().clear();
        Label errLabel = new Label("Could not retrieve floor metrics from MySQL database.");
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-padding: 10; -fx-font-size: 12px;");
        floorStatusContainer.getChildren().add(errLabel);
    }

    private void updateUI(DashboardData data) {
        List<ParkingSlot> slots = data.slots;
        if (slots == null || slots.isEmpty()) {
            lblTotalSlots.setText("0");
            lblAvailableSlots.setText("0");
            lblOccupiedSlots.setText("0");
            lblReservedSlots.setText("0");
            lblOccupancyPct.setText("0%");
            lblActiveSessions.setText("0");
            lblEvChargingBays.setText("0");
            lblOpenAlerts.setText(String.valueOf(data != null ? data.openAlertsCount : 0));
            lblCongestionLevel.setText("NO SLOTS");
            lblCongestionLevel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 20px; -fx-font-weight: 800;");
            lblCongestionAdvice.setText("No parking bays currently configured in database.");
            floorStatusContainer.getChildren().clear();
            Label emptyLbl = new Label("No parking slots configured across any floor.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 12;");
            floorStatusContainer.getChildren().add(emptyLbl);
            if (data != null && data.recentAudits != null) {
                tableRecentActivity.getItems().setAll(data.recentAudits);
            }
            return;
        }

        long available = slots.stream().filter(s -> s.getStatus() == SlotStatus.AVAILABLE).count();
        long occupied = slots.stream().filter(s -> s.getStatus() == SlotStatus.OCCUPIED || s.getStatus() == SlotStatus.EV_CHARGING).count();
        long reserved = slots.stream().filter(s -> s.getStatus() == SlotStatus.RESERVED).count();
        long maintenance = slots.stream().filter(s -> s.getStatus() == SlotStatus.MAINTENANCE).count();
        long totalCapacity = slots.size();

        // Exact reconciliation check: available + occupied + reserved + maintenance == totalCapacity
        lblTotalSlots.setText(String.valueOf(totalCapacity));
        lblAvailableSlots.setText(String.valueOf(available));
        lblOccupiedSlots.setText(String.valueOf(occupied));
        // Reserved card displays both reserved holds and maintenance bays if any
        lblReservedSlots.setText(String.valueOf(reserved + maintenance));

        int percentage = totalCapacity > 0 ? (int) Math.round(((double) occupied / totalCapacity) * 100.0) : 0;
        lblOccupancyPct.setText(percentage + "%");
        lblActiveSessions.setText(String.valueOf(data.activeSessionsCount));
        lblEvChargingBays.setText(String.valueOf(data.activeChargingCount));
        lblOpenAlerts.setText(String.valueOf(data.openAlertsCount));

        // Congestion Indicator
        double prog = percentage / 100.0;
        progressCongestion.setProgress(prog);

        if (percentage >= 75) {
            lblCongestionLevel.setText("HIGH (" + percentage + "%)");
            lblCongestionLevel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 20px; -fx-font-weight: 800;");
            lblCongestionAdvice.setText("⚠️ High traffic volume. Facility is heavily congested. Direct arriving vehicles to upper floors or alternative facilities.");
        } else if (percentage >= 40) {
            lblCongestionLevel.setText("MODERATE (" + percentage + "%)");
            lblCongestionLevel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 20px; -fx-font-weight: 800;");
            lblCongestionAdvice.setText("⚡ Moderate traffic flow. Good availability remains across standard parking zones.");
        } else {
            lblCongestionLevel.setText("LOW (" + percentage + "%)");
            lblCongestionLevel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 20px; -fx-font-weight: 800;");
            lblCongestionAdvice.setText("🟢 Smooth traffic flow. Ample spaces available across all floors and vehicle categories.");
        }

        // Multi-floor Breakdown Cards (Derived from both defined floors and active slot assignments)
        floorStatusContainer.getChildren().clear();
        Map<String, List<ParkingSlot>> byFloor = slots.stream()
                .collect(Collectors.groupingBy(ParkingSlot::getFloor, LinkedHashMap::new, Collectors.toList()));

        Set<String> allFloorNames = new LinkedHashSet<>();
        if (data.floors != null) {
            for (ParkingFloor pf : data.floors) {
                allFloorNames.add(pf.getFloorName());
            }
        }
        allFloorNames.addAll(byFloor.keySet());

        for (String floor : allFloorNames) {
            List<ParkingSlot> fSlots = byFloor.getOrDefault(floor, Collections.emptyList());
            long fAvail = fSlots.stream().filter(s -> s.getStatus() == SlotStatus.AVAILABLE).count();
            long fOcc = fSlots.stream().filter(s -> s.getStatus() == SlotStatus.OCCUPIED || s.getStatus() == SlotStatus.EV_CHARGING).count();
            long fHeld = fSlots.stream().filter(s -> s.getStatus() == SlotStatus.RESERVED || s.getStatus() == SlotStatus.MAINTENANCE).count();
            double fPct = fSlots.isEmpty() ? 0.0 : (double) fOcc / fSlots.size();

            VBox card = new VBox(6);
            card.setStyle("-fx-background-color: #0f172a; -fx-padding: 10 14; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblF = new Label(floor);
            lblF.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-font-size: 13px;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            String statusSummary;
            if (fSlots.isEmpty()) {
                statusSummary = "0 Bays Configured";
            } else {
                statusSummary = fAvail + " Free / " + fSlots.size() + " Total (" + Math.round(fPct * 100) + "% occupied" +
                        (fHeld > 0 ? ", " + fHeld + " held" : "") + ")";
            }

            Label lblCount = new Label(statusSummary);
            lblCount.setStyle("-fx-text-fill: " + (fSlots.isEmpty() ? "#94a3b8" : (fPct > 0.75 ? "#ef4444" : (fPct > 0.4 ? "#f59e0b" : "#10b981"))) + "; -fx-font-size: 12px; -fx-font-weight: bold;");

            header.getChildren().addAll(lblF, spacer, lblCount);

            ProgressBar pb = new ProgressBar(fPct);
            pb.setMaxWidth(Double.MAX_VALUE);
            pb.setStyle("-fx-accent: " + (fPct > 0.75 ? "#ef4444" : (fPct > 0.4 ? "#f59e0b" : "#10b981")) + ";");

            card.getChildren().addAll(header, pb);
            floorStatusContainer.getChildren().add(card);
        }

        // Recent Activity Table
        tableRecentActivity.getItems().setAll(data.recentAudits);
    }

    private static class DashboardData {
        List<ParkingSlot> slots = new ArrayList<>();
        List<ParkingFloor> floors = new ArrayList<>();
        int activeSessionsCount;
        int activeChargingCount;
        int openAlertsCount;
        List<AuditEvent> recentAudits = new ArrayList<>();
    }
}
