package com.smartpark.enums;

public enum PaymentStatus {
    PENDING("Payment Pending", "#f59e0b"),
    PAID("Paid", "#10b981"),
    WAIVED("Fee Waived", "#64748b"),
    DISPUTED("Disputed", "#ef4444");

    private final String label;
    private final String colorHex;

    PaymentStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }
}