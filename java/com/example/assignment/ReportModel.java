package com.example.assignment;

/**
 * Data class representing a single report item.
 */
public class ReportModel {
    private final String id;
    private final String category;
    private final String location;
    private final String status;

    public ReportModel(String id, String category, String location, String status) {
        this.id = id;
        this.category = category;
        this.location = location;
        this.status = status;
    }

    // Getters
    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
}
