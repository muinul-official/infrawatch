package com.example.maintenance_dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maintenance_dashboard.data.AppNotification;
import com.example.maintenance_dashboard.ui.NotificationAdapter;
import com.example.maintenance_dashboard.ui.NotificationListViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

public class NotificationListActivity extends AppCompatActivity
        implements NotificationAdapter.OnNotificationClickListener {

    private NotificationListViewModel viewModel;
    private NotificationAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private MaterialToolbar toolbar;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.notifications_recyclerview);
        emptyView = findViewById(R.id.empty_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NotificationAdapter(this);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(NotificationListViewModel.class);
        viewModel.getNotifications().observe(this, notifications -> {
            adapter.submitList(notifications);
            if (notifications == null || notifications.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        });

        setupBottomNavigation();
        toolbar.setNavigationOnClickListener(v -> handleBackNavigation());

        // Ensure back button in toolbar works when setSupportActionBar is used
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!adapter.getSelectedIds().isEmpty()) {
                    adapter.clearSelection();
                    updateToolbar();
                } else {
                    finish();
                }
            }
        });
    }

    private void setupBottomNavigation() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(
                R.id.bottomNavigationView);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_notification);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_monitor) {
                    Intent intent = new Intent(NotificationListActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_analytics) {
                    Intent intent = new Intent(NotificationListActivity.this, AnalyticsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_reports) {
                    Intent intent = new Intent(NotificationListActivity.this, ReportsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_notification) {
                    return true;
                }
                return false;
            });
        }
    }

    private void handleBackNavigation() {
        if (!adapter.getSelectedIds().isEmpty()) {
            adapter.clearSelection();
            updateToolbar();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notification_list_menu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            handleBackNavigation();
            return true;
        } else if (itemId == R.id.action_clear_all) {
            showClearAllConfirmationDialog();
            return true;
        } else if (itemId == R.id.action_clear_seen) {
            showClearSeenConfirmationDialog();
            return true;
        } else if (itemId == R.id.action_delete) {
            showDeleteSelectedConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        handleBackNavigation();
        return true;
    }

    private void showClearAllConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear all notifications?")
                .setMessage("Are you sure you want to clear all notifications? This action cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear all", (dialog, which) -> viewModel.clearAllNotifications())
                .show();
    }

    private void showClearSeenConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear seen notifications?")
                .setMessage("Only notifications you have already viewed will be removed. Continue?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear seen", (dialog, which) -> viewModel.clearSeenNotifications())
                .show();
    }

    private void showDeleteSelectedConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete selected notifications?")
                .setMessage("Are you sure you want to delete the selected notifications?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteNotificationsByIds(new ArrayList<>(adapter.getSelectedIds()));
                    adapter.clearSelection();
                    updateToolbar();
                })
                .show();
    }

    @Override
    public void onNotificationClick(AppNotification notification) {
        viewModel.markAsRead(notification.id);
        Intent intent = new Intent(this, ReportDetailActivity.class);
        intent.putExtra(ReportDetailActivity.EXTRA_DOCUMENT_ID, notification.reportId);
        intent.putExtra("notifType", notification.type);
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged() {
        updateToolbar();
    }

    private void updateToolbar() {
        if (adapter.getSelectedIds().isEmpty()) {
            toolbar.setTitle("Notifications");
            menu.findItem(R.id.action_delete).setVisible(false);
            menu.findItem(R.id.action_clear_all).setVisible(true);
            menu.findItem(R.id.action_clear_seen).setVisible(true);
        } else {
            toolbar.setTitle(adapter.getSelectedIds().size() + " selected");
            menu.findItem(R.id.action_delete).setVisible(true);
            menu.findItem(R.id.action_clear_all).setVisible(false);
            menu.findItem(R.id.action_clear_seen).setVisible(false);
        }
    }
}
