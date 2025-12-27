package com.example.notification_system.push;

import static com.example.notification_system.NotificationSystem.*;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.notification_system.R;
import com.example.notification_system.data.AppNotification;
import com.example.notification_system.data.NotificationRepository;
import com.example.notification_system.ui.ReportDetailActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override public void onNewToken(@NonNull String token) {
        Log.d("FCM", "new token: " + token);
        // TODO: send token to your backend if you have one
        // TokenUploader.upload(getApplicationContext(), token);
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override public void onMessageReceived(@NonNull RemoteMessage msg) {
        String type = msg.getData() != null ? msg.getData().get("type") : null;
        String reportId = msg.getData() != null ? msg.getData().get("reportId") : null;

        String title = msg.getNotification() != null ? msg.getNotification().getTitle() : null;
        String body  = msg.getNotification() != null ? msg.getNotification().getBody()  : null;

        String channel = CH_RECEIVED;
        int icon = R.drawable.ic_stat_infrawatch_foreground;
        int nid = 1001;

        if ("report.in_progress".equals(type)) { channel = CH_PROGRESS; nid = 1002; }
        else if ("report.completed".equals(type)) { channel = CH_COMPLETED; nid = 1003; }

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
        open.putExtra("reportId", reportId);
        open.putExtra("notifType", type);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, nid, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, channel)
                .setSmallIcon(icon)
                .setContentTitle(appNotification.title)
                .setContentText(appNotification.message)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS);

        NotificationManagerCompat.from(this).notify(nid, nb.build());
    }

    private String fallbackTitle(String type){
        if ("report.in_progress".equals(type)) return "Repair in progress";
        if ("report.completed".equals(type)) return "Issue resolved";
        return "Report received";
    }
    private String fallbackBody(String type, String id){
        String tail = id != null && id.length() > 4 ? id.substring(id.length()-4) : "";
        if ("report.in_progress".equals(type)) return "Work started for report #" + tail + ".";
        if ("report.completed".equals(type))   return "Report #" + tail + " is completed.";
        return "Thanks! Your report #" + tail + " has been received.";
    }
}
