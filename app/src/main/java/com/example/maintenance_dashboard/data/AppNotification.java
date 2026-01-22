package com.example.maintenance_dashboard.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications", indices = { @Index(value = { "reportId", "type" }, unique = true) })
public class AppNotification {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String message;
    public String type;
    public String reportId;
    public long timestamp;
    public boolean isRead;
}
