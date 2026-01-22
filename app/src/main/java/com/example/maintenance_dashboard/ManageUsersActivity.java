package com.example.maintenance_dashboard;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maintenance_dashboard.adapter.UserAdapter;
import com.example.maintenance_dashboard.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView rvUserList;
    private ProgressBar pbLoading;
    private UserAdapter adapter;
    private List<User> userList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        db = FirebaseFirestore.getInstance("infrawatch");
        initViews();
        fetchUsers();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvUserList = findViewById(R.id.rvUserList);
        pbLoading = findViewById(R.id.pbLoading);

        userList = new ArrayList<>();
        adapter = new UserAdapter(userList);
        rvUserList.setLayoutManager(new LinearLayoutManager(this));
        rvUserList.setAdapter(adapter);
    }

    private void fetchUsers() {
        pbLoading.setVisibility(View.VISIBLE);
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            userList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                User user = document.toObject(User.class);
                userList.add(user);
            }
            adapter.notifyDataSetChanged();
            pbLoading.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            pbLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
        });
    }
}
