package com.smartpark.service;

import com.smartpark.dao.AuditDAO;
import com.smartpark.dao.DisputeDAO;
import com.smartpark.dao.ParkingRecordDAO;
import com.smartpark.enums.DisputeStatus;
import com.smartpark.model.AuditEvent;
import com.smartpark.model.ParkingDispute;
import com.smartpark.model.ParkingRecord;

import java.util.List;

public class DisputeService {

    private final DisputeDAO disputeDAO = new DisputeDAO();
    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    public List<ParkingDispute> getAllDisputes() {
        return disputeDAO.getAllDisputes();
    }

    public boolean createDispute(String ticketOrPlate, String category, String description,
                                 double disputedFee, String operator) {
        ParkingRecord record = recordDAO.findRecordByTicketOrPlate(ticketOrPlate);
        ParkingDispute d = new ParkingDispute();
        if (record != null) {
            d.setRecordId(record.getId());
            d.setTicketId(record.getTicketId());
            d.setVehicleNumber(record.getVehicleNumber());
            if (disputedFee <= 0) disputedFee = record.getParkingFee();
        } else {
            d.setTicketId(ticketOrPlate);
            d.setVehicleNumber(ticketOrPlate);
        }
        d.setCategory(category);
        d.setDescription(description);
        d.setDisputedFee(disputedFee);
        d.setStatus(DisputeStatus.OPEN);

        boolean created = disputeDAO.createDispute(d);
        if (created) {
            auditDAO.logEvent(new AuditEvent("DISPUTE_FILED", d.getTicketId(),
                    "Dispute filed under category '" + category + "'. Disputed fee: ₹" + disputedFee, operator));
            return true;
        }
        return false;
    }

    public boolean resolveDispute(int disputeId, DisputeStatus newStatus, String resolutionNotes, String operator) {
        boolean updated = disputeDAO.updateDisputeStatus(disputeId, newStatus, resolutionNotes, operator);
        if (updated) {
            auditDAO.logEvent(new AuditEvent("DISPUTE_RESOLVED", "Dispute-" + disputeId,
                    "Dispute marked " + newStatus.name() + ". Notes: " + resolutionNotes, operator));
            return true;
        }
        return false;
    }
}
