package com.example.maintenance_dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ReportsActivity extends AppCompatActivity {

    private static final String TAG = "ReportsActivity";
    private static final String FIRESTORE_DATABASE_ID = "infrawatch";

    private FirebaseFirestore db;
    private ListenerRegistration reportListener;

    private RecyclerView rvPriorityReports;
    private RecyclerView rvModerateReports;
    private RecyclerView rvGeneralReports;
    private TextView tvNoReports;

    private TextView tvMajorHeader, tvModerateHeader, tvMinorHeader;
    private View llMajorHeader, llModerateHeader, llMinorHeader;

    private PriorityReportAdapter priorityAdapter;
    private ReportGridAdapter moderateAdapter;
    private ReportGridAdapter generalAdapter;

    private List<Report> priorityList = new ArrayList<>();
    private List<Report> moderateList = new ArrayList<>();
    private List<Report> generalList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        db = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FIRESTORE_DATABASE_ID);

        initViews();
        setupBottomNavigation();

        // Only use the listener in loadReports()
        loadReports();
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(
                R.id.bottomNavigationView);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_reports);
        }
    }

    private void setupBottomNavigation() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(
                R.id.bottomNavigationView);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_reports);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_monitor) {
                    Intent intent = new Intent(ReportsActivity.this,
                            MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_analytics) {
                    Intent intent = new Intent(ReportsActivity.this,
                            AnalyticsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_reports) {
                    return true;
                } else if (itemId == R.id.navigation_notification) {
                    Intent intent = new Intent(ReportsActivity.this,
                            NotificationListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return false;
                }
                return false;
            });
        }
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvPriorityReports = findViewById(R.id.rvPriorityReports);
        rvModerateReports = findViewById(R.id.rvModerateReports);
        rvGeneralReports = findViewById(R.id.rvGeneralReports);
        tvNoReports = findViewById(R.id.tvNoReports);

        tvMajorHeader = findViewById(R.id.tvMajorHeader);
        tvModerateHeader = findViewById(R.id.tvModerateHeader);
        tvMinorHeader = findViewById(R.id.tvMinorHeader);

        llMajorHeader = findViewById(R.id.llMajorHeader);
        llModerateHeader = findViewById(R.id.llModerateHeader);
        llMinorHeader = findViewById(R.id.llMinorHeader);

        rvPriorityReports.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvModerateReports.setLayoutManager(new GridLayoutManager(this, 2));
        rvGeneralReports.setLayoutManager(new GridLayoutManager(this, 2));

        priorityAdapter = new PriorityReportAdapter(this, priorityList);
        moderateAdapter = new ReportGridAdapter(this, moderateList);
        generalAdapter = new ReportGridAdapter(this, generalList);

        rvPriorityReports.setAdapter(priorityAdapter);
        rvModerateReports.setAdapter(moderateAdapter);
        rvGeneralReports.setAdapter(generalAdapter);
    }

    private void loadReports() {
        reportListener = db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading reports", error);
                        Toast.makeText(this, "Error loading reports", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (querySnapshot != null) {
                        priorityList.clear();
                        moderateList.clear();
                        generalList.clear();

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            try {
                                Report report = doc.toObject(Report.class);
                                if (report != null) {
                                    report.setDocumentId(doc.getId());

                                    String level = report.getConsolidatedDamageLevel().toUpperCase();

                                    if (level.equals("MAJOR") || level.equals("HIGH")) {
                                        priorityList.add(report);
                                    } else if (level.equals("MODERATE") || level.equals("MEDIUM")) {
                                        moderateList.add(report);
                                    } else {
                                        generalList.add(report);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing report: " + doc.getId(), e);
                            }
                        }

                        priorityAdapter.notifyDataSetChanged();
                        moderateAdapter.notifyDataSetChanged();
                        generalAdapter.notifyDataSetChanged();

                        // Update Visibility
                        boolean hasMajor = !priorityList.isEmpty();
                        boolean hasModerate = !moderateList.isEmpty();
                        boolean hasMinor = !generalList.isEmpty();

                        rvPriorityReports.setVisibility(hasMajor ? View.VISIBLE : View.GONE);
                        llMajorHeader.setVisibility(hasMajor ? View.VISIBLE : View.GONE);

                        rvModerateReports.setVisibility(hasModerate ? View.VISIBLE : View.GONE);
                        llModerateHeader.setVisibility(hasModerate ? View.VISIBLE : View.GONE);

                        rvGeneralReports.setVisibility(hasMinor ? View.VISIBLE : View.GONE);
                        llMinorHeader.setVisibility(hasMinor ? View.VISIBLE : View.GONE);

                        tvNoReports.setVisibility((hasMajor || hasModerate || hasMinor) ? View.GONE : View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reportListener != null) {
            reportListener.remove();
        }
    }
}
