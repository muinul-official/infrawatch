package com.example.maintenance_dashboard;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    TextView tvMinor, tvModerate, tvMajor;
    List<Report> reports = new ArrayList<>();
    private GoogleMap mMap;
    RecyclerView recentRv;
    RecentActivitiesAdapter recentAdapter;
    private String selectedCategory = "ALL";
    private FirebaseFirestore db;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private FloatingActionButton fabToggleSheet, fabAddReport;
    private com.google.android.material.button.MaterialButton btnFilterAll, btnFilterMinor, btnFilterModerate,
            btnFilterMajor;
    private Button btnLogout, btnMyReports, btnCreateReportLarge;
    private TextView tvDashboardGreeting, tvMyTotalReports, tvMyResolvedReports;
    private android.widget.EditText etSearch;
    private View ivSearchAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_map);

        db = FirebaseFirestore.getInstance(com.google.firebase.FirebaseApp.getInstance(), "infrawatch");
        tvMinor = findViewById(R.id.tv_minor_count);
        tvModerate = findViewById(R.id.tv_moderate_count);
        tvMajor = findViewById(R.id.tv_major_count);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fetchUserData(toolbar);

        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        fabToggleSheet = findViewById(R.id.fab_toggle_sheet);
        fabToggleSheet.setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        findViewById(R.id.ivProfileContainer).setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfileActivity.class));
        });

        etSearch = findViewById(R.id.etSearch);
        ivSearchAction = findViewById(R.id.ivSearchAction);

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        ivSearchAction.setOnClickListener(v -> performSearch());

        btnMyReports = findViewById(R.id.btnViewMyReports);
        btnMyReports.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserReportsActivity.class);
            startActivity(intent);
        });

        btnCreateReportLarge = findViewById(R.id.btnCreateNewReportLarge);
        btnCreateReportLarge.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserMainActivity.class);
            startActivity(intent);
        });

        tvDashboardGreeting = findViewById(R.id.tvDashboardGreeting);
        tvMyTotalReports = findViewById(R.id.tvMyTotalReports);
        tvMyResolvedReports = findViewById(R.id.tvMyResolvedReports);
        fetchMyReportStats();

        fabAddReport = findViewById(R.id.fab_add_report);
        fabAddReport.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), UserMainActivity.class);
            startActivity(intent);
        });

        btnLogout = findViewById(R.id.btn_map_logout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        tvMinor.setOnClickListener(v -> setFilter("MINOR"));
        tvModerate.setOnClickListener(v -> setFilter("MODERATE"));
        tvMajor.setOnClickListener(v -> setFilter("MAJOR"));

        // Also make parent containers clickable for better UX
        findViewById(R.id.cv_minor_stat).setOnClickListener(v -> setFilter("MINOR"));
        findViewById(R.id.cv_moderate_stat).setOnClickListener(v -> setFilter("MODERATE"));
        findViewById(R.id.cv_major_stat).setOnClickListener(v -> setFilter("MAJOR"));

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    fabToggleSheet.setImageResource(R.drawable.ic_arrow_back); // Or use a close icon if available
                    fabToggleSheet.setRotation(-90);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    fabToggleSheet.setImageResource(R.drawable.ic_details);
                    fabToggleSheet.setRotation(0);
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        recentRv = findViewById(R.id.recent_activities_rv);
        recentRv.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new RecentActivitiesAdapter(new ArrayList<>());
        recentRv.setAdapter(recentAdapter);

        setupFilterButtons();
        loadFirestoreData();
    }

    private void fetchUserData(com.google.android.material.appbar.MaterialToolbar toolbar) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(document -> {
                if (document.exists()) {
                    String name = document.getString("fName");
                    if (name != null && !name.isEmpty()) {
                        String firstName = name.split(" ")[0];
                        toolbar.setTitle("Hi, " + firstName);
                        tvDashboardGreeting.setText("Welcome Back, " + firstName);
                    }
                }
            });
        }
    }

    private void fetchMyReportStats() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            // Total reports by user
            db.collection("reports").whereEqualTo("userId", uid).get().addOnSuccessListener(snapshots -> {
                if (snapshots != null) {
                    tvMyTotalReports.setText(String.valueOf(snapshots.size()));

                    // Count resolved reports within this set
                    int resolvedCount = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        String status = doc.getString("status");
                        if (status != null && (status.equalsIgnoreCase(Report.STATUS_COMPLETED) ||
                                status.equalsIgnoreCase(Report.STATUS_CLOSED))) {
                            resolvedCount++;
                        }
                    }
                    tvMyResolvedReports.setText(String.valueOf(resolvedCount));
                }
            });
        }
    }

    private void loadFirestoreData() {
        if (db == null)
            return;

        db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null)
                        return;

                    if (snapshots == null || snapshots.isEmpty()) {
                        reports.clear();
                        updateCounts(reports);
                        updateRecentActivities(reports);
                        refreshMap();
                        return;
                    }
                    reports.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        try {
                            Report report = doc.toObject(Report.class);
                            if (report != null) {
                                if (report.getStatus() != null && (report.getStatus().equalsIgnoreCase("Completed")
                                        || report.getStatus().equalsIgnoreCase("Closed"))) {
                                    continue;
                                }
                                reports.add(report);
                            }
                        } catch (Exception ex) {
                            Log.e("Firestore", "Error converting document " + doc.getId(), ex);
                        }
                    }
                    updateCounts(reports);
                    updateRecentActivities(reports);
                    refreshMap();
                });
    }

    private void updateRecentActivities(List<Report> allReports) {
        List<Report> recent = allReports.size() > 3 ? allReports.subList(0, 3) : new ArrayList<>(allReports);
        recentAdapter.updateReports(recent);
    }

    private void refreshMap() {
        if (mMap == null)
            return;
        mMap.clear();
        List<Report> filtered = new ArrayList<>();
        for (Report r : reports) {
            String damageLevel = r.getConsolidatedDamageLevel();
            if (selectedCategory.equals("ALL") || damageLevel.equals(selectedCategory)) {
                filtered.add(r);
            }
        }
        addMarkers(filtered);
        zoomArea(filtered);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        LatLng kl = new LatLng(3.1390, 101.6869);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kl, 11));
        refreshMap();
    }

    private void addMarkers(List<Report> reports) {
        if (mMap == null)
            return;
        Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
        for (Report report : reports) {
            // Priority 1: Use direct Latitude/Longitude if available
            if (report.getLatitude() != null && report.getLongitude() != null) {
                LatLng pos = new LatLng(report.getLatitude(), report.getLongitude());
                addMarkerAtLocation(report, pos);
                continue;
            }

            // Priority 2: Use Geocoder as fallback
            String locationName = report.getLocation();
            if (locationName == null || locationName.isEmpty()) {
                continue;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(locationName, 1, addresses -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        runOnUiThread(() -> {
                            Address addr = addresses.get(0);
                            addMarkerAtLocation(report, new LatLng(addr.getLatitude(), addr.getLongitude()));
                        });
                    }
                });
            } else {
                try {
                    List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        addMarkerAtLocation(report, new LatLng(addr.getLatitude(), addr.getLongitude()));
                    }
                } catch (IOException e) {
                    Log.e("Marker", "Geocoding failed for: " + locationName, e);
                }
            }
        }
    }

    private void addMarkerAtLocation(Report report, LatLng pos) {
        if (mMap == null)
            return;

        float color;
        String lvl = report.getConsolidatedDamageLevel(); // It's already normalized to UPPERCASE in Report.java

        if (lvl.equals("MAJOR")) {
            color = BitmapDescriptorFactory.HUE_RED;
        } else if (lvl.equals("MODERATE")) {
            color = BitmapDescriptorFactory.HUE_ORANGE;
        } else if (lvl.equals("MINOR")) {
            color = BitmapDescriptorFactory.HUE_GREEN;
        } else {
            color = BitmapDescriptorFactory.HUE_AZURE;
        }

        String title = report.getDamageCategory() != null ? report.getDamageCategory() : "Issue";
        mMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(title)
                .snippet("Severity: " + lvl + " | Status: " + (report.getStatus() != null ? report.getStatus() : "New"))
                .icon(BitmapDescriptorFactory.defaultMarker(color)));
    }

    private void setupFilterButtons() {
        btnFilterAll = findViewById(R.id.bt_filter_all);
        btnFilterMinor = findViewById(R.id.bt_filter_minor);
        btnFilterModerate = findViewById(R.id.bt_filter_moderate);
        btnFilterMajor = findViewById(R.id.bt_filter_major);

        btnFilterAll.setOnClickListener(v -> setFilter("ALL"));
        btnFilterMinor.setOnClickListener(v -> setFilter("MINOR"));
        btnFilterModerate.setOnClickListener(v -> setFilter("MODERATE"));
        btnFilterMajor.setOnClickListener(v -> setFilter("MAJOR"));

        updateFilterButtonStates();
    }

    private void setFilter(String category) {
        Log.d("FILTER", "Setting filter to: " + category);
        selectedCategory = category;
        updateFilterButtonStates();
        refreshMap();

        // Brief toast to confirm selection
        Toast.makeText(this, "Showing " + category + " reports", Toast.LENGTH_SHORT).show();

        // Collapse the sheet so the user can see the map changes
        if (bottomSheetBehavior != null &&
                bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void updateFilterButtonStates() {
        highlightButton(btnFilterAll, "ALL".equals(selectedCategory));
        highlightButton(btnFilterMinor, "MINOR".equals(selectedCategory));
        highlightButton(btnFilterModerate, "MODERATE".equals(selectedCategory));
        highlightButton(btnFilterMajor, "MAJOR".equals(selectedCategory));
    }

    private void highlightButton(com.google.android.material.button.MaterialButton button, boolean active) {
        if (active) {
            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.brand_blue)));
            button.setTextColor(getResources().getColor(R.color.white));
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.white)));
            button.setTextColor(getResources().getColor(R.color.brand_blue));
            button.setStrokeWidth(2); // Thicker outline for better visibility
        }
    }

    private void updateCounts(List<Report> reports) {
        int minor = 0, moderate = 0, major = 0;
        for (Report r : reports) {
            String level = r.getConsolidatedDamageLevel(); // Already normalized to MAJOR, MODERATE, MINOR
            if (level.equals("MINOR")) {
                minor++;
            } else if (level.equals("MODERATE")) {
                moderate++;
            } else if (level.equals("MAJOR")) {
                major++;
            }
        }
        tvMinor.setText(String.valueOf(minor));
        tvModerate.setText(String.valueOf(moderate));
        tvMajor.setText(String.valueOf(major));
    }

    @SuppressWarnings("deprecation")
    private void zoomArea(List<Report> reports) {
        if (reports.isEmpty() || mMap == null) {
            LatLng kl = new LatLng(3.1390, 101.6869);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(kl, 11));
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
        final int[] validLocations = { 0 };
        final int totalReports = reports.size();
        final int[] processedReports = { 0 };

        for (Report report : reports) {
            String locationName = report.getLocation();
            if (locationName == null || locationName.isEmpty()) {
                synchronized (processedReports) {
                    processedReports[0]++;
                    checkAndZoom(builder, validLocations[0], processedReports[0], totalReports);
                }
                continue;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(locationName, 1, addresses -> {
                    synchronized (processedReports) {
                        processedReports[0]++;
                        if (addresses != null && !addresses.isEmpty()) {
                            builder.include(
                                    new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude()));
                            validLocations[0]++;
                        }
                        checkAndZoom(builder, validLocations[0], processedReports[0], totalReports);
                    }
                });
            } else {
                try {
                    List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
                    processedReports[0]++;
                    if (addresses != null && !addresses.isEmpty()) {
                        builder.include(new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude()));
                        validLocations[0]++;
                    }
                    checkAndZoom(builder, validLocations[0], processedReports[0], totalReports);
                } catch (IOException ignored) {
                    processedReports[0]++;
                    checkAndZoom(builder, validLocations[0], processedReports[0], totalReports);
                }
            }
        }
    }

    private void checkAndZoom(LatLngBounds.Builder builder, int validLocations, int processedReports,
            int totalReports) {
        if (processedReports == totalReports && validLocations > 0 && mMap != null) {
            runOnUiThread(() -> {
                try {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                } catch (Exception e) {
                    Log.e("Map", "Error zooming to fit markers", e);
                }
            });
        }
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                // Hide keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                        android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                }
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
