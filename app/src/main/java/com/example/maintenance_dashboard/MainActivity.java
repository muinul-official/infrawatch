package com.example.maintenance_dashboard;

import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.example.maintenance_dashboard.data.AppNotification;
import com.example.maintenance_dashboard.data.NotificationRepository;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements com.google.android.gms.maps.OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    private RecyclerView rvReports, rvPriorityQueue;
    private android.view.View llPriorityQueueHeader;
    private ReportAdapter adapter;
    private PriorityReportAdapter priorityQueueAdapter;
    private List<Report> reportList;
    private List<Report> priorityQueueList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isInitialLoad = true;
    private ListenerRegistration reportListener;
    private TextView tvCompletedCount, tvNewReportsCount, tvInProgressCount, tvPriorityQueueHeader;
    private com.google.android.gms.maps.GoogleMap mGoogleMap;

    // Custom Firestore database ID
    private static final String FIRESTORE_DATABASE_ID = "infrawatch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Initialize Firestore with the custom database ID "infrawatch" in
        // asia-southeast1
        db = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FIRESTORE_DATABASE_ID);

        rvReports = findViewById(R.id.rvReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));

        rvPriorityQueue = findViewById(R.id.rvPriorityQueue);
        rvPriorityQueue.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        llPriorityQueueHeader = findViewById(R.id.llPriorityQueueHeader);
        tvPriorityQueueHeader = findViewById(R.id.tvPriorityQueueHeader);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        reportList = new ArrayList<>();
        adapter = new ReportAdapter(this, reportList);
        rvReports.setAdapter(adapter);

        priorityQueueList = new ArrayList<>();
        priorityQueueAdapter = new PriorityReportAdapter(this, priorityQueueList);
        rvPriorityQueue.setAdapter(priorityQueueAdapter);

        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvNewReportsCount = findViewById(R.id.tvNewReportsCount);
        tvInProgressCount = findViewById(R.id.tvInProgressCount);
        tvPriorityQueueHeader = findViewById(R.id.tvPriorityQueueHeader);

        createNotificationChannels();

        // Setup bottom navigation
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(
                R.id.bottomNavigationView);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_monitor);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_analytics) {
                    Intent intent = new Intent(MainActivity.this, AnalyticsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_monitor) {
                    // Already on this screen
                    return true;
                } else if (itemId == R.id.navigation_reports) {
                    Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_notification) {
                    Intent intent = new Intent(MainActivity.this, NotificationListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }

        android.widget.ImageView ivProfile = findViewById(R.id.ivProfile);
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AdminProfileActivity.class);
                startActivity(intent);
            });
        }

        // Initialize the Map Fragment
        com.google.android.gms.maps.SupportMapFragment mapFragment = (com.google.android.gms.maps.SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull com.google.android.gms.maps.GoogleMap googleMap) {
        this.mGoogleMap = googleMap;

        // Command Center map configuration - Interactive
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        // Move camera to a default location (Kuala Lumpur)
        com.google.android.gms.maps.model.LatLng defaultLocation = new com.google.android.gms.maps.model.LatLng(3.1390,
                101.6869);
        googleMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));

        // If reports are already loaded, update markers
        if (!reportList.isEmpty()) {
            updateMapMarkers();
        }
    }

    private void updateMapMarkers() {
        if (mGoogleMap == null)
            return;
        mGoogleMap.clear();

        for (Report report : reportList) {
            Double lat = report.getLatitude();
            Double lng = report.getLongitude();

            if (lat != null && lng != null) {
                placeMarker(report, new com.google.android.gms.maps.model.LatLng(lat, lng));
            } else {
                // FALLBACK: Geocode the location name for older reports
                String locationName = report.getLocation();
                if (locationName != null && !locationName.isEmpty()) {
                    android.location.Geocoder geocoder = new android.location.Geocoder(this,
                            java.util.Locale.getDefault());
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocationName(locationName, 1, addresses -> {
                            if (addresses != null && !addresses.isEmpty()) {
                                runOnUiThread(() -> placeMarker(report, new com.google.android.gms.maps.model.LatLng(
                                        addresses.get(0).getLatitude(), addresses.get(0).getLongitude())));
                            }
                        });
                    } else {
                        try {
                            java.util.List<android.location.Address> addresses = geocoder
                                    .getFromLocationName(locationName, 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                placeMarker(report, new com.google.android.gms.maps.model.LatLng(
                                        addresses.get(0).getLatitude(), addresses.get(0).getLongitude()));
                            }
                        } catch (java.io.IOException ignored) {
                        }
                    }
                }
            }
        }
    }

    private void placeMarker(Report report, com.google.android.gms.maps.model.LatLng pos) {
        if (mGoogleMap == null)
            return;

        float markerColor;
        String level = report.getConsolidatedDamageLevel().toUpperCase();
        if (level.equals("MAJOR") || level.equals("HIGH")) {
            markerColor = com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED;
        } else if (level.equals("MODERATE") || level.equals("MEDIUM")) {
            markerColor = com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE;
        } else {
            markerColor = com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN;
        }

        mGoogleMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                .position(pos)
                .title(report.getDamageCategory())
                .snippet("Status: " + report.getStatus())
                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(markerColor)));
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User not signed in. Attempting anonymous sign in.");
            signInAnonymously();
        } else {
            Log.d(TAG, "User already signed in with UID: " + currentUser.getUid());
            loadReports();
        }
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInAnonymously:success");
                            loadReports();
                        } else {
                            Log.w(TAG, "signInAnonymously:failure", task.getException());
                            Toast.makeText(MainActivity.this,
                                    "Authentication failed. Please check your connection.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void loadReports() {
        Log.d(TAG, "Loading reports from Firestore...");
        Log.d(TAG, "Current user: " + (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "null"));

        // Reset listener management
        if (reportListener != null) {
            reportListener.remove();
        }
        isInitialLoad = true;

        // Real-time listener for all report updates (This replaces the redundant .get()
        // call)
        reportListener = db.collection("reports")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Snapshot listener error: " + error.getMessage());
                        return;
                    }

                    if (querySnapshot != null) {
                        // Check for new reports to notify admin
                        for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                if (!isInitialLoad) {
                                    try {
                                        Report newReport = dc.getDocument().toObject(Report.class);
                                        if (newReport != null) {
                                            String userId = newReport.getUserId();
                                            NotificationRepository repository = new NotificationRepository(
                                                    getApplication());

                                            // Fetch User Name for detailed notification
                                            if (userId != null) {
                                                db.collection("users").document(userId).get()
                                                        .addOnSuccessListener(userDoc -> {
                                                            String userName = userDoc != null
                                                                    ? userDoc.getString("fName")
                                                                    : "A user";
                                                            String role = userDoc != null ? userDoc.getString("role")
                                                                    : "user";

                                                            if (userName == null || userName.isEmpty())
                                                                userName = "A user";

                                                            AppNotification notif = new AppNotification();
                                                            String titleSuffix = "admin".equalsIgnoreCase(role)
                                                                    ? " (Admin)"
                                                                    : "";
                                                            notif.title = "New Report From " + userName + titleSuffix;

                                                            // Detailed message: [Username] reported [category] on
                                                            // [time] and it's [damage level]
                                                            String timeStr = newReport.getFormattedDate();
                                                            String catStr = newReport.getDamageCategory();
                                                            if (catStr == null || catStr.isEmpty())
                                                                catStr = "an infrastructure issue";
                                                            String levelStr = newReport.getConsolidatedDamageLevel();

                                                            notif.message = String.format(
                                                                    "%s reported %s on %s and it's %s",
                                                                    userName, catStr, timeStr, levelStr);

                                                            notif.type = "report.received";
                                                            notif.reportId = dc.getDocument().getId();
                                                            notif.timestamp = System.currentTimeMillis();
                                                            notif.isRead = false;
                                                            repository.insert(notif);

                                                            Log.d(TAG, "Admin notified with detailed info: "
                                                                    + notif.message);
                                                        });
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error creating detailed admin notification", e);
                                    }
                                }
                            }
                        }

                        // Mark initial load as complete after the first batch
                        isInitialLoad = false;

                        // Update the report list
                        reportList.clear();
                        priorityQueueList.clear();

                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            try {
                                Report report = document.toObject(Report.class);
                                if (report != null) {
                                    report.setDocumentId(document.getId());
                                    reportList.add(report);

                                    // Check for Priority Queue: New & Major
                                    String level = report.getConsolidatedDamageLevel().toUpperCase();
                                    String status = report.getStatus();
                                    if ((level.equals("MAJOR") || level.equals("HIGH")) &&
                                            (status == null || status.equals(Report.STATUS_NEW)
                                                    || status.equals(Report.STATUS_PENDING))) {
                                        priorityQueueList.add(report);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing document in listener: " + document.getId(), e);
                            }
                        }

                        sortReportsBySeverity(reportList);
                        sortReportsBySeverity(priorityQueueList);

                        adapter.notifyDataSetChanged();
                        priorityQueueAdapter.notifyDataSetChanged();

                        // Visibility management for priority queue
                        boolean hasPriority = !priorityQueueList.isEmpty();
                        rvPriorityQueue.setVisibility(hasPriority ? android.view.View.VISIBLE : android.view.View.GONE);
                        llPriorityQueueHeader
                                .setVisibility(hasPriority ? android.view.View.VISIBLE : android.view.View.GONE);
                        tvPriorityQueueHeader
                                .setVisibility(hasPriority ? android.view.View.VISIBLE : android.view.View.GONE);

                        // Update Priority Queue visibility
                        if (priorityQueueList.isEmpty()) {
                            rvPriorityQueue.setVisibility(android.view.View.GONE);
                            tvPriorityQueueHeader.setVisibility(android.view.View.GONE);
                        } else {
                            rvPriorityQueue.setVisibility(android.view.View.VISIBLE);
                            tvPriorityQueueHeader.setVisibility(android.view.View.VISIBLE);
                        }

                        updateDashboardStats();
                        updateMapMarkers();
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reportListener != null) {
            reportListener.remove();
            reportListener = null;
        }
    }

    private void updateDashboardStats() {
        int completedCount = 0;
        int newReportsCount = 0;
        int inProgressCount = 0;

        for (Report report : reportList) {
            String status = report.getStatus();
            if (status != null) {
                switch (status) {
                    case Report.STATUS_COMPLETED:
                    case Report.STATUS_CLOSED:
                        completedCount++;
                        break;
                    case Report.STATUS_NEW:
                    case Report.STATUS_PENDING:
                        newReportsCount++;
                        break;
                    case Report.STATUS_IN_PROGRESS:
                        inProgressCount++;
                        break;
                    default:
                        // Handle any other status as new
                        newReportsCount++;
                        break;
                }
            }
        }

        // Update the stats in the UI
        if (tvCompletedCount != null)
            tvCompletedCount.setText(String.valueOf(completedCount));
        if (tvNewReportsCount != null)
            tvNewReportsCount.setText(String.valueOf(newReportsCount));
        if (tvInProgressCount != null)
            tvInProgressCount.setText(String.valueOf(inProgressCount));

        Log.d(TAG, "Stats updated - Completed: " + completedCount +
                ", New: " + newReportsCount + ", In Progress: " + inProgressCount);
    }

    private void sortReportsBySeverity(List<Report> reports) {
        reports.sort((r1, r2) -> {
            int p1 = getSeverityPriority(r1.getConsolidatedDamageLevel());
            int p2 = getSeverityPriority(r2.getConsolidatedDamageLevel());

            if (p1 != p2) {
                return Integer.compare(p2, p1); // Higher priority first
            }

            // If same priority, sort by timestamp (newest first)
            if (r1.getTimestamp() != null && r2.getTimestamp() != null) {
                return r2.getTimestamp().compareTo(r1.getTimestamp());
            }
            return 0;
        });
    }

    private int getSeverityPriority(String level) {
        if (level == null)
            return 0;
        switch (level.toUpperCase()) {
            case "MAJOR":
            case "HIGH":
                return 3;
            case "MODERATE":
            case "MEDIUM":
                return 2;
            case "MINOR":
            case "LOW":
                return 1;
            default:
                return 0;
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(new NotificationChannel(
                        NotificationConstants.CH_RECEIVED, "Report Received", NotificationManager.IMPORTANCE_HIGH));
                nm.createNotificationChannel(new NotificationChannel(
                        NotificationConstants.CH_PROGRESS, "Repair In Progress",
                        NotificationManager.IMPORTANCE_DEFAULT));
                nm.createNotificationChannel(new NotificationChannel(
                        NotificationConstants.CH_COMPLETED, "Issue Resolved", NotificationManager.IMPORTANCE_DEFAULT));
            }
        }
    }
}
