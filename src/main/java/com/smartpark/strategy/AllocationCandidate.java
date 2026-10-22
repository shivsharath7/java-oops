package com.smartpark.strategy;

import com.smartpark.model.ParkingSlot;

/**
 * Represents a ranked slot candidate with detailed score breakdown and explanation.
 */
public class AllocationCandidate {
    private final ParkingSlot slot;
    private final double totalScore;
    private final double congestionScore;
    private final double distanceScore;
    private final double preferenceScore;
    private final String reason;

    public AllocationCandidate(ParkingSlot slot, double totalScore, double congestionScore,
                               double distanceScore, double preferenceScore, String reason) {
        this.slot = slot;
        this.totalScore = Math.round(totalScore * 10.0) / 10.0;
        this.congestionScore = Math.round(congestionScore * 10.0) / 10.0;
        this.distanceScore = Math.round(distanceScore * 10.0) / 10.0;
        this.preferenceScore = Math.round(preferenceScore * 10.0) / 10.0;
        this.reason = reason;
    }

    public ParkingSlot getSlot() { return slot; }
    public double getTotalScore() { return totalScore; }
    public double getCongestionScore() { return congestionScore; }
    public double getDistanceScore() { return distanceScore; }
    public double getPreferenceScore() { return preferenceScore; }
    public String getReason() { return reason; }
}
