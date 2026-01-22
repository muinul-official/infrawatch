package com.example.maintenance_dashboard.push;

import static com.example.maintenance_dashboard.NotificationConstants.*;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.maintenance_dashboard.R;
import com.example.maintenance_dashboard.ReportDetailActivity;
import com.example.maintenance_dashboard.data.AppNotification;
import com.example.maintenance_dashboard.data.NotificationRepository;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("FCM", "new token: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        String type = msg.getData() != null ? msg.getData().get("type") : null;
        String reportId = msg.getData() != null ? msg.getData().get("reportId") : null;

        String title = msg.getNotification() != null ? msg.getNotification().getTitle() : null;
        String body = msg.getNotification() != null ? msg.getNotification().getBody() : null;

        String channel = CH_RECEIVED;
        int icon = R.drawable.ic_launcher_foreground; // Fallback to launcher icon
        int nid = 1001;

        if ("report.in_progress".equals(type)) {
            channel = CH_PROGRESS;
            nid = 1002;
        } else if ("report.completed".equals(type)) {
            channel = CH_COMPLETED;
            nid = 1003;
        }

        // Save notification to local database
        NotificationRepository repository = new NotificationRepository(getApplication());
        AppNotification appNotification = new AppNotification();
        appNotification.title = title != null ? title : fallbackTitle(type);
        appNotification.message = body != null ? body : fallbackBody(type, reportId);
        appNotification.type = type;
        appNotification.reportId = reportId;
        appNotification.timestamp = System.currentTimeMillis();
        appNotification.isRead = false;
        repository.insert(appNotification);

        Intent open = new Intent(this, ReportDetailActivity.class);
        open.putExtra(ReportDetailActivity.EXTRA_DOCUMENT_ID, reportId);
        open.putExtra("notifType", type);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, nid, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, channel)
                .setSmallIcon(icon)
                .setContentTitle(appNotification.title)
                .setContentText(appNotification.message)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(nid, nb.build());
        }
    }

    private String fallbackTitle(String type) {
        if ("report.in_progress".equals(type))
            return "Repair in progress";
        if ("report.completed".equals(type))
            return "Issue resolved";
        return "Report received";
    }

    private String fallbackBody(String type, String id) {
        String tail = id != null && id.length() > 4 ? id.substring(id.length() - 4) : "";
        if ("report.in_progress".equals(type))
            return "Work started for report #" + tail + ".";
        if ("report.completed".equals(type))
            return "Report #" + tail + " is completed.";
        return "Thanks! Your report #" + tail + " has been received.";
    }
}
