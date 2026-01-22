package com.example.maintenance_dashboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.maintenance_dashboard.ai.TFLiteDamageAnalyzer;
import com.example.maintenance_dashboard.data.AppNotification;
import com.example.maintenance_dashboard.data.DamageAnalysisResult;
import com.example.maintenance_dashboard.data.NotificationRepository;
import com.example.maintenance_dashboard.utils.BitmapUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserMainActivity extends AppCompatActivity {

    private static final String TAG = "UserMainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_MEDIA_PICK = 1002;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // UI elements
    private AutoCompleteTextView autoCompleteCategory;
    private TextInputEditText editTextIssueType;
    private TextInputEditText editTextLocation;
    private TextInputEditText editTextDescription;
    private Button buttonSubmitReport;
    private Button buttonLogout; // Added logout button
    private CardView uploadCard;
    private TextView uploadStatusText;
    private ImageView ivPreview;
    private View uploadPrompt;

    // Location services
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isLocationRequested = false;
    private Location latestLocation = null;

    // Media tracking
    private Uri selectedMediaUri = null;

    // Media Picker Launcher
    private final androidx.activity.result.ActivityResultLauncher<Intent> mediaPickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null
                        && result.getData().getData() != null) {
                    selectedMediaUri = result.getData().getData();
                    processSelectedMedia();
                } else {
                    // User cancelled or no media selected
                    Toast.makeText(this, "No media selected.", Toast.LENGTH_SHORT).show();
                    uploadStatusText.setText("Supporting Photo/Video");
                    uploadStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                }
            });

    // Damage Analysis
    private TFLiteDamageAnalyzer damageAnalyzer;
    private DamageAnalysisResult damageAnalysisResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main); // Use the new layout

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(com.google.firebase.FirebaseApp.getInstance(), "infrawatch");
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check login status
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize UI components
        autoCompleteCategory = findViewById(R.id.autoCompleteCategory);
        editTextIssueType = findViewById(R.id.editTextIssueType);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextDescription = findViewById(R.id.editTextDescription);
        buttonSubmitReport = findViewById(R.id.buttonSubmitReport);
        uploadCard = findViewById(R.id.uploadCard);
        uploadStatusText = findViewById(R.id.uploadStatusText);
        ivPreview = findViewById(R.id.ivPreview);
        uploadPrompt = findViewById(R.id.uploadPrompt);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup Dropdown
        setupCategoryDropdown();
        autoCompleteCategory.setOnClickListener(v -> autoCompleteCategory.showDropDown());

        // Setup Click Listeners
        buttonSubmitReport.setOnClickListener(v -> submitReport());
        uploadCard.setOnClickListener(v -> openMediaChooser());

        // Request Permissions and Location on start
        checkPermissionsAndGetLocation();

        // Initialize Damage Analyzer
        try {
            damageAnalyzer = new TFLiteDamageAnalyzer(this);
        } catch (IOException e) {
            Log.e(TAG, "Error initializing TFLiteDamageAnalyzer", e);
            Toast.makeText(this, "Could not initialize AI model.", Toast.LENGTH_LONG).show();
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Report");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ------------------------------------
    // --- Location Handling Methods ---
    // ------------------------------------

    private void checkPermissionsAndGetLocation() {
        String[] requiredPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA
        };

        boolean allPermissionsGranted = true;
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean locationGranted = false;
            boolean cameraGranted = false;

            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true;
                }
                if (permissions[i].equals(Manifest.permission.CAMERA)
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    cameraGranted = true;
                }
            }

            if (locationGranted) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied. Please enter location manually.", Toast.LENGTH_LONG)
                        .show();
            }

            if (!cameraGranted) {
                Toast.makeText(this, "Camera permission denied. Cannot upload photos/videos.", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {
        if (isLocationRequested)
            return;

        LocationRequest locationRequest = new LocationRequest.Builder(10000)
                .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w(TAG, "Location result is null.");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        reverseGeocodeLocation(location);
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                        isLocationRequested = false;
                        return;
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        isLocationRequested = true;
        // Toast.makeText(this, "Attempting to auto-capture location...",
        // Toast.LENGTH_SHORT).show();
    }

    private void reverseGeocodeLocation(Location location) {
        this.latestLocation = location;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        sb.append(", ");
                    }
                }

                String fullAddress = sb.toString();
                editTextLocation.setText(fullAddress);
                Toast.makeText(this, "Location auto-captured!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Location Captured: " + fullAddress);

            } else {
                String coordinates = String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f",
                        location.getLatitude(), location.getLongitude());
                editTextLocation.setText(coordinates);
                Toast.makeText(this, "Location captured (Coordinates).", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoding failed: " + e.getMessage());
            Toast.makeText(this, "Location captured, but geocoding failed.", Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------
    // --- Media Upload Handling Methods ---
    // ------------------------------------

    private void openMediaChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(intent, "Select Photo");
        mediaPickerLauncher.launch(chooserIntent);
    }

    private void processSelectedMedia() {
        try {
            Bitmap bitmap = BitmapUtils.getBitmapFromUri(this, selectedMediaUri);
            if (damageAnalyzer != null) {
                damageAnalysisResult = damageAnalyzer.analyze(bitmap);
                String severityFormatted = formatSeverity(damageAnalysisResult.getSeverity());
                String analysisText = "Damage Detected: " + severityFormatted + " ("
                        + String.format(Locale.US, "%.2f", damageAnalysisResult.getConfidence() * 100) + "%)";
                uploadStatusText.setText(analysisText);
                uploadStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));

                // Show preview
                ivPreview.setImageBitmap(bitmap);
                ivPreview.setVisibility(View.VISIBLE);
                uploadPrompt.setVisibility(View.GONE);
            } else {
                uploadStatusText.setText("1 Media Item Selected. Ready to Upload.");
                uploadStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));

                // Show preview
                ivPreview.setImageBitmap(bitmap);
                ivPreview.setVisibility(View.VISIBLE);
                uploadPrompt.setVisibility(View.GONE);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting bitmap from URI", e);
            Toast.makeText(this, "Failed to load image for analysis.", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(this, "Media selected: " + selectedMediaUri.getLastPathSegment(), Toast.LENGTH_LONG).show();
    }

    // ------------------------------------
    // --- Form Handling Methods ---
    // ------------------------------------

    private void setupCategoryDropdown() {
        String[] categories = getResources().getStringArray(R.array.damage_categories_array);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories);

        autoCompleteCategory.setAdapter(adapter);
    }

    private void submitReport() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first!", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedCategory = autoCompleteCategory.getText().toString().trim();
        String issueType = editTextIssueType.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();

        if (selectedCategory.isEmpty() || issueType.isEmpty() || location.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSubmitReport.setEnabled(false);
        buttonSubmitReport.setText("Uploading...");

        if (selectedMediaUri != null) {
            StorageReference fileRef = storageRef.child("reports/" + System.currentTimeMillis() + ".jpg");

            fileRef.putFile(selectedMediaUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            ArrayList<String> urls = new ArrayList<>();
                            urls.add(uri.toString());
                            saveDataToFirestore(selectedCategory, issueType, location, description, urls);
                        });
                    })
                    .addOnFailureListener(e -> {
                        buttonSubmitReport.setEnabled(true);
                        buttonSubmitReport.setText("Submit Report");
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveDataToFirestore(selectedCategory, issueType, location, description, new ArrayList<>());
        }
    }

    private void saveDataToFirestore(String cat, String type, String loc, String desc, ArrayList<String> urls) {
        buttonSubmitReport.setText("Saving Data...");

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            buttonSubmitReport.setEnabled(true);
            buttonSubmitReport.setText("Submit Report");
            Toast.makeText(this, "Error: You must be logged in to submit a report.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = user.getUid();

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("userId", currentUserId);
        reportData.put("damage_Category", cat);
        reportData.put("specific_Issue_Type", type);
        reportData.put("location", loc);
        reportData.put("description", desc);
        reportData.put("photoVideoUrls", urls);
        reportData.put("timestamp", FieldValue.serverTimestamp());
        reportData.put("status", "New"); // Add initial status

        if (latestLocation != null) {
            reportData.put("latitude", latestLocation.getLatitude());
            reportData.put("longitude", latestLocation.getLongitude());
        }

        if (damageAnalysisResult != null) {
            String severityStr = formatSeverity(damageAnalysisResult.getSeverity());
            reportData.put("damage_severity", severityStr);
            reportData.put("damage_Level", severityStr); // For compatibility with ReportDetailActivity
            reportData.put("damage_confidence", damageAnalysisResult.getConfidence());
        }

        db.collection("reports")
                .add(reportData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(UserMainActivity.this, "Report Submitted!", Toast.LENGTH_LONG).show();

                    // Create local notification
                    try {
                        NotificationRepository repository = new NotificationRepository(getApplication());
                        AppNotification notif = new AppNotification();
                        notif.title = "Report Submitted";
                        notif.message = "Report successfully submitted for: " + cat;
                        notif.type = "report.received";
                        notif.reportId = documentReference.getId();
                        notif.timestamp = System.currentTimeMillis();
                        notif.isRead = false;
                        repository.insert(notif);
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating local notification", e);
                    }

                    // Optional: Reset form or navigate elsewhere
                    // finish();

                    // Reset UI
                    editTextDescription.setText("");
                    editTextIssueType.setText("");
                    autoCompleteCategory.setText("");
                    uploadStatusText.setText("Supporting Photo/Video");
                    uploadStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                    ivPreview.setVisibility(View.GONE);
                    uploadPrompt.setVisibility(View.VISIBLE);
                    selectedMediaUri = null;
                    damageAnalysisResult = null;

                    buttonSubmitReport.setEnabled(true);
                    buttonSubmitReport.setText("Submit Report");
                })
                .addOnFailureListener(e -> {
                    buttonSubmitReport.setEnabled(true);
                    buttonSubmitReport.setText("Submit Report");
                    Toast.makeText(UserMainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String formatSeverity(DamageAnalysisResult.Severity severity) {
        if (severity == null)
            return "Minor";
        switch (severity) {
            case MAJOR:
                return "Major";
            case MODERATE:
                return "Moderate";
            case MINOR:
                return "Minor";
            default:
                return "Minor";
        }
    }
}
