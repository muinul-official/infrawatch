package com.example.maintenance_dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnalyticsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "AnalyticsActivity";

    // UI Components
    private TextView tvTotalIssues, tvHighRisk;
    private LineChart lineChart;
    private GoogleMap mMap;

    // Firebase
    private FirebaseFirestore db;

    // State
    private final AtomicBoolean isMapReady = new AtomicBoolean(false);
    private AnalyticsResult currentResult = null; // Hold data until map is ready

    // Cache for geocoding results to ensure fast live updates
    private static final Map<String, LatLng> locationCache = new java.util.concurrent.ConcurrentHashMap<>();

    // --- INNER CLASS: Pure Data Container (Thread Safe) ---
    private static class AnalyticsResult {
        int totalIssues = 0;
        int highRiskCount = 0;

        int countNew = 0;
        int countProgress = 0;
        int countCompleted = 0;

        final List<LatLng> highRiskPoints = new ArrayList<>();
        final List<LatLng> mediumRiskPoints = new ArrayList<>();
        final List<LatLng> lowRiskPoints = new ArrayList<>();

        final List<Entry> trendEntries = new ArrayList<>();
        final List<Map.Entry<String, Integer>> topCategories = new ArrayList<>();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        try {
            initViews();
            setupBottomNavigation();

            db = FirebaseFirestore.getInstance(com.google.firebase.FirebaseApp.getInstance(), "infrawatch");

            // Start map loading
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map_fragment);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }

            // Start Data Loading
            loadAnalyticsSafely();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing Analytics", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tvTotalIssues = findViewById(R.id.tvTotalIssues);
        tvHighRisk = findViewById(R.id.tvHighRisk);
        lineChart = findViewById(R.id.lineChart);
    }

    private void setupBottomNavigation() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(
                R.id.bottomNavigationView);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_analytics);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_monitor) {
                    Intent intent = new Intent(AnalyticsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_analytics) {
                    return true;
                } else if (itemId == R.id.navigation_reports) {
                    Intent intent = new Intent(AnalyticsActivity.this, ReportsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private void loadAnalyticsSafely() {
        Log.d(TAG, "Starting safe analytics load...");
        db.collection("reports")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Firestore Listen Failed", e);
                        Toast.makeText(AnalyticsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d(TAG, "No documents found.");
                        return;
                    }

                    // Process on Background Thread
                    new Thread(() -> {
                        try {
                            AnalyticsResult result = processData(snapshots);

                            // Post Result to Main Thread
                            runOnUiThread(() -> applyResult(result));

                        } catch (Exception ex) {
                            Log.e(TAG, "Background Processing Error", ex);
                        }
                    }).start();
                });
    }

    // --- PURE FUNCTION: Processes data into a Result Object ---
    private AnalyticsResult processData(com.google.firebase.firestore.QuerySnapshot snapshots) {
        AnalyticsResult result = new AnalyticsResult();

        Map<Long, Integer> weeklyCountMap = new HashMap<>();
        Map<String, Integer> categoryMap = new HashMap<>();
        Map<String, Integer> locationFrequency = new HashMap<>();
        Map<String, LatLng> validLocations = new HashMap<>();

        android.location.Geocoder geocoder = new android.location.Geocoder(this);
        int geocoderCount = 0;
        final int MAX_GEOCODE_ATTEMPTS = 15; // Limit to prevent timeouts

        for (QueryDocumentSnapshot doc : snapshots) {
            try {
                // SKIP Completed/Closed reports from "Active Issues" logic if desired
                // But for Analytics we usually want ALL history.
                // Let's stick to the previous logic: if status is closed, maybe skip strictly
                // active counts?
                // The previous code skipped Completed/Closed for totalIssues count. preserving
                // that.
                String status = doc.getString("status");
                if (status != null && (status.equalsIgnoreCase("Completed") || status.equalsIgnoreCase("Closed"))) {
                    // Still might want them for Historical Trends?
                    // Previous code: "continue" -> skipped entirely. strict adherence to previous
                    // logic:
                    continue;
                }

                result.totalIssues++;

                // 1. Coordinates (STRICT: Only use pre-existing Lat/Lng)
                Double lat = doc.getDouble("latitude");
                Double lng = doc.getDouble("longitude");
                String location = doc.getString("location");

                if (location != null && lat != null && lng != null) {
                    validLocations.put(location, new LatLng(lat, lng));
                    locationCache.put(location, new LatLng(lat, lng)); // Update cache with DB data
                } else if (location != null && !location.isEmpty()) {
                    // 1. Check Cache
                    if (locationCache.containsKey(location)) {
                        validLocations.put(location, locationCache.get(location));
                    }
                    // 2. Fallback to Geocoding if not in DB and not in Cache
                    else if (!validLocations.containsKey(location)) {
                        try {
                            if (geocoderCount < MAX_GEOCODE_ATTEMPTS) {
                                List<android.location.Address> addresses = geocoder.getFromLocationName(location, 1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    double gLat = addresses.get(0).getLatitude();
                                    double gLng = addresses.get(0).getLongitude();
                                    LatLng coord = new LatLng(gLat, gLng);
                                    validLocations.put(location, coord);
                                    locationCache.put(location, coord); // Save to cache
                                    geocoderCount++;
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Geocoding failed for: " + location);
                        }
                    }
                }

                // If we now have a valid location (either from DB or Geocoding), count it
                if (validLocations.containsKey(location)) {
                    // Severity Weighting
                    String severity = doc.getString("damage_severity");
                    String dLevel = doc.getString("damage_Level");
                    boolean isMajor = (severity != null && severity.equalsIgnoreCase("MAJOR")) ||
                            (dLevel != null && dLevel.equalsIgnoreCase("MAJOR")) ||
                            (dLevel != null && dLevel.equalsIgnoreCase("HIGH"));

                    locationFrequency.put(location,
                            locationFrequency.getOrDefault(location, 0) + (isMajor ? 3 : 1));
                }

                // 2. Weekly Trends
                com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                if (timestamp != null) {
                    long time = timestamp.toDate().getTime();
                    long weekIndex = time / (1000L * 60 * 60 * 24 * 7);
                    weeklyCountMap.put(weekIndex, weeklyCountMap.getOrDefault(weekIndex, 0) + 1);
                }

                // 3. Categories
                String cat = doc.getString("damage_Category");
                if (cat != null && !cat.isEmpty()) {
                    categoryMap.put(cat, categoryMap.getOrDefault(cat, 0) + 1);
                }

            } catch (Exception e) {
                // Skip malformed document, don't crash thread
                Log.w(TAG, "Skipping bad doc: " + doc.getId());
            }
        }

        // --- Build derived lists from maps ---

        // Trend Lines
        ArrayList<Long> sortedWeeks = new ArrayList<>(weeklyCountMap.keySet());
        Collections.sort(sortedWeeks);
        ArrayList<Integer> weeklyCounts = new ArrayList<>();
        int idx = 0;
        for (Long week : sortedWeeks) {
            int count = weeklyCountMap.get(week);
            weeklyCounts.add(count);
            result.trendEntries.add(new Entry(idx++, count));
        }
        // Ensure at least 2 points for valid line
        if (result.trendEntries.size() == 1) {
            Entry first = result.trendEntries.get(0);
            result.trendEntries.add(new Entry(first.getX() + 1, first.getY()));
        }

        // Heatmap Lists
        for (String loc : locationFrequency.keySet()) {
            int count = locationFrequency.get(loc);
            LatLng pt = validLocations.get(loc);
            if (pt == null)
                continue;

            if (count >= 5) {
                result.highRiskCount++;
                for (int i = 0; i < count; i++)
                    result.highRiskPoints.add(pt);
            } else if (count >= 2) {
                for (int i = 0; i < count; i++)
                    result.mediumRiskPoints.add(pt);
            } else {
                result.lowRiskPoints.add(pt);
            }
        }

        // Top Categories
        result.topCategories.addAll(categoryMap.entrySet());
        Collections.sort(result.topCategories, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        return result;
    }

    // --- UI THREAD: Apply Updates ---
    private void applyResult(AnalyticsResult result) {
        if (isFinishing() || isDestroyed())
            return;

        this.currentResult = result;

        // 1. Text Views
        tvTotalIssues.setText(String.valueOf(result.totalIssues));
        tvHighRisk.setText(String.valueOf(result.highRiskCount));

        // 1.5 Workflow Efficiency (Status Distribution)
        updateStatusDistribution(result);

        // 2. Chart
        setupChart(result.trendEntries);

        // 3. Categories
        updateCategoryRow(result.topCategories, 0, R.id.tvCat1Name, R.id.tvCat1Count, R.id.pbCat1);
        updateCategoryRow(result.topCategories, 1, R.id.tvCat2Name, R.id.tvCat2Count, R.id.pbCat2);
        updateCategoryRow(result.topCategories, 2, R.id.tvCat3Name, R.id.tvCat3Count, R.id.pbCat3);

        // 4. Map (if ready)
        if (isMapReady.get() && mMap != null) {
            updateMapOverlays(result);
        }
    }

    private void setupChart(List<Entry> entries) {
        if (entries.isEmpty())
            return;

        try {
            LineDataSet dataSet = new LineDataSet(entries, "Maintenance Trend");
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setCubicIntensity(0.2f);
            dataSet.setLineWidth(3f);
            dataSet.setColor(android.graphics.Color.parseColor("#001492"));
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(android.graphics.Color.parseColor("#001492"));
            dataSet.setFillAlpha(30);

            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);

            lineChart.getXAxis().setDrawLabels(false);
            lineChart.getAxisRight().setEnabled(false);
            lineChart.getDescription().setEnabled(false);
            lineChart.setTouchEnabled(false);

            lineChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Chart Error", e);
        }
    }

    private void updateStatusDistribution(AnalyticsResult result) {
        try {
            int total = result.countNew + result.countProgress + result.countCompleted;
            if (total == 0)
                return;

            View vNew = findViewById(R.id.viewStatusNew);
            View vProg = findViewById(R.id.viewStatusProgress);
            View vComp = findViewById(R.id.viewStatusCompleted);

            TextView tNew = findViewById(R.id.tvLabelNew);
            TextView tProg = findViewById(R.id.tvLabelProgress);
            TextView tComp = findViewById(R.id.tvLabelCompleted);

            // Calculate weights
            float wNew = (float) result.countNew / total;
            float wProg = (float) result.countProgress / total;
            float wComp = (float) result.countCompleted / total;

            // Update layout weights
            ((android.widget.LinearLayout.LayoutParams) vNew.getLayoutParams()).weight = wNew > 0 ? wNew : 0.01f;
            ((android.widget.LinearLayout.LayoutParams) vProg.getLayoutParams()).weight = wProg > 0 ? wProg : 0.01f;
            ((android.widget.LinearLayout.LayoutParams) vComp.getLayoutParams()).weight = wComp > 0 ? wComp : 0.01f;

            vNew.requestLayout();

            // Update Labels
            tNew.setText("New: " + Math.round(wNew * 100) + "%");
            tProg.setText("In Progress: " + Math.round(wProg * 100) + "%");
            tComp.setText("Completed: " + Math.round(wComp * 100) + "%");

        } catch (Exception e) {
            Log.e(TAG, "Status Dist Error", e);
        }
    }

    private void updateCategoryRow(List<Map.Entry<String, Integer>> list, int index, int nameId, int countId,
            int pbId) {
        try {
            TextView tvName = findViewById(nameId);
            TextView tvCount = findViewById(countId);
            android.widget.ProgressBar pb = findViewById(pbId);
            View parent = (View) tvName.getParent();

            if (index < list.size()) {
                Map.Entry<String, Integer> entry = list.get(index);
                int max = list.get(0).getValue();

                tvName.setText(entry.getKey());
                tvCount.setText(entry.getValue() + " reports");
                pb.setMax(max);
                pb.setProgress(entry.getValue());
                parent.setVisibility(View.VISIBLE);
            } else {
                parent.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Category Update Error", e);
        }
    }

    private void updateMapOverlays(AnalyticsResult result) {
        if (result == null || mMap == null)
            return;

        try {
            mMap.clear(); // Clear old overlays

            addHeatmap(result.highRiskPoints,
                    new int[] { android.graphics.Color.rgb(255, 0, 0), android.graphics.Color.rgb(255, 80, 80) },
                    60, 0.8f);

            addHeatmap(result.mediumRiskPoints,
                    new int[] { android.graphics.Color.rgb(255, 165, 0), android.graphics.Color.rgb(255, 200, 100) },
                    50, 0.7f);

            addHeatmap(result.lowRiskPoints,
                    new int[] { android.graphics.Color.rgb(0, 180, 0), android.graphics.Color.rgb(120, 220, 120) },
                    40, 0.6f);

        } catch (Exception e) {
            Log.e(TAG, "Map Overlay Error", e);
        }
    }

    private void addHeatmap(List<LatLng> points, int[] colors, int radius, float opacity) {
        if (points == null || points.isEmpty())
            return;

        try {
            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                    .data(points)
                    .radius(radius)
                    .opacity(opacity)
                    .gradient(new com.google.maps.android.heatmaps.Gradient(colors, new float[] { 0.2f, 1f }))
                    .build();

            mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
        } catch (Exception e) {
            Log.e(TAG, "Heatmap Creation Error", e);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        isMapReady.set(true);

        LatLng malaysia = new LatLng(3.1390, 101.6869);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(malaysia, 10f));

        if (currentResult != null) {
            updateMapOverlays(currentResult);
        }
    }
}
