package com.example.vehicalrentalserviceplatform.model;

public class ActivityLogEntry {
    private String timestamp;
    private String actor;
    private String action;
    private String details;

    public ActivityLogEntry(String timestamp, String actor, String action, String details) {
        this.timestamp = timestamp;
        this.actor = actor;
        this.action = action;
        this.details = details;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }
}
