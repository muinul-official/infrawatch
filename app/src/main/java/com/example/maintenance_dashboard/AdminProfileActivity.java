package com.example.maintenance_dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        db = FirebaseFirestore.getInstance("infrawatch");
        auth = FirebaseAuth.getInstance();

        initViews();
        loadAdminData();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvName = findViewById(R.id.tvAdminProfileName);
        tvEmail = findViewById(R.id.tvAdminProfileEmail);

        findViewById(R.id.btnAdminProfileMonitor).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });

        findViewById(R.id.btnAdminProfileAnalytics).setOnClickListener(v -> {
            startActivity(new Intent(this, AnalyticsActivity.class));
        });

        findViewById(R.id.btnAdminProfileUsers).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
        });

        findViewById(R.id.btnAdminLogout).setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadAdminData() {
        String uid = auth.getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(document -> {
                if (document.exists()) {
                    String name = document.getString("fullName");
                    String email = document.getString("email");
                    tvName.setText(name != null ? name : "Administrator");
                    tvEmail.setText(email != null ? email : "admin@infrawatch.com");
                }
            }).addOnFailureListener(e -> {
                tvName.setText("Administrator");
                tvEmail.setText("Error loading profile");
            });
        }
    }
}
