package com.smartpark.controller;

import com.smartpark.enums.VehicleType;
import com.smartpark.service.ReportService;
import com.smartpark.service.ReportService.AnalyticsSummary;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class AnalyticsController {

    @FXML private ComboBox<String> comboAnalyticsVehicleType;
    @FXML private Label lblReportStatus;

    @FXML private Label lblTotalRevenue;
    @FXML private Label lblCompletedCount;
    @FXML private Label lblAvgDuration;
    @FXML private Label lblReservationCount;

    @FXML private BarChart<String, Number> chartFloorOccupancy;
    @FXML private PieChart chartVehicleTypes;
    @FXML private BarChart<String, Number> chartDailyRevenue;

    private final ReportService reportService = new ReportService();

    @FXML
    public void initialize() {
        comboAnalyticsVehicleType.getItems().setAll("ALL", "CAR", "BIKE", "EV_CAR", "ACCESSIBLE_CAR");
        comboAnalyticsVehicleType.setValue("ALL");

        loadAnalytics();
    }

    @FXML
    public void loadAnalytics() {
        String typeFilter = comboAnalyticsVehicleType.getValue();

        Task<AnalyticsSummary> task = new Task<>() {
            @Override
            protected AnalyticsSummary call() {
                return reportService.computeAnalytics(null, null, typeFilter);
            }
        };

        task.setOnSucceeded(e -> {
            AnalyticsSummary s = task.getValue();
            updateChartsAndKpis(s);
        });

        Thread t = new Thread(task, "AnalyticsComputeThread");
        t.setDaemon(true);
        t.start();
    }

    private void updateChartsAndKpis(AnalyticsSummary s) {
        lblTotalRevenue.setText("₹" + String.format("%.2f", s.totalRevenue));
        lblCompletedCount.setText(String.valueOf(s.completedSessionsCount));
        lblAvgDuration.setText(String.format("%.0f mins", s.averageDurationMinutes));
        lblReservationCount.setText(String.valueOf(s.totalReservations));

        // 1. Floor Occupancy Bar Chart
        chartFloorOccupancy.getData().clear();
        XYChart.Series<String, Number> floorSeries = new XYChart.Series<>();
        floorSeries.setName("Occupied Slots");
        for (Map.Entry<String, Integer> entry : s.occupancyByFloor.entrySet()) {
            floorSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chartFloorOccupancy.getData().add(floorSeries);

        // 2. Vehicle Types Pie Chart
        chartVehicleTypes.getData().clear();
        for (Map.Entry<VehicleType, Integer> entry : s.countByVehicleType.entrySet()) {
            if (entry.getValue() > 0) {
                chartVehicleTypes.getData().add(new PieChart.Data(
                        entry.getKey().getDisplayName() + " (" + entry.getValue() + ")", entry.getValue()
                ));
            }
        }

        // 3. Daily Revenue Timeline Bar Chart
        chartDailyRevenue.getData().clear();
        XYChart.Series<String, Number> revSeries = new XYChart.Series<>();
        revSeries.setName("Revenue");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd");
        if (s.dailyRevenue.isEmpty()) {
            // Seed today with 0 for clean display
            revSeries.getData().add(new XYChart.Data<>(LocalDate.now().format(df), 0));
        } else {
            for (Map.Entry<LocalDate, Double> entry : s.dailyRevenue.entrySet()) {
                revSeries.getData().add(new XYChart.Data<>(entry.getKey().format(df), entry.getValue()));
            }
        }
        chartDailyRevenue.getData().add(revSeries);
    }

    @FXML
    public void handleExportCsv() {
        try {
            File exportDir = new File("reports");
            if (!exportDir.exists()) exportDir.mkdirs();

            String filename = "SmartPark_Report_" + System.currentTimeMillis() + ".csv";
            File target = new File(exportDir, filename);

            reportService.exportRecordsToCsv(target, null, null, comboAnalyticsVehicleType.getValue());

            lblReportStatus.setText("✓ Report exported: " + target.getName() + " in ./reports folder");
            lblReportStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        } catch (Exception ex) {
            lblReportStatus.setText("CSV export failed: " + ex.getMessage());
            lblReportStatus.setStyle("-fx-text-fill: #ef4444;");
        }
    }
}
