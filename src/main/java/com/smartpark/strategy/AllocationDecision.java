package com.smartpark.strategy;

import com.smartpark.model.ParkingSlot;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of the allocation algorithm execution.
 */
public class AllocationDecision {
    private final ParkingSlot recommendedSlot;
    private final String explanation;
    private final List<AllocationCandidate> topCandidates;
    private final String strategyName;

    public AllocationDecision(ParkingSlot recommendedSlot, String explanation,
                              List<AllocationCandidate> topCandidates, String strategyName) {
        this.recommendedSlot = recommendedSlot;
        this.explanation = explanation;
        this.topCandidates = topCandidates != null ? topCandidates : new ArrayList<>();
        this.strategyName = strategyName;
    }

    public static AllocationDecision noSlotAvailable(String reason, String strategyName) {
        return new AllocationDecision(null, reason, new ArrayList<>(), strategyName);
    }

    public ParkingSlot getRecommendedSlot() { return recommendedSlot; }
    public String getExplanation() { return explanation; }
    public List<AllocationCandidate> getTopCandidates() { return topCandidates; }
    public String getStrategyName() { return strategyName; }
    public boolean hasRecommendation() { return recommendedSlot != null; }
}
