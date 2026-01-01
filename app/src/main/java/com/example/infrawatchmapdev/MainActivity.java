package com.example.infrawatchmapdev;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    TextView tvMinor, tvModerate, tvMajor;
    List<RepairReport> reports = new ArrayList<>();
    private GoogleMap mMap;
    RecyclerView recentRv;
    RecentActivitiesAdapter recentAdapter;
    private String selectedCategory = "All";
    private FirebaseFirestore db;
    // Filter buttons
    private Button btnFilterAll, btnFilterMinor, btnFilterModerate, btnFilterMajor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        db = FirebaseFirestore.getInstance("infrawatch");
        tvMinor = findViewById(R.id.tv_minor_count);
        tvModerate = findViewById(R.id.tv_moderate_count);
        tvMajor = findViewById(R.id.tv_major_count);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        recentRv = findViewById(R.id.recent_activities_rv);
        recentRv.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new RecentActivitiesAdapter(new ArrayList<>());
        recentRv.setAdapter(recentAdapter);
        setupFilterButtons();
        loadFirestoreData();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void loadFirestoreData() {
        if (db == null) {
            Log.e("Firestore", "FirebaseFirestore instance is null!");
            return;
        }
        db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e);
                        return;
                    }
                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d("Firestore", "No reports found in the collection.");
                        reports.clear();
                        updateCounts(reports);
                        updateRecentActivities(reports);
                        refreshMap();
                        return;
                    }
                    reports.clear();
                    for (var doc : snapshots.getDocuments()) {
                        try {
                            RepairReport report = doc.toObject(RepairReport.class);
                            if (report != null) {
                                // Skip completed reports
                                if (report.status != null && report.status.equalsIgnoreCase("Completed")) {
                                    continue;
                                }
                                reports.add(report);
                            } else {
                                Log.w("Firestore", "Document " + doc.getId() + " returned null when converting to RepairReport.");
                            }
                        } catch (Exception ex) {
                            Log.e("Firestore", "Error converting document " + doc.getId(), ex);
                        }
                    }
                    Log.d("Firestore", "Loaded " + reports.size() + " reports (excluding completed)");
                    updateCounts(reports);
                    updateRecentActivities(reports);
                    refreshMap();
                });
    }
    private void updateRecentActivities(List<RepairReport> allReports) {
        List<RepairReport> recent =
                allReports.size() > 3 ? allReports.subList(0, 3) : new ArrayList<>(allReports);
        recentAdapter.updateReports(recent);
    }
    private void refreshMap() {
        if (mMap == null) return;
        mMap.clear();
        List<RepairReport> filtered = new ArrayList<>();
        for (RepairReport r : reports) {
            if (selectedCategory.equals("All") ||
                    (r.damage_Level != null && r.damage_Level.equalsIgnoreCase(selectedCategory))) {
                filtered.add(r);
            }
        }
        Log.d("Filter", "Selected category: " + selectedCategory + ", Filtered count: " + filtered.size());
        addMarkers(filtered);
        zoomArea(filtered);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng kl = new LatLng(3.1390, 101.6869);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kl, 11));
        refreshMap();
    }
    private void addMarkers(List<RepairReport> reports) {
        Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
        for (RepairReport report : reports) {
            // Skip reports with null or empty location
            if (report.location == null || report.location.isEmpty()) {
                Log.w("Marker", "Skipping report with null/empty location");
                continue;
            }
            try {
                List<Address> addresses = geocoder.getFromLocationName(report.location, 1);
                if (addresses == null || addresses.isEmpty()) {
                    Log.w("Marker", "No address found for: " + report.location);
                    continue;
                }
                LatLng pos = new LatLng(
                        addresses.get(0).getLatitude(),
                        addresses.get(0).getLongitude()
                );
                float color;
                if (report.damage_Level == null) {
                    color = BitmapDescriptorFactory.HUE_AZURE;
                } else if (report.damage_Level.equalsIgnoreCase("Minor")) {
                    color = BitmapDescriptorFactory.HUE_GREEN;
                } else if (report.damage_Level.equalsIgnoreCase("Moderate")) {
                    color = BitmapDescriptorFactory.HUE_ORANGE;
                } else if (report.damage_Level.equalsIgnoreCase("Major")) {
                    color = BitmapDescriptorFactory.HUE_RED;
                } else {
                    color = BitmapDescriptorFactory.HUE_AZURE;
                }

                String title = report.damage_Category != null ?
                        report.damage_Category : "Unknown";
                mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(title)
                        .snippet("Status: " + (report.status != null ? report.status : "Unknown"))
                        .icon(BitmapDescriptorFactory.defaultMarker(color)));
                Log.d("Marker", "Added marker: " + title + " at " + report.location);
            } catch (IOException e) {
                Log.e("Marker", "Geocoding failed for: " + report.location, e);
            }
        }
    }
    private void setupFilterButtons() {
        btnFilterAll = findViewById(R.id.bt_filter_all);
        btnFilterMinor = findViewById(R.id.bt_filter_minor);
        btnFilterModerate = findViewById(R.id.bt_filter_moderate);
        btnFilterMajor = findViewById(R.id.bt_filter_major);
        btnFilterAll.setOnClickListener(v -> setFilter("All"));
        btnFilterMinor.setOnClickListener(v -> setFilter("Minor"));
        btnFilterModerate.setOnClickListener(v -> setFilter("Moderate"));
        btnFilterMajor.setOnClickListener(v -> setFilter("Major"));
        // Set initial active state
        updateFilterButtonStates();
    }
    private void setFilter(String category) {
        selectedCategory = category;
        Log.d("Filter", "Filter changed to: " + category);
        updateFilterButtonStates();
        refreshMap();
    }
    private void updateFilterButtonStates() {
        // Reset all buttons to default state
        btnFilterAll.setSelected(false);
        btnFilterMinor.setSelected(false);
        btnFilterModerate.setSelected(false);
        btnFilterMajor.setSelected(false);
        // Set the active button
        switch (selectedCategory) {
            case "All":
                btnFilterAll.setSelected(true);
                break;
            case "Minor":
                btnFilterMinor.setSelected(true);
                break;
            case "Moderate":
                btnFilterModerate.setSelected(true);
                break;
            case "Major":
                btnFilterMajor.setSelected(true);
                break;
        }
    }
    private void updateCounts(List<RepairReport> reports) {
        int minor = 0, moderate = 0, major = 0;
        for (RepairReport r : reports) {
            if (r.damage_Level == null) continue;
            if (r.damage_Level.equalsIgnoreCase("Minor")) {
                minor++;
            } else if (r.damage_Level.equalsIgnoreCase("Moderate")) {
                moderate++;
            } else if (r.damage_Level.equalsIgnoreCase("Major")) {
                major++;
            }
        }
        tvMinor.setText(String.valueOf(minor));
        tvModerate.setText(String.valueOf(moderate));
        tvMajor.setText(String.valueOf(major));
    }
    private void zoomArea(List<RepairReport> reports) {
        if (reports.isEmpty() || mMap == null) {
            LatLng kl = new LatLng(3.1390, 101.6869);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(kl, 11));
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
        int validLocations = 0;
        for (RepairReport report : reports) {
            if (report.location == null || report.location.isEmpty()) continue;
            try {
                List<Address> addresses = geocoder.getFromLocationName(report.location, 1);
                if (addresses == null || addresses.isEmpty()) continue;
                builder.include(new LatLng(
                        addresses.get(0).getLatitude(),
                        addresses.get(0).getLongitude()
                ));
                validLocations++;
            } catch (Exception ignored) {}
        }
        if (validLocations > 0) {
            try {
                mMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(builder.build(), 100)
                );
            } catch (Exception e) {
                Log.e("Map", "Error zooming to fit markers", e);
            }
        }
    }
}