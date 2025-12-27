package com.example.notification_system.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
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
