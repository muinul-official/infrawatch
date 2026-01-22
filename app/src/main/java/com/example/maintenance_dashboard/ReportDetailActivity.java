package com.example.maintenance_dashboard;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportDetailActivity extends AppCompatActivity {

    private static final String TAG = "ReportDetailActivity";
    public static final String EXTRA_DOCUMENT_ID = "document_id";
    private static final String FIRESTORE_DATABASE_ID = "infrawatch";

    private FirebaseFirestore db;
    private String documentId;
    private Report currentReport;

    // Views
    private ImageButton btnBack;
    private ImageView ivReportImage;
    private TextView tvNoImage;
    private TextView tvReportId;
    private TextView tvCategory;
    private TextView tvLocation;
    private TextView tvDateTime;
    private TextView tvDescription;
    private TextView tvDamageLevel;
    private TextView tvIssueType;
    private TextView tvCurrentStatus;
    private Spinner spinnerStatus;
    private Button btnUpdateStatus;
    private TextView tvAssignedContractor;
    private Spinner spinnerContractor;
    private Button btnAssignContractor;
    private Button btnCloseTask;
    private androidx.cardview.widget.CardView cardManageProgress;
    private androidx.cardview.widget.CardView cardContractorAssignment;

    // Data
    private List<Contractor> contractorList;
    private List<String> contractorNames;
    private String[] statusOptions = {
            Report.STATUS_NEW,
            Report.STATUS_PENDING,
            Report.STATUS_IN_PROGRESS,
            Report.STATUS_COMPLETED
    };

    private com.google.firebase.auth.FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        // Initialize Firestore with the custom database ID "infrawatch" in
        // asia-southeast1
        db = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FIRESTORE_DATABASE_ID);
        auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        documentId = getIntent().getStringExtra(EXTRA_DOCUMENT_ID);

        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(this, "Error: Report not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadReportDetails();
        loadContractors();
        checkUserRole();
    }

    private void checkUserRole() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            if ("admin".equalsIgnoreCase(role)) {
                                // Show admin controls
                                cardManageProgress.setVisibility(View.VISIBLE);
                                cardContractorAssignment.setVisibility(View.VISIBLE);
                            } else {
                                // Hide admin controls for normal users
                                cardManageProgress.setVisibility(View.GONE);
                                cardContractorAssignment.setVisibility(View.GONE);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching user role", e));
        } else {
            // No user logged in, hide admin controls
            cardManageProgress.setVisibility(View.GONE);
            cardContractorAssignment.setVisibility(View.GONE);
        }
    }

    private void initViews() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivReportImage = findViewById(R.id.ivReportImage);
        tvNoImage = findViewById(R.id.tvNoImage);

        // Map included row layouts
        View rowId = findViewById(R.id.rowId);
        ((TextView) rowId.findViewById(R.id.tvLabel)).setText("Report ID:");
        tvReportId = rowId.findViewById(R.id.tvValue);

        View rowCategory = findViewById(R.id.rowCategory);
        ((TextView) rowCategory.findViewById(R.id.tvLabel)).setText("Category:");
        tvCategory = rowCategory.findViewById(R.id.tvValue);

        View rowDamageLevel = findViewById(R.id.rowDamageLevel);
        ((TextView) rowDamageLevel.findViewById(R.id.tvLabel)).setText("Damage Level:");
        tvDamageLevel = rowDamageLevel.findViewById(R.id.tvValue);

        View rowIssueType = findViewById(R.id.rowIssueType);
        ((TextView) rowIssueType.findViewById(R.id.tvLabel)).setText("Issue Type:");
        tvIssueType = rowIssueType.findViewById(R.id.tvValue);

        View rowLocation = findViewById(R.id.rowLocation);
        ((TextView) rowLocation.findViewById(R.id.tvLabel)).setText("Location:");
        tvLocation = rowLocation.findViewById(R.id.tvValue);

        View rowDateTime = findViewById(R.id.rowDateTime);
        ((TextView) rowDateTime.findViewById(R.id.tvLabel)).setText("Date/Time:");
        tvDateTime = rowDateTime.findViewById(R.id.tvValue);

        tvDescription = findViewById(R.id.tvDescription);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        tvAssignedContractor = findViewById(R.id.tvAssignedContractor);
        spinnerContractor = findViewById(R.id.spinnerContractor);
        btnAssignContractor = findViewById(R.id.btnAssignContractor);
        btnCloseTask = findViewById(R.id.btnCloseTask);

        cardManageProgress = findViewById(R.id.cardManageProgress);
        cardContractorAssignment = findViewById(R.id.cardContractorAssignment);

        // Default to GONE until role is verified
        cardManageProgress.setVisibility(View.GONE);
        cardContractorAssignment.setVisibility(View.GONE);

        // Setup status spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        // Initialize contractor list
        contractorList = new ArrayList<>();
        contractorNames = new ArrayList<>();
        contractorNames.add("Select a contractor...");
    }

    private void setupListeners() {
        btnUpdateStatus.setOnClickListener(v -> updateReportStatus());

        btnAssignContractor.setOnClickListener(v -> assignContractor());

        btnCloseTask.setOnClickListener(v -> showCloseTaskConfirmation());
    }

    private void loadReportDetails() {
        db.collection("reports").document(documentId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading report", error);
                        Toast.makeText(this, "Error loading report details", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        try {
                            currentReport = documentSnapshot.toObject(Report.class);
                            if (currentReport != null) {
                                currentReport.setDocumentId(documentSnapshot.getId());
                                displayReportDetails();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing report", e);
                            Toast.makeText(this, "Error parsing report data", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void displayReportDetails() {
        if (currentReport == null)
            return;

        // Report ID (document ID)
        tvReportId.setText(currentReport.getDocumentId() != null ? currentReport.getDocumentId() : "N/A");

        // Damage Category
        tvCategory.setText(currentReport.getDamageCategory() != null ? currentReport.getDamageCategory() : "N/A");

        // Location
        tvLocation.setText(currentReport.getLocation() != null ? currentReport.getLocation() : "N/A");

        // Date/Time from timestamp
        tvDateTime.setText(currentReport.getFormattedDate());

        // Description
        tvDescription.setText(
                currentReport.getDescription() != null ? currentReport.getDescription() : "No description provided");

        // Damage Level
        if (tvDamageLevel != null) {
            tvDamageLevel.setText(currentReport.getConsolidatedDamageLevel());
        }

        // Specific Issue Type
        if (tvIssueType != null) {
            tvIssueType.setText(
                    currentReport.getSpecificIssueType() != null ? currentReport.getSpecificIssueType() : "N/A");
        }

        // Status
        updateStatusDisplay();

        // Contractor
        if (currentReport.getContractorName() != null && !currentReport.getContractorName().isEmpty()) {
            tvAssignedContractor.setText(currentReport.getContractorName());
        } else {
            tvAssignedContractor.setText("Not Assigned");
        }

        // Image
        loadReportImage();

        // Set spinner to current status
        for (int i = 0; i < statusOptions.length; i++) {
            if (statusOptions[i].equals(currentReport.getStatus())) {
                spinnerStatus.setSelection(i);
                break;
            }
        }

        // Show/hide close task button
        updateCloseTaskButtonVisibility();
    }

    private void loadReportImage() {
        String imageUrl = currentReport.getFirstImageUrl();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            ivReportImage.setVisibility(View.VISIBLE);
            tvNoImage.setVisibility(View.GONE);

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.color.light_gray)
                    .error(R.color.light_gray)
                    .centerCrop()
                    .into(ivReportImage);
        } else {
            ivReportImage.setVisibility(View.GONE);
            tvNoImage.setVisibility(View.VISIBLE);
        }
    }

    private void updateStatusDisplay() {
        String status = currentReport.getStatus();
        tvCurrentStatus.setText(status != null ? status : "Unknown");

        // Update badge color based on status
        GradientDrawable background = (GradientDrawable) tvCurrentStatus.getBackground();
        int color;

        if (status != null) {
            switch (status) {
                case Report.STATUS_NEW:
                case Report.STATUS_PENDING:
                    color = Color.parseColor("#2196F3"); // Blue
                    break;
                case Report.STATUS_IN_PROGRESS:
                    color = Color.parseColor("#FF9800"); // Orange
                    break;
                case Report.STATUS_COMPLETED:
                    color = Color.parseColor("#4CAF50"); // Green
                    break;
                case Report.STATUS_CLOSED:
                    color = Color.parseColor("#9E9E9E"); // Gray
                    break;
                default:
                    color = Color.parseColor("#2196F3"); // Default blue
            }
        } else {
            color = Color.parseColor("#9E9E9E"); // Gray
        }

        background.setColor(color);
    }

    private void updateCloseTaskButtonVisibility() {
        if (currentReport != null && currentReport.canBeClosed()) {
            btnCloseTask.setVisibility(View.VISIBLE);
        } else {
            btnCloseTask.setVisibility(View.GONE);
        }

        // Disable editing if task is closed
        if (currentReport != null && Report.STATUS_CLOSED.equals(currentReport.getStatus())) {
            spinnerStatus.setEnabled(false);
            btnUpdateStatus.setEnabled(false);
            spinnerContractor.setEnabled(false);
            btnAssignContractor.setEnabled(false);
            btnCloseTask.setVisibility(View.GONE);
        }
    }

    private void loadContractors() {
        db.collection("contractors")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading contractors", error);
                        addDefaultContractors();
                        return;
                    }

                    contractorList.clear();
                    contractorNames.clear();
                    contractorNames.add("Select a contractor...");

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Contractor contractor = doc.toObject(Contractor.class);
                            if (contractor != null) {
                                contractor.setDocumentId(doc.getId());
                                contractorList.add(contractor);
                                contractorNames.add(contractor.toString());
                            }
                        }
                    } else {
                        addDefaultContractors();
                    }

                    updateContractorSpinner();
                });
    }

    private void addDefaultContractors() {
        List<Contractor> defaultContractors = new ArrayList<>();
        defaultContractors.add(new Contractor("Ahmad Contractor Services", "Roads & Transportation", "012-3456789",
                "ahmad@contractor.com"));
        defaultContractors.add(new Contractor("Bina Jaya Construction", "Public Facilities", "013-4567890",
                "binajaya@contractor.com"));
        defaultContractors.add(
                new Contractor("CleanPro Solutions", "Waste & Cleanliness", "014-5678901", "cleanpro@contractor.com"));
        defaultContractors.add(new Contractor("PowerTech Utilities", "Utilities & Services", "015-6789012",
                "powertech@contractor.com"));

        for (Contractor contractor : defaultContractors) {
            db.collection("contractors").add(contractor)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Added default contractor: " + contractor.getName());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding contractor", e);
                    });
        }

        contractorList.addAll(defaultContractors);
        for (Contractor c : defaultContractors) {
            contractorNames.add(c.toString());
        }
        updateContractorSpinner();
    }

    private void updateContractorSpinner() {
        ArrayAdapter<String> contractorAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                contractorNames);
        contractorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContractor.setAdapter(contractorAdapter);

        if (currentReport != null && currentReport.getAssignedContractor() != null) {
            for (int i = 0; i < contractorList.size(); i++) {
                if (contractorList.get(i).getDocumentId() != null &&
                        contractorList.get(i).getDocumentId().equals(currentReport.getAssignedContractor())) {
                    spinnerContractor.setSelection(i + 1);
                    break;
                }
            }
        }
    }

    private void updateReportStatus() {
        String selectedStatus = (String) spinnerStatus.getSelectedItem();

        if (selectedStatus == null || selectedStatus.equals(currentReport.getStatus())) {
            Toast.makeText(this, "Please select a different status", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", selectedStatus);

        db.collection("reports").document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Status updated to: " + selectedStatus, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Status updated successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating status", e);
                });
    }

    private void assignContractor() {
        int selectedPosition = spinnerContractor.getSelectedItemPosition();

        if (selectedPosition == 0) {
            Toast.makeText(this, "Please select a contractor", Toast.LENGTH_SHORT).show();
            return;
        }

        Contractor selectedContractor = contractorList.get(selectedPosition - 1);

        Map<String, Object> updates = new HashMap<>();
        updates.put("assignedContractor", selectedContractor.getDocumentId());
        updates.put("contractorName", selectedContractor.getName());

        // Also update status to In Progress if it's a new/pending report
        String currentStatus = currentReport.getStatus();
        if (Report.STATUS_NEW.equals(currentStatus) || Report.STATUS_PENDING.equals(currentStatus)) {
            updates.put("status", Report.STATUS_IN_PROGRESS);
        }

        db.collection("reports").document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Contractor assigned: " + selectedContractor.getName(), Toast.LENGTH_SHORT)
                            .show();
                    Log.d(TAG, "Contractor assigned successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error assigning contractor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error assigning contractor", e);
                });
    }

    private void showCloseTaskConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Close Task")
                .setMessage("Are you sure you want to close this task? This action cannot be undone.")
                .setPositiveButton("Close Task", (dialog, which) -> closeTask())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void closeTask() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Report.STATUS_CLOSED);

        db.collection("reports").document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task closed successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Task closed successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error closing task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error closing task", e);
                });
    }
}
