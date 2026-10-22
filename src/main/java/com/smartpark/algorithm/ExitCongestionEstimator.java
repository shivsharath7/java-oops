package com.smartpark.algorithm;

import com.smartpark.model.PathResult;
import com.smartpark.model.RouteNode;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, rule-based exit congestion estimator.
 * Evaluates route lengths, zone occupancies, and peak-hour traffic multipliers
 * to recommend the most convenient exit gate.
 */
public class ExitCongestionEstimator {

    public static class ExitRecommendation {
        private final RouteNode exitNode;
        private final PathResult pathResult;
        private final double estimatedDelayScore;
        private final String congestionLevel; // LOW, MODERATE, HIGH
        private final String rationale;

        public ExitRecommendation(RouteNode exitNode, PathResult pathResult, double estimatedDelayScore,
                                  String congestionLevel, String rationale) {
            this.exitNode = exitNode;
            this.pathResult = pathResult;
            this.estimatedDelayScore = estimatedDelayScore;
            this.congestionLevel = congestionLevel;
            this.rationale = rationale;
        }

        public RouteNode getExitNode() { return exitNode; }
        public PathResult getPathResult() { return pathResult; }
        public double getEstimatedDelayScore() { return estimatedDelayScore; }
        public String getCongestionLevel() { return congestionLevel; }
        public String getRationale() { return rationale; }
    }

    private final DijkstraRoutePlanner routePlanner;

    public ExitCongestionEstimator(DijkstraRoutePlanner routePlanner) {
        this.routePlanner = routePlanner;
    }

    public List<ExitRecommendation> evaluateExits(String startNodeId, List<RouteNode> availableExits,
                                                  Map<String, Double> zoneOccupancyMap,
                                                  LocalTime morningPeakStart, LocalTime morningPeakEnd,
                                                  LocalTime eveningPeakStart, LocalTime eveningPeakEnd) {
        List<ExitRecommendation> list = new ArrayList<>();
        if (availableExits == null || availableExits.isEmpty()) return list;

        LocalTime now = LocalTime.now();
        boolean isPeakHour = (now.isAfter(morningPeakStart) && now.isBefore(morningPeakEnd)) ||
                             (now.isAfter(eveningPeakStart) && now.isBefore(eveningPeakEnd));

        for (RouteNode exit : availableExits) {
            PathResult path = routePlanner.findShortestRoute(startNodeId, exit.getNodeId());
            if (!path.isFound()) continue;

            // Route base delay proportional to walking/transit distance
            double distanceFactor = path.getTotalDistanceMeters() * 0.5;

            // Exit specific traffic factor (North express exit usually clearer during peak)
            double exitBaseCongestion = exit.getName().toLowerCase().contains("north") ? 20.0 : 35.0;
            if (isPeakHour) {
                exitBaseCongestion *= 1.4; // Peak-hour multiplier
            }

            double totalScore = distanceFactor + exitBaseCongestion;
            String level = totalScore > 65 ? "HIGH" : (totalScore > 35 ? "MODERATE" : "LOW");

            StringBuilder rationale = new StringBuilder();
            rationale.append(exit.getName()).append(": ");
            rationale.append("Route distance is ").append(path.getTotalDistanceMeters()).append("m. ");
            if (isPeakHour) {
                rationale.append("Operating in configured Peak-Hour period (moderate exit queue). ");
            } else {
                rationale.append("Off-peak smooth outflow. ");
            }
            rationale.append("Estimated exit level is ").append(level).append(".");

            list.add(new ExitRecommendation(exit, path, totalScore, level, rationale.toString()));
        }

        list.sort(Comparator.comparingDouble(ExitRecommendation::getEstimatedDelayScore));
        return list;
    }
}
