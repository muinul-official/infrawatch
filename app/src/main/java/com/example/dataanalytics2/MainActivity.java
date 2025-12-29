package com.example.dataanalytics2;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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

import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import android.location.Address;
import android.location.Geocoder;
import java.util.List;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private TextView tvTotalIssues, tvHighRisk, tvPrediction;
    private LineChart lineChart;
    private GoogleMap mMap;

    private FirebaseFirestore db;

    int totalIssues = 0;
    int highRiskCount = 0;

    private ArrayList<LatLng> highRiskPoints = new ArrayList<>();
    private ArrayList<LatLng> mediumRiskPoints = new ArrayList<>();
    private ArrayList<LatLng> lowRiskPoints = new ArrayList<>();

    private TileOverlay highRiskOverlay;
    private TileOverlay mediumRiskOverlay;
    private TileOverlay lowRiskOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTotalIssues = findViewById(R.id.tvTotalIssues);
        tvHighRisk = findViewById(R.id.tvHighRisk);
        tvPrediction = findViewById(R.id.tvPrediction);
        lineChart = findViewById(R.id.lineChart);

        db = FirebaseFirestore.getInstance("infrawatch");

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadAnalyticsFromFirebase();
    }

    private void loadAnalyticsFromFirebase() {
        Log.d("CHART_DEBUG", "loadAnalyticsFromFirebase() CALLED");

        db.collection("reports")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("CHART_DEBUG", "Firestore SUCCESS");
                    Log.d("CHART_DEBUG", "Documents = " + queryDocumentSnapshots.size());

                    Log.d("CHART_DEBUG", "Documents = " + queryDocumentSnapshots.size());

                    ArrayList<Entry> trendEntries = new ArrayList<>();
                    ArrayList<Integer> weeklyCounts = new ArrayList<>();

                    totalIssues = 0;
                    highRiskCount = 0;


                    Map<String, Integer> locationFrequency = new HashMap<>();
                    Map<String, LatLng> locationLatLngMap = new HashMap<>();
                    Map<Long, Integer> weeklyCountMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        totalIssues++;

                        String location = doc.getString("location");

                        // --- WEEKLY AGGREGATION ---
                        if (doc.getTimestamp("timestamp") != null) {
                            long time = doc.getTimestamp("timestamp")
                                    .toDate()
                                    .getTime();

                            long weekIndex = time / (1000L * 60 * 60 * 24 * 7);

                            weeklyCountMap.put(
                                    weekIndex,
                                    weeklyCountMap.getOrDefault(weekIndex, 0) + 1
                            );
                        }

                        // --- LOCATION FREQUENCY ---
                        if (location != null) {
                            locationFrequency.put(
                                    location,
                                    locationFrequency.getOrDefault(location, 0) + 1
                            );

                            if (!locationLatLngMap.containsKey(location)) {
                                try {
                                    Geocoder geocoder = new Geocoder(this);
                                    List<Address> addresses =
                                            geocoder.getFromLocationName(location, 1);

                                    if (addresses != null && !addresses.isEmpty()) {
                                        locationLatLngMap.put(
                                                location,
                                                new LatLng(
                                                        addresses.get(0).getLatitude(),
                                                        addresses.get(0).getLongitude()
                                                )
                                        );
                                    }
                                } catch (Exception e) {
                                    Log.e("Heatmap", "Geocoding failed", e);
                                }
                            }
                        }
                    }

                    ArrayList<Long> sortedWeeks = new ArrayList<>(weeklyCountMap.keySet());
                    java.util.Collections.sort(sortedWeeks);

                    weeklyCounts.clear();
                    trendEntries.clear();

                    int index = 0;
                    for (Long week : sortedWeeks) {
                        int count = weeklyCountMap.get(week);
                        weeklyCounts.add(count);
                        trendEntries.add(new Entry(index++, count));
                    }

                    if (trendEntries.size() == 1) {
                        Entry first = trendEntries.get(0);
                        trendEntries.add(new Entry(first.getX() + 1, first.getY()));
                    }


                    highRiskPoints.clear();
                    mediumRiskPoints.clear();
                    lowRiskPoints.clear();
                    highRiskCount = 0;

                    for (String loc : locationFrequency.keySet()) {
                        int count = locationFrequency.get(loc);
                        LatLng point = locationLatLngMap.get(loc);

                        if (point == null) continue;

                        if (count >= 5) {
                            // 🔴 HIGH RISK
                            highRiskCount++;
                            for (int i = 0; i < count; i++) {
                                highRiskPoints.add(point);
                            }
                        } else if (count >= 2) {
                            // 🟠 MEDIUM RISK
                            for (int i = 0; i < count; i++) {
                                mediumRiskPoints.add(point);
                            }
                        } else {
                            // 🟢 LOW RISK
                            lowRiskPoints.add(point);
                        }
                    }

                    // --- SIMPLE PREDICTION ---
                    int predictedNextWeek = 0;

                    if (weeklyCounts.size() >= 2) {
                        int totalIncrease = 0;
                        for (int i = 1; i < weeklyCounts.size(); i++) {
                            totalIncrease += (weeklyCounts.get(i) - weeklyCounts.get(i - 1));
                        }
                        int avgIncrease = totalIncrease / (weeklyCounts.size() - 1);
                        predictedNextWeek =
                                weeklyCounts.get(weeklyCounts.size() - 1) + avgIncrease;
                    }

                    if (weeklyCounts.size() >= 2) {
                        if (predictedNextWeek > weeklyCounts.get(weeklyCounts.size()-1)) {
                            tvPrediction.setText("Maintenance demand is expected to increase based on recent weekly trends.");
                        } else {
                            tvPrediction.setText("Maintenance demand is stable for the upcoming week.");
                        }
                    } else {
                        tvPrediction.setText("Analyzing data... check back soon for planning insights.");
                    }

                        // Remove existing overlays
                        if (highRiskOverlay != null) highRiskOverlay.remove();
                        if (mediumRiskOverlay != null) mediumRiskOverlay.remove();
                        if (lowRiskOverlay != null) lowRiskOverlay.remove();

                        // 🔴 HIGH RISK - RED
                        if (!highRiskPoints.isEmpty()) {
                            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                                    .data(highRiskPoints)
                                    .radius(60)
                                    .opacity(0.8f)
                                    .gradient(new com.google.maps.android.heatmaps.Gradient(
                                            new int[]{
                                                    android.graphics.Color.rgb(255, 0, 0),
                                                    android.graphics.Color.rgb(255, 80, 80)
                                            },
                                            new float[]{0.2f, 1f}
                                    ))
                                    .build();

                            highRiskOverlay = mMap.addTileOverlay(
                                    new TileOverlayOptions().tileProvider(provider)
                            );
                        }

                        // 🟠 MEDIUM RISK - ORANGE
                        if (!mediumRiskPoints.isEmpty()) {
                            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                                    .data(mediumRiskPoints)
                                    .radius(50)
                                    .opacity(0.7f)
                                    .gradient(new com.google.maps.android.heatmaps.Gradient(
                                            new int[]{
                                                    android.graphics.Color.rgb(255, 165, 0),
                                                    android.graphics.Color.rgb(255, 200, 100)
                                            },
                                            new float[]{0.2f, 1f}
                                    ))
                                    .build();

                            mediumRiskOverlay = mMap.addTileOverlay(
                                    new TileOverlayOptions().tileProvider(provider)
                            );
                        }

                        // 🟢 LOW RISK - GREEN
                        if (!lowRiskPoints.isEmpty()) {
                            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                                    .data(lowRiskPoints)
                                    .radius(40)
                                    .opacity(0.6f)
                                    .gradient(new com.google.maps.android.heatmaps.Gradient(
                                            new int[]{
                                                    android.graphics.Color.rgb(0, 180, 0),
                                                    android.graphics.Color.rgb(120, 220, 120)
                                            },
                                            new float[]{0.2f, 1f}
                                    ))
                                    .build();

                            lowRiskOverlay = mMap.addTileOverlay(
                                    new TileOverlayOptions().tileProvider(provider)
                            );
                        }

                    // --- UI ---
                    tvTotalIssues.setText(String.valueOf(totalIssues));
                    tvHighRisk.setText(String.valueOf(highRiskCount));

                    LineDataSet dataSet = new LineDataSet(trendEntries, "Maintenance Trend");
                    dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    dataSet.setCubicIntensity(0.2f);

                    dataSet.setLineWidth(3f);
                    dataSet.setColor(android.graphics.Color.parseColor("#2196F3"));

                    dataSet.setDrawCircles(false);
                    dataSet.setDrawValues(false);

                    dataSet.setDrawFilled(true);
                    dataSet.setFillColor(android.graphics.Color.parseColor("#DDEBFF"));
                    dataSet.setFillAlpha(180);


                    lineChart.setData(new LineData(dataSet));
                    lineChart.getXAxis().setAxisMinimum(0f);
                    lineChart.setVisibleXRangeMaximum(10);
                    lineChart.getXAxis().setDrawLabels(false);
                    lineChart.getAxisRight().setEnabled(false);
                    lineChart.getDescription().setEnabled(false);
                    lineChart.invalidate();
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error reading data", e)
                );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        final androidx.core.widget.NestedScrollView mainScroll = findViewById(R.id.main);

        mMap.setOnCameraMoveStartedListener(reason -> {
            if (mainScroll != null) {
                mainScroll.requestDisallowInterceptTouchEvent(true);
            }
        });

        mMap.setOnCameraIdleListener(() -> {
            if (mainScroll != null) {
                mainScroll.requestDisallowInterceptTouchEvent(false);
            }
        });

        LatLng malaysia = new LatLng(3.1390, 101.6869);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(malaysia, 10f));
    }
}
