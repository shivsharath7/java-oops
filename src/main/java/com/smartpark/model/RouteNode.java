package com.smartpark.model;

public class RouteNode {
    private int id;
    private String nodeId;
    private String floor;
    private String name;
    private String nodeType; // SLOT, AISLE, RAMP, ELEVATOR, EXIT, ENTRANCE
    private int xCoord;
    private int yCoord;

    public RouteNode() {}

    public RouteNode(String nodeId, String floor, String name, String nodeType, int xCoord, int yCoord) {
        this.nodeId = nodeId;
        this.floor = floor;
        this.name = name;
        this.nodeType = nodeType;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public int getXCoord() { return xCoord; }
    public void setXCoord(int xCoord) { this.xCoord = xCoord; }

    public int getYCoord() { return yCoord; }
    public void setYCoord(int yCoord) { this.yCoord = yCoord; }

    @Override
    public String toString() {
        return name + " (" + nodeId + ")";
    }
}
