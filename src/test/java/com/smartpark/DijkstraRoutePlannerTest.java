package com.smartpark;

import com.smartpark.algorithm.DijkstraRoutePlanner;
import com.smartpark.model.PathResult;
import com.smartpark.model.RouteEdge;
import com.smartpark.model.RouteNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dijkstra Route Planner Algorithm Tests")
public class DijkstraRoutePlannerTest {

    private DijkstraRoutePlanner planner;

    @BeforeEach
    void setup() {
        List<RouteNode> nodes = new ArrayList<>();
        nodes.add(new RouteNode("N_SLOT_GA05", "Ground Floor", "Slot G-A05", "SLOT", 150, 130));
        nodes.add(new RouteNode("N_AISLE_GA", "Ground Floor", "Zone A Aisle", "AISLE", 150, 100));
        nodes.add(new RouteNode("N_AISLE_GB", "Ground Floor", "Zone B Aisle", "AISLE", 350, 100));
        nodes.add(new RouteNode("N_EXIT_NORTH", "Ground Floor", "North Exit Gate", "EXIT", 450, 50));
        nodes.add(new RouteNode("N_EXIT_SOUTH", "Ground Floor", "South Exit Gate", "EXIT", 450, 150));
        nodes.add(new RouteNode("N_ISOLATED", "Basement 2", "Unreachable Node", "SLOT", 0, 0));

        List<RouteEdge> edges = new ArrayList<>();
        // GA05 -> Aisle GA (6m)
        edges.add(new RouteEdge("N_SLOT_GA05", "N_AISLE_GA", 6, "Back out into Zone A aisle"));
        // Aisle GA -> Aisle GB (30m)
        edges.add(new RouteEdge("N_AISLE_GA", "N_AISLE_GB", 30, "Follow central corridor to Zone B"));
        // Aisle GB -> North Exit (20m)
        edges.add(new RouteEdge("N_AISLE_GB", "N_EXIT_NORTH", 20, "Turn left towards North Exit Gate"));
        // Aisle GB -> South Exit (25m)
        edges.add(new RouteEdge("N_AISLE_GB", "N_EXIT_SOUTH", 25, "Turn right towards South Exit Gate"));

        // Direct express link from Aisle GA to North Exit (45m)
        edges.add(new RouteEdge("N_AISLE_GA", "N_EXIT_NORTH", 45, "Take direct overhead express ramp"));

        planner = new DijkstraRoutePlanner(nodes, edges);
    }

    @Test
    @DisplayName("Should find shortest path between slot and North Exit gate")
    void testShortestRouteToNorthExit() {
        // Path 1: GA05 -> GA -> GB -> NORTH = 6 + 30 + 20 = 56m
        // Path 2: GA05 -> GA -> NORTH = 6 + 45 = 51m (Shorter!)
        PathResult result = planner.findShortestRoute("N_SLOT_GA05", "N_EXIT_NORTH");

        assertTrue(result.isFound());
        assertEquals(51, result.getTotalDistanceMeters());
        assertEquals(3, result.getPathNodes().size()); // GA05 -> GA -> NORTH
        assertEquals(2, result.getTurnByTurnInstructions().size());
        assertTrue(result.getEstimatedWalkingMinutes() >= 1);
    }

    @Test
    @DisplayName("Should find shortest path to South Exit gate")
    void testShortestRouteToSouthExit() {
        // Path: GA05 -> GA -> GB -> SOUTH = 6 + 30 + 25 = 61m
        PathResult result = planner.findShortestRoute("N_SLOT_GA05", "N_EXIT_SOUTH");

        assertTrue(result.isFound());
        assertEquals(61, result.getTotalDistanceMeters());
        assertEquals(4, result.getPathNodes().size()); // GA05 -> GA -> GB -> SOUTH
    }

    @Test
    @DisplayName("Should return notFound when destination is unreachable")
    void testUnreachableNode() {
        PathResult result = planner.findShortestRoute("N_SLOT_GA05", "N_ISOLATED");
        assertFalse(result.isFound());
        assertTrue(result.getTurnByTurnInstructions().get(0).contains("No accessible route found"));
    }

    @Test
    @DisplayName("Should return notFound when starting node does not exist")
    void testNonExistentNode() {
        PathResult result = planner.findShortestRoute("NON_EXISTENT_NODE", "N_EXIT_NORTH");
        assertFalse(result.isFound());
    }
}
