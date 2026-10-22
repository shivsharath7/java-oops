package com.smartpark.controller;

import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.enums.ReservationStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Reservation;
import com.smartpark.model.Vehicle;
import com.smartpark.service.AuthenticationService;
import com.smartpark.service.ParkingService;
import com.smartpark.service.ReservationService;
import com.smartpark.service.ReservationService.ReservationResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationController {

    @FXML private TextField txtResPlate;
    @FXML private ComboBox<VehicleType> comboResType;
    @FXML private ComboBox<ParkingSlot> comboResSlot;
    @FXML private TextField txtResName;
    @FXML private TextField txtResPhone;
    @FXML private ComboBox<Integer> comboHoldMinutes;
    @FXML private Label lblResFormStatus;

    @FXML private TableView<Reservation> tableReservations;
    @FXML private TableColumn<Reservation, String> colResCode;
    @FXML private TableColumn<Reservation, String> colResPlate;
    @FXML private TableColumn<Reservation, String> colResSlot;
    @FXML private TableColumn<Reservation, String> colResFloor;
    @FXML private TableColumn<Reservation, String> colResCustomer;
    @FXML private TableColumn<Reservation, String> colResExpires;
    @FXML private TableColumn<Reservation, String> colResStatus;

    @FXML private Button btnCheckIn;
    @FXML private Button btnCancel;
    @FXML private Label lblTableActionStatus;

    private final ReservationService reservationService = new ReservationService();
    private final ParkingService parkingService = new ParkingService();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        comboResType.getItems().setAll(VehicleType.values());
        comboResType.setValue(VehicleType.CAR);

        comboHoldMinutes.getItems().setAll(5, 10, 15, 30, 45, 60);
        comboHoldMinutes.setValue(15);

        setupTable();
        loadAvailableSlots();
        loadReservations();

        tableReservations.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean active = newV != null && newV.getStatus() == ReservationStatus.PENDING;
            btnCheckIn.setDisable(!active);
            btnCancel.setDisable(!active);
        });
    }

    private void setupTable() {
        colResCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReservationCode()));
        colResPlate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleNumber()));
        colResSlot.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlotNumber()));
        colResFloor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFloor() + " (" + c.getValue().getZone() + ")"));
        colResCustomer.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue().getCustomerName() != null ? c.getValue().getCustomerName() : "") +
                (c.getValue().getContactNumber() != null ? " (" + c.getValue().getContactNumber() + ")" : "")
        ));
        colResExpires.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getExpiresAt().format(DTF)));
        colResStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getLabel()));
    }

    @FXML
    public void handleCategoryChanged() {
        loadAvailableSlots();
    }

    private void loadAvailableSlots() {
        VehicleType type = comboResType.getValue() != null ? comboResType.getValue() : VehicleType.CAR;
        List<ParkingSlot> avail = slotDAO.getAvailableSlotsByType(type);
        comboResSlot.getItems().setAll(avail);
        if (!avail.isEmpty()) {
            comboResSlot.setValue(avail.get(0));
        }
    }

    @FXML
    public void loadReservations() {
        List<Reservation> list = reservationService.getAllReservations();
        tableReservations.getItems().setAll(list);
    }

    @FXML
    public void handleCreateReservation() {
        lblResFormStatus.setText("");
        String plate = txtResPlate.getText();
        ParkingSlot slot = comboResSlot.getValue();
        if (slot == null) {
            lblResFormStatus.setText("Please select an available parking slot.");
            lblResFormStatus.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        int mins = comboHoldMinutes.getValue() != null ? comboHoldMinutes.getValue() : 15;
        ReservationResult res = reservationService.createHold(
                plate, comboResType.getValue(), slot.getId(),
                txtResName.getText(), txtResPhone.getText(), mins
        );

        if (res.isSuccess()) {
            lblResFormStatus.setText("✓ " + res.getMessage());
            lblResFormStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            txtResPlate.clear();
            txtResName.clear();
            txtResPhone.clear();
            loadAvailableSlots();
            loadReservations();
        } else {
            lblResFormStatus.setText("✗ " + res.getMessage());
            lblResFormStatus.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void handleCheckIn() {
        Reservation sel = tableReservations.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getStatus() != ReservationStatus.PENDING) return;

        Vehicle v = new Vehicle(sel.getVehicleNumber(), sel.getVehicleType(), sel.getCustomerName(), sel.getContactNumber());
        String operator = AuthenticationService.getInstance().isLoggedIn()
                ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                : "Operator";

        ParkingService.EntryResult entry = parkingService.parkVehicle(v, sel.getSlotId(), operator, "Converted from reservation " + sel.getReservationCode());
        if (entry.isSuccess()) {
            new com.smartpark.dao.ReservationDAO().updateStatus(sel.getId(), ReservationStatus.CLAIMED);
            lblTableActionStatus.setText("✓ Vehicle checked in! Ticket issued: " + entry.getRecord().getTicketId());
            lblTableActionStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Check-In Complete");
            alert.setHeaderText("Vehicle Checked In");
            alert.setContentText("Reservation " + sel.getReservationCode() + " successfully converted to active parking ticket " +
                    entry.getRecord().getTicketId() + " in slot " + sel.getSlotNumber() + ".");
            alert.showAndWait();

            loadReservations();
            loadAvailableSlots();
        } else {
            lblTableActionStatus.setText("Check-in error: " + entry.getMessage());
            lblTableActionStatus.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void handleCancelReservation() {
        Reservation sel = tableReservations.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getStatus() != ReservationStatus.PENDING) return;

        String operator = AuthenticationService.getInstance().isLoggedIn()
                ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                : "Operator";

        boolean ok = reservationService.cancelReservation(sel.getId(), operator);
        if (ok) {
            lblTableActionStatus.setText("Reservation " + sel.getReservationCode() + " cancelled. Slot " + sel.getSlotNumber() + " freed.");
            lblTableActionStatus.setStyle("-fx-text-fill: #38bdf8;");
            loadReservations();
            loadAvailableSlots();
        }
    }

    @FXML
    public void handleSweepExpired() {
        int swept = reservationService.releaseExpiredReservations();
        lblTableActionStatus.setText("Sweep completed: " + swept + " expired holds released.");
        lblTableActionStatus.setStyle("-fx-text-fill: #10b981;");
        loadReservations();
        loadAvailableSlots();
    }
}
