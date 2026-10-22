package com.smartpark.controller;

import com.smartpark.model.ParkingRecord;
import com.smartpark.service.AuthenticationService;
import com.smartpark.service.ParkingService;
import com.smartpark.util.ParkingFeeCalculator.FeeBreakdown;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;

public class VehicleExitController {

    @FXML private TextField txtSearchQuery;
    @FXML private Label lblSearchFeedback;

    @FXML private Label lblDetailsTicket;
    @FXML private Label lblDetailsPlate;
    @FXML private Label lblDetailsSlot;
    @FXML private Label lblDetailsEntryTime;
    @FXML private Label lblDetailsExitTime;
    @FXML private Label lblDetailsDuration;

    @FXML private ComboBox<String> comboPaymentMethod;
    @FXML private TextField txtTotalFeeDisplay;
    @FXML private Button btnProcessExit;
    @FXML private Label lblCheckoutStatus;

    @FXML private Label lblBreakdownFirstHr;
    @FXML private Label lblBreakdownExtraHrs;
    @FXML private Label lblBreakdownEvSurcharge;
    @FXML private Label lblBreakdownGrace;
    @FXML private Label lblBreakdownTotal;

    @FXML private Label lblRcptNo;
    @FXML private Label lblRcptTicket;
    @FXML private Label lblRcptPlate;
    @FXML private Label lblRcptSlot;
    @FXML private Label lblRcptCheckIn;
    @FXML private Label lblRcptCheckOut;
    @FXML private Label lblRcptDuration;
    @FXML private Label lblRcptPayMethod;
    @FXML private Label lblRcptAmount;

    private final ParkingService parkingService = new ParkingService();
    private ParkingService.ExitDetails currentExitDetails = null;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        comboPaymentMethod.getItems().setAll("UPI / QR Code", "Credit / Debit Card", "Cash", "Fastag / RFID");
        comboPaymentMethod.setValue("UPI / QR Code");
        btnProcessExit.setDisable(true);
    }

    @FXML
    public void handleSearchSession() {
        lblCheckoutStatus.setText("");
        String query = txtSearchQuery.getText();
        if (query == null || query.isBlank()) {
            lblSearchFeedback.setText("Please enter a ticket ID or vehicle registration number.");
            lblSearchFeedback.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        currentExitDetails = parkingService.calculateExitDetails(query);
        if (currentExitDetails == null) {
            lblSearchFeedback.setText("No active parking record found for '" + query + "'.");
            lblSearchFeedback.setStyle("-fx-text-fill: #ef4444;");
            clearDetails();
            return;
        }

        lblSearchFeedback.setText("Active session found!");
        lblSearchFeedback.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

        ParkingRecord record = currentExitDetails.getRecord();
        FeeBreakdown fee = currentExitDetails.getBreakdown();

        lblDetailsTicket.setText(record.getTicketId());
        lblDetailsPlate.setText(record.getVehicleNumber());
        lblDetailsSlot.setText(record.getSlotNumber() + " (" + record.getFloor() + ")");
        lblDetailsEntryTime.setText(record.getEntryTime().format(DTF));
        lblDetailsExitTime.setText(currentExitDetails.getExitTime().format(DTF));
        lblDetailsDuration.setText(fee.getFormattedDuration() + " (" + fee.getActualMinutes() + " mins)");

        txtTotalFeeDisplay.setText("₹" + String.format("%.2f", fee.getTotalFee()));

        // Breakdown grid
        lblBreakdownFirstHr.setText("₹" + String.format("%.2f", fee.getFirstHourRate()));
        long extraHrs = Math.max(0, fee.getBilledHours() - 1);
        lblBreakdownExtraHrs.setText(extraHrs + " hrs @ ₹" + String.format("%.2f", fee.getAdditionalHourRate()) + "/hr");
        lblBreakdownEvSurcharge.setText(fee.getEvChargingSurcharge() > 0 ? "₹" + String.format("%.2f", fee.getEvChargingSurcharge()) : "₹0.00");
        lblBreakdownGrace.setText(fee.isGracePeriodApplied() ? "APPLIED (Duration <= 15m)" : "Not Applicable");
        lblBreakdownTotal.setText("₹" + String.format("%.2f", fee.getTotalFee()));

        btnProcessExit.setDisable(false);
    }

    @FXML
    public void handleProcessExitPayment() {
        if (currentExitDetails == null) return;

        ParkingRecord record = currentExitDetails.getRecord();
        FeeBreakdown fee = currentExitDetails.getBreakdown();
        String method = comboPaymentMethod.getValue();
        String operator = AuthenticationService.getInstance().isLoggedIn()
                ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                : "Cashier";

        boolean success = parkingService.processExitPayment(
                record, currentExitDetails.getExitTime(), (int) fee.getActualMinutes(),
                fee.getTotalFee(), method, operator
        );

        if (success) {
            // Update official receipt card
            String rcptId = "RCPT-" + System.currentTimeMillis() % 1000000;
            lblRcptNo.setText(rcptId);
            lblRcptTicket.setText(record.getTicketId());
            lblRcptPlate.setText(record.getVehicleNumber());
            lblRcptSlot.setText(record.getSlotNumber() + " (" + record.getFloor() + ")");
            lblRcptCheckIn.setText(record.getEntryTime().format(DTF));
            lblRcptCheckOut.setText(currentExitDetails.getExitTime().format(DTF));
            lblRcptDuration.setText(fee.getFormattedDuration());
            lblRcptPayMethod.setText(method);
            lblRcptAmount.setText("INR " + String.format("%.2f", fee.getTotalFee()) + " (PAID)");

            lblCheckoutStatus.setText("✓ Checkout completed! Slot " + record.getSlotNumber() + " is now released and AVAILABLE.");
            lblCheckoutStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Checkout Confirmed");
            alert.setHeaderText("Payment Succeeded (" + method + ")");
            alert.setContentText("Fee of ₹" + String.format("%.2f", fee.getTotalFee()) + " received.\nSlot " +
                    record.getSlotNumber() + " has been freed for incoming vehicles.");
            alert.showAndWait();

            btnProcessExit.setDisable(true);
            currentExitDetails = null;
        } else {
            lblCheckoutStatus.setText("Database error processing exit. Session remains active.");
            lblCheckoutStatus.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    private void clearDetails() {
        lblDetailsTicket.setText("--");
        lblDetailsPlate.setText("--");
        lblDetailsSlot.setText("--");
        lblDetailsEntryTime.setText("--");
        lblDetailsExitTime.setText("--");
        lblDetailsDuration.setText("--");
        txtTotalFeeDisplay.setText("₹0.00");
        lblBreakdownFirstHr.setText("₹0.00");
        lblBreakdownExtraHrs.setText("0 hrs @ ₹0.00");
        lblBreakdownEvSurcharge.setText("₹0.00");
        lblBreakdownGrace.setText("Not Applicable");
        lblBreakdownTotal.setText("₹0.00");
        btnProcessExit.setDisable(true);
    }
}
