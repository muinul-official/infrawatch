package com.example.assignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.os.Bundle;
import android.view.MenuItem;

/**
 * DashboardActivity: The main activity that hosts the bottom navigation and
 * dynamically swaps different Fragment views (Monitor, Reports, Analytics, Alerts)
 * into the 'fragment_container' (FrameLayout).
 */
public class DashboardActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_dashboard.xml only contains the FrameLayout and the BottomNavigationView
        setContentView(R.layout.activity_dashboard);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        // Set the listener to this activity to handle tab clicks
        bottomNavigationView.setOnItemSelectedListener(this);

        // Load the default fragment (MonitorFragment) only on the first launch
        // savedInstanceState check prevents fragment duplication on screen rotation
        if (savedInstanceState == null) {
            // Programmatically select the default tab
            bottomNavigationView.setSelectedItemId(R.id.nav_monitor);
            loadFragment(new MonitorFragment());
        }
    }

    /**
     * Handles the Fragment transaction to swap the current screen content.
     * @param fragment The Fragment instance to display.
     * @return true if the fragment was loaded, false otherwise.
     */
    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    // R.id.fragment_container is the FrameLayout in activity_dashboard.xml
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    /**
     * Listener method triggered when a tab icon is clicked.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;

        int itemId = item.getItemId();

        // 1. YOUR RESPONSIBILITY: Monitoring Dashboard
        if (itemId == R.id.nav_monitor) {
            fragment = new MonitorFragment();
            // 2. YOUR RESPONSIBILITY: Reports List
        } else if (itemId == R.id.nav_reports) {
            fragment = new ReportsFragment();
            // 3. COLLEAGUE'S RESPONSIBILITY: Analytics (Placeholder)
        } else if (itemId == R.id.nav_analytics) {
            fragment = new AnalyticsFragment();
            // 4. COLLEAGUE'S RESPONSIBILITY: Alerts (Placeholder)
        } else if (itemId == R.id.nav_notifications) {
            fragment = new AlertsFragment();
        }

        return loadFragment(fragment);
    }
}
