package com.smartpark.enums;

public enum SlotStatus {
    AVAILABLE("Available", "🟢", "#10b981"),
    OCCUPIED("Occupied", "🔴", "#ef4444"),
    RESERVED("Reserved", "🟡", "#f59e0b");

    private final String label;
    private final String emoji;
    private final String colorHex;

    SlotStatus(String label, String emoji, String colorHex) {
        this.label = label;
        this.emoji = emoji;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getColorHex() {
        return colorHex;
    }
}