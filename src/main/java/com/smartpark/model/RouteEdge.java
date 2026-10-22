package com.smartpark.model;

public class RouteEdge {
    private int id;
    private String fromNode;
    private String toNode;
    private int distanceWeight;
    private String directionInfo;

    public RouteEdge() {}

    public RouteEdge(String fromNode, String toNode, int distanceWeight, String directionInfo) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.distanceWeight = distanceWeight;
        this.directionInfo = directionInfo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFromNode() { return fromNode; }
    public void setFromNode(String fromNode) { this.fromNode = fromNode; }

    public String getToNode() { return toNode; }
    public void setToNode(String toNode) { this.toNode = toNode; }

    public int getDistanceWeight() { return distanceWeight; }
    public void setDistanceWeight(int distanceWeight) { this.distanceWeight = distanceWeight; }

    public String getDirectionInfo() { return directionInfo; }
    public void setDirectionInfo(String directionInfo) { this.directionInfo = directionInfo; }
}
