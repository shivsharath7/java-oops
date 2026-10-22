package com.smartpark.model;

import java.time.LocalDateTime;

public class AuditEvent {
    private int id;
    private LocalDateTime timestamp;
    private String eventType;
    private String targetRef;
    private String description;
    private String actor;

    public AuditEvent() {
        this.timestamp = LocalDateTime.now();
        this.actor = "System";
    }

    public AuditEvent(int id, LocalDateTime timestamp, String eventType, String targetRef, String description, String actor) {
        this.id = id;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.targetRef = targetRef;
        this.description = description;
        this.actor = actor;
    }

    public AuditEvent(String eventType, String targetRef, String description, String actor) {
        this(0, LocalDateTime.now(), eventType, targetRef, description, actor);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTargetRef() { return targetRef; }
    public void setTargetRef(String targetRef) { this.targetRef = targetRef; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
}
