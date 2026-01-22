package com.example.maintenance_dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        db = FirebaseFirestore.getInstance("infrawatch");
        auth = FirebaseAuth.getInstance();

        initViews();
        loadUserData();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);

        findViewById(R.id.btnProfileMyReports).setOnClickListener(v -> {
            startActivity(new Intent(this, UserReportsActivity.class));
        });

        findViewById(R.id.btnProfileLogout).setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        String uid = auth.getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(document -> {
                if (document.exists()) {
                    String name = document.getString("fName");
                    String email = document.getString("email");
                    tvName.setText(name != null ? name : "User");
                    tvEmail.setText(email != null ? email : "No email available");
                }
            }).addOnFailureListener(e -> {
                tvName.setText("User");
                tvEmail.setText("Error loading profile");
            });
        }
    }
}
