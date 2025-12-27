package com.example.notification_system;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class NotificationSystem extends Application {
    public static final String CH_RECEIVED = "report_received";
    public static final String CH_PROGRESS = "report_in_progress";
    public static final String CH_COMPLETED = "report_completed";

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(
                    CH_RECEIVED, "Report Received", NotificationManager.IMPORTANCE_HIGH));
            nm.createNotificationChannel(new NotificationChannel(
                    CH_PROGRESS, "Repair In Progress", NotificationManager.IMPORTANCE_DEFAULT));
            nm.createNotificationChannel(new NotificationChannel(
                    CH_COMPLETED, "Issue Resolved", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }
}

