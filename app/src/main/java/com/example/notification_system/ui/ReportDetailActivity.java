package com.example.notification_system.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.notification_system.R;

public class ReportDetailActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        String reportId = getIntent().getStringExtra("reportId");
        String type = getIntent().getStringExtra("notifType");

        TextView tv = findViewById(R.id.reportText);
        tv.setText("Report " + reportId + " • " + type);
        // TODO: call your API to load details and show rating UI when type == report.completed
        Log.d("ReportDetail", "Opened from notification, type=" + type + ", reportId=" + reportId);

    }
}
