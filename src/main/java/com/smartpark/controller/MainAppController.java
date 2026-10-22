package com.smartpark.controller;

import com.smartpark.model.Admin;
import com.smartpark.model.ParkingSlot;
import com.smartpark.service.AuthenticationService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class MainAppController {

    @FXML private Button btnDashboard;
    @FXML private Button btnMap;
    @FXML private Button btnEntry;
    @FXML private Button btnExit;
    @FXML private Button btnFind;
    @FXML private Button btnAlloc;
    @FXML private Button btnReservations;
    @FXML private Button btnEv;
    @FXML private Button btnAnomalies;
    @FXML private Button btnHistory;
    @FXML private Button btnReports;
    @FXML private Button btnAdmin;

    @FXML private Label lblHeaderTitle;
    @FXML private Label lblHeaderSubtitle;
    @FXML private Label lblClock;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    @FXML private StackPane contentContainer;

    private Object activeViewController;
    private Button currentActiveBtn;

    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    @FXML
    public void initialize() {
        startDigitalClock();
        updateUserHeader();

        // Load initial Dashboard
        handleNavDashboard();
    }

    private void startDigitalClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e ->
                lblClock.setText(LocalDateTime.now().format(CLOCK_FORMAT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public void updateUserHeader() {
        Admin admin = AuthenticationService.getInstance().getCurrentAdmin();
        if (admin != null) {
            lblUserName.setText(admin.getFullName());
            lblUserRole.setText(admin.getRole());
        } else {
            lblUserName.setText("Shift Operator");
            lblUserRole.setText("OPERATOR");
        }
    }

    private void setActiveButton(Button btn) {
        if (currentActiveBtn != null) {
            currentActiveBtn.getStyleClass().remove("nav-btn-active");
        }
        this.currentActiveBtn = btn;
        if (btn != null && !btn.getStyleClass().contains("nav-btn-active")) {
            btn.getStyleClass().add("nav-btn-active");
        }
    }

    private void loadView(String fxmlPath, String title, String subtitle, Button navBtn) {
        try {
            setActiveButton(navBtn);
            lblHeaderTitle.setText(title);
            lblHeaderSubtitle.setText(subtitle);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            this.activeViewController = loader.getController();

            if (activeViewController instanceof ParkingMapController mapCtrl) {
                mapCtrl.setMainController(this);
            }

            contentContainer.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("[MainAppController Error] Failed to load view " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML public void handleNavDashboard() {
        loadView("/fxml/dashboard_view.fxml", "Live Operations Dashboard",
                "Real-time multi-storey capacity, floor congestion metrics, and operational activity", btnDashboard);
    }

    @FXML public void handleNavMap() {
        loadView("/fxml/parking_map_view.fxml", "Interactive Multi-Floor Parking Map",
                "Live graphical slot grid with real-time status colors, vehicle assignments, and operator actions", btnMap);
    }

    @FXML public void handleNavEntry() {
        loadView("/fxml/vehicle_entry_view.fxml", "Vehicle Check-In & Smart Allocation",
                "Register incoming vehicle, evaluate optimal slot using algorithm, and issue digital parking ticket", btnEntry);
    }

    public void navigateToEntryWithSlot(ParkingSlot slot) {
        loadView("/fxml/vehicle_entry_view.fxml", "Vehicle Check-In & Smart Allocation",
                "Register incoming vehicle, evaluate optimal slot using algorithm, and issue digital parking ticket", btnEntry);
        if (activeViewController instanceof VehicleEntryController entryCtrl) {
            entryCtrl.preselectSlot(slot);
        }
    }

    @FXML public void handleNavExit() {
        loadView("/fxml/vehicle_exit_view.fxml", "Vehicle Check-Out & Payment Settlement",
                "Search active parking session, calculate duration and tiered tariffs, settle payment, and release bay", btnExit);
    }

    @FXML public void handleNavFind() {
        loadView("/fxml/find_vehicle_view.fxml", "Find My Parked Vehicle & Indoor Exit Route",
                "Locate parked vehicle across complex floors and compute step-by-step navigation path to nearest exit gate", btnFind);
    }

    @FXML public void handleNavAlloc() {
        loadView("/fxml/smart_allocation_view.fxml", "Congestion-Aware Smart Allocation Engine",
                "Compare standard entrance-first allocation versus weighted multi-factor congestion optimization", btnAlloc);
    }

    @FXML public void handleNavReservations() {
        loadView("/fxml/reservations_view.fxml", "Temporary Slot Reservations & Auto-Release",
                "Place timed holds on parking bays with automatic background expiry release and arrival check-in", btnReservations);
    }

    @FXML public void handleNavEv() {
        loadView("/fxml/ev_charging_view.fxml", "EV Fast-Charging Bays & Session Management",
                "Manage electric vehicle charging hardware, monitor active power delivery sessions, and flag overstay delays", btnEv);
    }

    @FXML public void handleNavAnomalies() {
        loadView("/fxml/anomalies_view.fxml", "Security, Alerts & Parking Anomaly Detection",
                "Automated consistency scanning across physical database records to detect ghost sessions, expired holds, and conflicting slots", btnAnomalies);
    }

    @FXML public void handleNavHistory() {
        loadView("/fxml/parking_history_view.fxml", "Parking Records History & Dispute Management",
                "Audit trail of completed sessions, fee collections, receipts, and customer dispute resolution", btnHistory);
    }

    @FXML public void handleNavReports() {
        loadView("/fxml/analytics_view.fxml", "Business Analytics & Management Reports",
                "Statistical charts, financial revenue trends, multi-storey capacity utilization, and CSV report exports", btnReports);
    }

    @FXML public void handleNavAdmin() {
        loadView("/fxml/admin_settings_view.fxml", "System Administration & Policy Settings",
                "Configure dynamic parking tariffs, traffic congestion thresholds, slot layouts, and alternative parking facilities", btnAdmin);
    }

    @FXML
    public void handleGlobalRefresh() {
        if (activeViewController instanceof DashboardController c) c.refreshData();
        else if (activeViewController instanceof ParkingMapController c) c.loadSlots();
        else if (activeViewController instanceof ReservationController c) c.loadReservations();
        else if (activeViewController instanceof EvChargingController c) c.loadData();
        else if (activeViewController instanceof AnomaliesController c) c.loadAnomalies();
        else if (activeViewController instanceof ParkingHistoryController c) c.loadRecords();
        else if (activeViewController instanceof AnalyticsController c) c.loadAnalytics();
        else if (activeViewController instanceof AdminSettingsController c) c.loadAllSettings();
    }

    @FXML
    public void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Logout");
        confirm.setHeaderText("Log out of SmartPark Console?");
        confirm.setContentText("You will need to re-authenticate with your administrator or operator credentials.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            AuthenticationService.getInstance().logout();
            openLoginModal();
        }
    }

    public void openLoginModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login_dialog.fxml"));
            Parent root = loader.load();
            LoginDialogController ctrl = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("SmartPark — Operator Authentication");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            if (ctrl.isLoginSuccessful()) {
                updateUserHeader();
                handleNavDashboard();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
