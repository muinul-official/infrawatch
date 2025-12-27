package com.example.notification_system;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notification_system.ui.NotificationListActivity;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ask notification permission on Android 13+ (if not already done)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        10
                );
            }
        }

        // Get FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("FCM", "Token: " + token);
                    Toast.makeText(this, "Token logged in Logcat", Toast.LENGTH_SHORT).show();
                });

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 10);
            }
        }

        Button viewNotificationsButton = findViewById(R.id.view_notifications_button);
        viewNotificationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationListActivity.class);
            startActivity(intent);
        });
    }
}
