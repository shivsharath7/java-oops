package com.smartpark.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of shortest path calculation through the parking complex.
 */
public class PathResult {
    private final boolean found;
    private final int totalDistanceMeters;
    private final List<RouteNode> pathNodes;
    private final List<String> turnByTurnInstructions;
    private final String destinationExitName;

    public PathResult(boolean found, int totalDistanceMeters, List<RouteNode> pathNodes,
                      List<String> turnByTurnInstructions, String destinationExitName) {
        this.found = found;
        this.totalDistanceMeters = totalDistanceMeters;
        this.pathNodes = pathNodes != null ? pathNodes : new ArrayList<>();
        this.turnByTurnInstructions = turnByTurnInstructions != null ? turnByTurnInstructions : new ArrayList<>();
        this.destinationExitName = destinationExitName;
    }

    public static PathResult notFound(String message) {
        List<String> list = new ArrayList<>();
        list.add(message);
        return new PathResult(false, 0, new ArrayList<>(), list, "");
    }

    public boolean isFound() { return found; }
    public int getTotalDistanceMeters() { return totalDistanceMeters; }
    public List<RouteNode> getPathNodes() { return pathNodes; }
    public List<String> getTurnByTurnInstructions() { return turnByTurnInstructions; }
    public String getDestinationExitName() { return destinationExitName; }

    public int getEstimatedWalkingMinutes() {
        // Average walking speed ~ 1.4 m/s => ~84 meters per min
        return Math.max(1, (int) Math.ceil(totalDistanceMeters / 70.0));
    }
}
