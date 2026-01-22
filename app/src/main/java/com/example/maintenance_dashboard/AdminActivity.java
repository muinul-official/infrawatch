package com.example.maintenance_dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    TextView tvTotalUsers, tvTotalReports, tvPendingReports;
    CardView cardManageUsers, cardViewReports, cardSettings, cardViewAnalytics;
    Button btnLogout;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance("infrawatch");

        // Initialize views
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalReports = findViewById(R.id.tvTotalReports);
        tvPendingReports = findViewById(R.id.tvPendingReports);
        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardViewReports = findViewById(R.id.cardViewReports);
        cardSettings = findViewById(R.id.cardSettings);
        cardViewAnalytics = findViewById(R.id.cardViewAnalytics);
        btnLogout = findViewById(R.id.btnLogout);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.ivProfileContainer).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminProfileActivity.class));
        });

        // Load admin info and stats
        loadAdminInfo();
        loadSystemStats();

        // Card click listeners
        cardManageUsers.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
        });

        cardViewReports.setOnClickListener(v -> {
            // Navigate to Dashboard (MainActivity)
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        });

        cardViewAnalytics.setOnClickListener(v -> {
            // Navigate to Analytics Activity
            Intent intent = new Intent(getApplicationContext(), AnalyticsActivity.class);
            startActivity(intent);
        });

        cardSettings.setOnClickListener(v -> {
            // TODO: Navigate to Settings Activity
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        });

        // Logout button
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(AdminActivity.this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        mAuth.signOut();
                        Intent intent = new Intent(getApplicationContext(), Login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // Back navigation is handled by the default behavior and the toolbar navigation
        // icon
    }

    private void loadSystemStats() {
        // Fetch total users
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (tvTotalUsers != null) {
                tvTotalUsers.setText(String.valueOf(queryDocumentSnapshots.size()));
            }
        });

        // Fetch total and pending reports
        db.collection("reports").addSnapshotListener((value, error) -> {
            if (error != null)
                return;
            if (value != null) {
                int total = value.size();
                int pending = 0;
                for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                    String status = doc.getString("status");
                    if ("Pending".equalsIgnoreCase(status) || "Draft".equalsIgnoreCase(status)) {
                        pending++;
                    }
                }
                if (tvTotalReports != null)
                    tvTotalReports.setText(String.valueOf(total));
                if (tvPendingReports != null)
                    tvPendingReports.setText(String.valueOf(pending));
            }
        });
    }

    private void loadAdminInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            // User data loaded successfully - no UI updates needed
        }
    }
}
