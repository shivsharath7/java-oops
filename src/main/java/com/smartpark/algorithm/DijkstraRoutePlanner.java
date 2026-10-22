package com.smartpark.algorithm;

import com.smartpark.model.PathResult;
import com.smartpark.model.RouteEdge;
import com.smartpark.model.RouteNode;

import java.util.*;

/**
 * Dijkstra's shortest path graph algorithm for indoor parking navigation.
 * Demonstrates graph algorithm implementation, PriorityQueue, adjacency lists,
 * and deterministic route reconstruction.
 */
public class DijkstraRoutePlanner {

    private final Map<String, RouteNode> nodeMap = new HashMap<>();
    private final Map<String, List<RouteEdge>> adjacencyMap = new HashMap<>();

    public DijkstraRoutePlanner(List<RouteNode> nodes, List<RouteEdge> edges) {
        if (nodes != null) {
            for (RouteNode n : nodes) {
                nodeMap.put(n.getNodeId(), n);
                adjacencyMap.put(n.getNodeId(), new ArrayList<>());
            }
        }
        if (edges != null) {
            for (RouteEdge e : edges) {
                if (adjacencyMap.containsKey(e.getFromNode())) {
                    adjacencyMap.get(e.getFromNode()).add(e);
                }
            }
        }
    }

    public PathResult findShortestRoute(String startNodeId, String destinationExitNodeId) {
        if (startNodeId == null || !nodeMap.containsKey(startNodeId)) {
            return PathResult.notFound("Starting navigation node '" + startNodeId + "' not found in complex map.");
        }
        if (destinationExitNodeId == null || !nodeMap.containsKey(destinationExitNodeId)) {
            return PathResult.notFound("Destination exit '" + destinationExitNodeId + "' not found in complex map.");
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNode = new HashMap<>();
        Map<String, RouteEdge> previousEdge = new HashMap<>();

        for (String id : nodeMap.keySet()) {
            distances.put(id, Integer.MAX_VALUE);
        }
        distances.put(startNodeId, 0);

        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingInt(nd -> nd.distance));
        pq.add(new NodeDistance(startNodeId, 0));

        Set<String> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String u = current.nodeId;

            if (visited.contains(u)) continue;
            visited.add(u);

            if (u.equals(destinationExitNodeId)) {
                break; // Found destination!
            }

            List<RouteEdge> neighbors = adjacencyMap.getOrDefault(u, Collections.emptyList());
            for (RouteEdge edge : neighbors) {
                String v = edge.getToNode();
                if (visited.contains(v)) continue;

                int weight = edge.getDistanceWeight();
                int newDist = distances.get(u) + weight;
                if (newDist < distances.get(v)) {
                    distances.put(v, newDist);
                    previousNode.put(v, u);
                    previousEdge.put(v, edge);
                    pq.add(new NodeDistance(v, newDist));
                }
            }
        }

        if (distances.get(destinationExitNodeId) == Integer.MAX_VALUE) {
            return PathResult.notFound("No accessible route found between " +
                    nodeMap.get(startNodeId).getName() + " and " + nodeMap.get(destinationExitNodeId).getName() + ".");
        }

        // Reconstruct path
        LinkedList<RouteNode> path = new LinkedList<>();
        LinkedList<String> instructions = new LinkedList<>();

        String curr = destinationExitNodeId;
        while (curr != null) {
            path.addFirst(nodeMap.get(curr));
            RouteEdge edge = previousEdge.get(curr);
            if (edge != null) {
                instructions.addFirst(edge.getDirectionInfo() + " (" + edge.getDistanceWeight() + "m)");
            }
            curr = previousNode.get(curr);
        }

        String exitName = nodeMap.get(destinationExitNodeId).getName();
        return new PathResult(true, distances.get(destinationExitNodeId), path, instructions, exitName);
    }

    private static class NodeDistance {
        final String nodeId;
        final int distance;

        NodeDistance(String nodeId, int distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }
}
