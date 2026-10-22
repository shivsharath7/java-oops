package com.smartpark.controller;

import com.smartpark.dao.ParkingRecordDAO;
import com.smartpark.enums.DisputeStatus;
import com.smartpark.model.ParkingDispute;
import com.smartpark.model.ParkingRecord;
import com.smartpark.service.AuthenticationService;
import com.smartpark.service.DisputeService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ParkingHistoryController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboStatus;
    @FXML private ComboBox<String> comboType;
    @FXML private Label lblRecordCount;

    @FXML private TableView<ParkingRecord> tableRecords;
    @FXML private TableColumn<ParkingRecord, String> colTicket;
    @FXML private TableColumn<ParkingRecord, String> colPlate;
    @FXML private TableColumn<ParkingRecord, String> colType;
    @FXML private TableColumn<ParkingRecord, String> colSlot;
    @FXML private TableColumn<ParkingRecord, String> colFloor;
    @FXML private TableColumn<ParkingRecord, String> colEntry;
    @FXML private TableColumn<ParkingRecord, String> colExit;
    @FXML private TableColumn<ParkingRecord, String> colDuration;
    @FXML private TableColumn<ParkingRecord, String> colFee;
    @FXML private TableColumn<ParkingRecord, String> colPayStatus;
    @FXML private TableColumn<ParkingRecord, String> colStatus;

    @FXML private Button btnFileDispute;
    @FXML private Button btnViewReceipt;
    @FXML private Label lblHistoryActionStatus;

    @FXML private TableView<ParkingDispute> tableDisputes;
    @FXML private TableColumn<ParkingDispute, String> colDispId;
    @FXML private TableColumn<ParkingDispute, String> colDispTicket;
    @FXML private TableColumn<ParkingDispute, String> colDispPlate;
    @FXML private TableColumn<ParkingDispute, String> colDispCat;
    @FXML private TableColumn<ParkingDispute, String> colDispDesc;
    @FXML private TableColumn<ParkingDispute, String> colDispFee;
    @FXML private TableColumn<ParkingDispute, String> colDispStatus;
    @FXML private TableColumn<ParkingDispute, String> colDispDate;

    @FXML private Button btnResolveDispute;
    @FXML private Button btnRejectDispute;
    @FXML private Label lblDisputeActionFeedback;

    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();
    private final DisputeService disputeService = new DisputeService();

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        comboStatus.getItems().setAll("ALL", "ACTIVE", "COMPLETED", "DISPUTED");
        comboStatus.setValue("ALL");

        comboType.getItems().setAll("ALL", "CAR", "BIKE", "EV_CAR", "ACCESSIBLE_CAR");
        comboType.setValue("ALL");

        setupRecordTable();
        setupDisputeTable();

        loadRecords();
        loadDisputes();

        tableRecords.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            btnFileDispute.setDisable(newV == null);
            btnViewReceipt.setDisable(newV == null);
        });

        tableDisputes.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean canAct = newV != null && newV.getStatus() == DisputeStatus.OPEN;
            btnResolveDispute.setDisable(!canAct);
            btnRejectDispute.setDisable(!canAct);
        });

        btnFileDispute.setDisable(true);
        btnViewReceipt.setDisable(true);
        btnResolveDispute.setDisable(true);
        btnRejectDispute.setDisable(true);
    }

    private void setupRecordTable() {
        colTicket.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTicketId()));
        colPlate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleNumber()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleType() != null ? c.getValue().getVehicleType().name() : "--"));
        colSlot.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlotNumber()));
        colFloor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFloor() + " (" + c.getValue().getZone() + ")"));
        colEntry.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEntryTime() != null ? c.getValue().getEntryTime().format(DTF) : "--"));
        colExit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getExitTime() != null ? c.getValue().getExitTime().format(DTF) : "--"));
        colDuration.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDurationMinutes())));
        colFee.setCellValueFactory(c -> new SimpleStringProperty("₹" + String.format("%.2f", c.getValue().getParkingFee())));
        colPayStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentStatus() != null ? c.getValue().getPaymentStatus().getLabel() : "--"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }

    private void setupDisputeTable() {
        colDispId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colDispTicket.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTicketId()));
        colDispPlate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleNumber()));
        colDispCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colDispDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colDispFee.setCellValueFactory(c -> new SimpleStringProperty("₹" + String.format("%.2f", c.getValue().getDisputedFee())));
        colDispStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getLabel()));
        colDispDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(DTF) : "--"));
    }

    @FXML
    public void loadRecords() {
        String query = txtSearch.getText();
        String status = comboStatus.getValue();
        String type = comboType.getValue();

        List<ParkingRecord> list = recordDAO.getRecordsByFilter(query, status, type, null, null);
        tableRecords.getItems().setAll(list);
        lblRecordCount.setText("Showing " + list.size() + " records");
    }

    @FXML
    public void loadDisputes() {
        List<ParkingDispute> list = disputeService.getAllDisputes();
        tableDisputes.getItems().setAll(list);
    }

    @FXML
    public void handleFileDispute() {
        ParkingRecord sel = tableRecords.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("File Customer Parking Dispute");
        dialog.setHeaderText("Filing dispute on Ticket: " + sel.getTicketId() + " (" + sel.getVehicleNumber() + ")");

        ButtonType submitBtn = new ButtonType("Submit Dispute", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ComboBox<String> comboCat = new ComboBox<>();
        comboCat.getItems().setAll(
                "Fee Overcharge / Calculation Error",
                "Timestamp / Duration Mismatch",
                "Grace Period Exemption Not Honored",
                "EV Charging Interruption / Damage",
                "Vehicle Scratch / Facility Incident",
                "Other Dispute"
        );
        comboCat.setValue("Fee Overcharge / Calculation Error");

        TextArea txtDesc = new TextArea();
        txtDesc.setPromptText("Enter detailed customer dispute notes...");
        txtDesc.setPrefRowCount(3);

        TextField txtFee = new TextField(String.valueOf(sel.getParkingFee()));

        grid.add(new Label("Category:"), 0, 0);
        grid.add(comboCat, 1, 0);
        grid.add(new Label("Disputed Amount (INR):"), 0, 1);
        grid.add(txtFee, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(txtDesc, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == submitBtn) {
            double fee = sel.getParkingFee();
            try {
                fee = Double.parseDouble(txtFee.getText().trim());
            } catch (Exception ignored) {}

            String operator = AuthenticationService.getInstance().isLoggedIn()
                    ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                    : "Operator";

            boolean ok = disputeService.createDispute(sel.getTicketId(), comboCat.getValue(), txtDesc.getText(), fee, operator);
            if (ok) {
                lblHistoryActionStatus.setText("✓ Dispute successfully recorded against " + sel.getTicketId());
                lblHistoryActionStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                loadDisputes();
            }
        }
    }

    @FXML
    public void handleViewReceipt() {
        ParkingRecord sel = tableRecords.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Official Digital Receipt");
        alert.setHeaderText("Tax Invoice Receipt: " + sel.getTicketId());

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("       SMARTPARK INTELLIGENT PARKING SYSTEM         \n");
        sb.append("                 TAX INVOICE RECEIPT                \n");
        sb.append("====================================================\n");
        sb.append("Ticket ID:       ").append(sel.getTicketId()).append("\n");
        sb.append("Vehicle Number:  ").append(sel.getVehicleNumber()).append("\n");
        sb.append("Category:        ").append(sel.getVehicleType() != null ? sel.getVehicleType().name() : "CAR").append("\n");
        sb.append("Assigned Bay:    ").append(sel.getSlotNumber()).append(" (").append(sel.getFloor()).append(")\n");
        sb.append("Check-In Time:   ").append(sel.getEntryTime() != null ? sel.getEntryTime().format(DTF) : "--").append("\n");
        sb.append("Check-Out Time:  ").append(sel.getExitTime() != null ? sel.getExitTime().format(DTF) : "ACTIVE (Not checked out)").append("\n");
        sb.append("Duration Billed: ").append(sel.getDurationMinutes()).append(" Minutes\n");
        sb.append("Payment Method:  ").append(sel.getPaymentMethod()).append("\n");
        sb.append("Payment Status:  ").append(sel.getPaymentStatus() != null ? sel.getPaymentStatus().getLabel() : "").append("\n");
        sb.append("----------------------------------------------------\n");
        sb.append("TOTAL PAID:      INR ").append(String.format("%.2f", sel.getParkingFee())).append("\n");
        sb.append("====================================================\n");

        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    @FXML
    public void handleApproveDispute() {
        ParkingDispute sel = tableDisputes.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getStatus() != DisputeStatus.OPEN) return;

        String operator = AuthenticationService.getInstance().isLoggedIn()
                ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                : "Operator";

        boolean ok = disputeService.resolveDispute(sel.getId(), DisputeStatus.RESOLVED, "Fee adjusted/waived by supervisor.", operator);
        if (ok) {
            lblDisputeActionFeedback.setText("✓ Dispute #" + sel.getId() + " marked RESOLVED.");
            lblDisputeActionFeedback.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            loadDisputes();
        }
    }

    @FXML
    public void handleRejectDispute() {
        ParkingDispute sel = tableDisputes.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getStatus() != DisputeStatus.OPEN) return;

        String operator = AuthenticationService.getInstance().isLoggedIn()
                ? AuthenticationService.getInstance().getCurrentAdmin().getUsername()
                : "Operator";

        boolean ok = disputeService.resolveDispute(sel.getId(), DisputeStatus.REJECTED, "Dispute rejected following audit review.", operator);
        if (ok) {
            lblDisputeActionFeedback.setText("Dispute #" + sel.getId() + " marked REJECTED.");
            lblDisputeActionFeedback.setStyle("-fx-text-fill: #ef4444;");
            loadDisputes();
        }
    }
}
