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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private TextView tvTotalIssues, tvHighRisk, tvPrediction;
    private LineChart lineChart;
    private GoogleMap mMap;

    private FirebaseFirestore db;

    int totalIssues = 0;
    int highRiskCount = 0;

    private HeatmapTileProvider heatmapProvider;
    private TileOverlay heatmapOverlay;
    private ArrayList<LatLng> highRiskPoints = new ArrayList<>();

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

                    int trendIndex = 0;
                    ArrayList<Entry> trendEntries = new ArrayList<>();
                    ArrayList<Integer> weeklyCounts = new ArrayList<>();

                    totalIssues = 0;
                    highRiskCount = 0;
                    highRiskPoints.clear();

                    long currentWeek = -1;
                    int currentWeekCount = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        totalIssues++;

                        String status = doc.getString("status");
                        String location = doc.getString("location");

                        boolean isHighRisk = status != null &&
                                status.toLowerCase().contains("pending");

                        if (isHighRisk) {
                            highRiskCount++;
                        }

                        trendEntries.add(new Entry(trendIndex++, totalIssues));

                        if (doc.getTimestamp("timestamp") != null) {
                            long time = doc.getTimestamp("timestamp")
                                    .toDate()
                                    .getTime();

                            long weekIndex = time / (1000L * 60 * 60 * 24 * 7);

                            if (currentWeek == -1) {
                                currentWeek = weekIndex;
                            }

                            if (weekIndex == currentWeek) {
                                currentWeekCount++;
                            } else {
                                weeklyCounts.add(currentWeekCount);
                                currentWeek = weekIndex;
                                currentWeekCount = 1;
                            }
                        }

                        // --- HEATMAP DATA ---
                        if (isHighRisk && location != null) {
                            try {
                                Geocoder geocoder = new Geocoder(this);
                                List<Address> addresses =
                                        geocoder.getFromLocationName(location, 1);

                                if (addresses != null && !addresses.isEmpty()) {
                                    LatLng point = new LatLng(
                                            addresses.get(0).getLatitude(),
                                            addresses.get(0).getLongitude()
                                    );
                                    highRiskPoints.add(point);
                                }
                            } catch (Exception e) {
                                Log.e("Heatmap", "Geocoding failed", e);
                            }
                        }
                    }

                    if (currentWeekCount > 0) {
                        weeklyCounts.add(currentWeekCount);
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
                            tvPrediction.setText("Maintenance demand is expected to increase next month in Area A and Bridge Zone");
                        } else {
                            tvPrediction.setText("Maintenance demand is stable for the upcoming week.");
                        }
                    } else {
                        tvPrediction.setText("Analyzing data... check back soon for planning insights.");
                    }

                    // --- HEATMAP ---
                    if (mMap != null && !highRiskPoints.isEmpty()) {
                        if (heatmapOverlay != null) {
                            heatmapOverlay.remove();
                        }

                        heatmapProvider = new HeatmapTileProvider.Builder()
                                .data(highRiskPoints)
                                .radius(50)
                                .opacity(0.7f)
                                .build();

                        heatmapOverlay = mMap.addTileOverlay(
                                new TileOverlayOptions().tileProvider(heatmapProvider)
                        );
                    }

                    // --- UI ---
                    tvTotalIssues.setText(String.valueOf(totalIssues));
                    tvHighRisk.setText(String.valueOf(highRiskCount));

                    LineDataSet dataSet = new LineDataSet(trendEntries, "Maintenance Trend");
                    dataSet.setLineWidth(2f);
                    dataSet.setDrawCircles(true);
                    dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    dataSet.setDrawFilled(true);
                    dataSet.setFillColor(android.graphics.Color.parseColor("#DDEBFF"));
                    dataSet.setColor(android.graphics.Color.parseColor("#2196F3"));
                    dataSet.setLineWidth(3f);
                    dataSet.setDrawCircles(false);


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
