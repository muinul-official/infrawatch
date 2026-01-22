package com.example.maintenance_dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class UserReportsActivity extends AppCompatActivity {

    private static final String TAG = "UserReportsActivity";
    private static final String FIRESTORE_DATABASE_ID = "infrawatch";

    private FirebaseFirestore db;
    private ListenerRegistration reportListener;
    private RecyclerView rvUserReports;
    private TextView tvNoReports;
    private ReportGridAdapter adapter;
    private List<Report> reportList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_reports);

        db = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FIRESTORE_DATABASE_ID);

        initViews();
        loadUserReports();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvUserReports = findViewById(R.id.rvUserReports);
        tvNoReports = findViewById(R.id.tvNoUserReports);

        rvUserReports.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ReportGridAdapter(this, reportList);
        rvUserReports.setAdapter(adapter);
    }

    private void loadUserReports() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        reportListener = db.collection("reports")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading user reports", error);
                        Toast.makeText(this, "Error loading reports", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (querySnapshot != null) {
                        reportList.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            try {
                                Report report = doc.toObject(Report.class);
                                if (report != null) {
                                    report.setDocumentId(doc.getId());
                                    reportList.add(report);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing report: " + doc.getId(), e);
                            }
                        }

                        adapter.notifyDataSetChanged();

                        if (reportList.isEmpty()) {
                            tvNoReports.setVisibility(View.VISIBLE);
                        } else {
                            tvNoReports.setVisibility(View.GONE);
                        }
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
