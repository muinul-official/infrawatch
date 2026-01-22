package com.example.maintenance_dashboard;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

import java.util.List;

public class Report {
    @DocumentId
    private String documentId;

    @PropertyName("damage_Category")
    private String damageCategory;

    @PropertyName("damage_Level")
    private String damageLevel;

    @PropertyName("damage_severity")
    private String damageSeverity;

    private String description;
    private String location;

    @PropertyName("photoVideoUrls")
    private List<String> photoVideoUrls;

    @PropertyName("specific_Issue_Type")
    private String specificIssueType;

    private String status;
    private Timestamp timestamp;
    private String userId;
    private Double latitude;
    private Double longitude;

    // Additional fields for contractor assignment
    private String assignedContractor;
    private String contractorName;

    // Status constants
    public static final String STATUS_NEW = "New Report";
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CLOSED = "Closed";

    public Report() {
        // Default constructor required for Firestore
    }

    // Document ID
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    // Damage Category
    @PropertyName("damage_Category")
    public String getDamageCategory() {
        return damageCategory;
    }

    @PropertyName("damage_Category")
    public void setDamageCategory(String damageCategory) {
        this.damageCategory = damageCategory;
    }

    // Damage Level
    @PropertyName("damage_Level")
    public String getDamageLevel() {
        return damageLevel;
    }

    @PropertyName("damage_Level")
    public void setDamageLevel(String damageLevel) {
        this.damageLevel = damageLevel;
    }

    // Damage Severity
    @PropertyName("damage_severity")
    public String getDamageSeverity() {
        return damageSeverity;
    }

    @PropertyName("damage_severity")
    public void setDamageSeverity(String damageSeverity) {
        this.damageSeverity = damageSeverity;
    }

    // Description
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Location
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // Photo/Video URLs
    @PropertyName("photoVideoUrls")
    public List<String> getPhotoVideoUrls() {
        return photoVideoUrls;
    }

    @PropertyName("photoVideoUrls")
    public void setPhotoVideoUrls(List<String> photoVideoUrls) {
        this.photoVideoUrls = photoVideoUrls;
    }

    // Specific Issue Type
    @PropertyName("specific_Issue_Type")
    public String getSpecificIssueType() {
        return specificIssueType;
    }

    @PropertyName("specific_Issue_Type")
    public void setSpecificIssueType(String specificIssueType) {
        this.specificIssueType = specificIssueType;
    }

    // Status
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Timestamp
    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    // User ID
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Latitude
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    // Longitude
    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    // Assigned Contractor ID
    public String getAssignedContractor() {
        return assignedContractor;
    }

    public void setAssignedContractor(String assignedContractor) {
        this.assignedContractor = assignedContractor;
    }

    // Contractor Name (for display)
    public String getContractorName() {
        return contractorName;
    }

    public void setContractorName(String contractorName) {
        this.contractorName = contractorName;
    }

    // Helper method to get first image URL
    public String getFirstImageUrl() {
        if (photoVideoUrls != null && !photoVideoUrls.isEmpty()) {
            return photoVideoUrls.get(0);
        }
        return null;
    }

    // Helper to get consolidated damage level/severity (normalized)
    public String getConsolidatedDamageLevel() {
        String level = "N/A";
        String raw = "";
        if (damageLevel != null && !damageLevel.trim().isEmpty()) {
            raw = damageLevel.trim().toUpperCase();
        } else if (damageSeverity != null && !damageSeverity.trim().isEmpty()) {
            raw = damageSeverity.trim().toUpperCase();
        }

        if (raw.equals("MAJOR") || raw.equals("HIGH"))
            return "MAJOR";
        if (raw.equals("MODERATE") || raw.equals("MEDIUM"))
            return "MODERATE";
        if (raw.equals("MINOR") || raw.equals("LOW"))
            return "MINOR";

        return raw.isEmpty() ? "N/A" : raw;
    }

    // Helper method to check if report can be assigned
    public boolean canBeAssigned() {
        return status != null && !status.equals(STATUS_CLOSED);
    }

    // Helper method to check if report can be closed
    public boolean canBeClosed() {
        return status != null && status.equals(STATUS_COMPLETED);
    }

    // Helper to get formatted date from timestamp
    public String getFormattedDate() {
        if (timestamp != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm",
                    java.util.Locale.getDefault());
            return sdf.format(timestamp.toDate());
        }
        return "N/A";
    }
}
