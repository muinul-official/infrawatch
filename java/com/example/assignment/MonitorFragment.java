package com.example.assignment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;
import android.widget.Toast;

public class MonitorFragment extends Fragment {

    // CORRECTED ID: The RecyclerView ID in fragment_monitor.xml is 'recycler_reports'.
    private static final int REPORTS_RECYCLERVIEW_ID = R.id.recycler_reports;

    public MonitorFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment (fragment_monitor.xml)
        View view = inflater.inflate(R.layout.fragment_monitor, container, false);

        // Initialize and set up the reports RecyclerView
        setupReportsRecyclerView(view);

        // You should also implement a method here to populate the summary cards
        // using the IDs: R.id.card_completed, R.id.card_new_reports, R.id.card_in_progress

        return view;
    }

    /**
     * Initializes the RecyclerView for recent reports.
     */
    private void setupReportsRecyclerView(View view) {
        // 1. Get the RecyclerView instance using the CORRECT ID: R.id.recycler_reports
        RecyclerView reportsRecyclerView = view.findViewById(REPORTS_RECYCLERVIEW_ID);

        // Crucial check: if the view is null, stop processing.
        if (reportsRecyclerView == null) {
            // Log an error or handle the unexpected case where the RecyclerView wasn't found
            // System.err.println("Error: RecyclerView not found with ID R.id.recycler_reports");
            return;
        }

        // 2. Sample Data
        List<ReportModel> sampleReports = Arrays.asList(
                new ReportModel("#RPT12345", "Roads & Transportation", "123 Main St, Cityville", "New"),
                new ReportModel("#RPT12344", "Water & Health", "Central Park West", "In Progress"),
                new ReportModel("#RPT12343", "Water Leakage", "789 Oak Lane", "Completed"),
                new ReportModel("#RPT12342", "Building Maintenance", "45 Industrial Ave", "New"),
                new ReportModel("#RPT12341", "Environmental Hazard", "Riverbank Area", "In Progress")
        );

        // 3. Define the Layout Manager
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 4. Create Adapter and set click handler
        ReportAdapter adapter = new ReportAdapter(sampleReports, report -> {
            // Example click action
            Toast.makeText(getContext(), "Report Clicked: " + report.getId(), Toast.LENGTH_SHORT).show();
        });

        reportsRecyclerView.setAdapter(adapter);
    }
}
