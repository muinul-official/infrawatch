package com.example.assignmentreporting;
// IMPORTANT: Make sure this package name matches your project exactly (e.g., com.example.assignment)
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import android.location.Location;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ReportDamageApp";
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
    private CardView uploadCard;
    private TextView uploadStatusText;

    // Location services
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isLocationRequested = false;

    // Media tracking
    private Uri selectedMediaUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance("infrawatch");
        // Match your specific database name "infrawatch"

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        // Initialize FusedLocationProviderClient for location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 1. Initialize UI components
        autoCompleteCategory = findViewById(R.id.autoCompleteCategory);
        editTextIssueType = findViewById(R.id.editTextIssueType);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextDescription = findViewById(R.id.editTextDescription);
        buttonSubmitReport = findViewById(R.id.buttonSubmitReport);
        uploadCard = findViewById(R.id.uploadCard);
        uploadStatusText = findViewById(R.id.uploadStatusText);

        // 2. Setup Dropdown
        setupCategoryDropdown();

        // FIX: Force the dropdown to show on click
        autoCompleteCategory.setOnClickListener(v -> autoCompleteCategory.showDropDown());

        // 3. Setup Click Listeners
        buttonSubmitReport.setOnClickListener(v -> submitReport());

        // NEW: Set click listener for the upload card to open media chooser
        uploadCard.setOnClickListener(v -> openMediaChooser());

        // 4. Request Permissions and Location on start
        checkPermissionsAndGetLocation();
    }

    // ------------------------------------
    // --- Location Handling Methods ---
    // ------------------------------------

    /**
     * Checks if necessary permissions (Location and Camera) are granted,
     * otherwise requests them.
     */
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
            // Permissions are granted, start location updates
            startLocationUpdates();
        } else {
            // Request missing permissions
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Handles the result of the permission request dialog.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean locationGranted = false;
            boolean cameraGranted = false;

            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true;
                }
                if (permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    cameraGranted = true;
                }
            }

            if (locationGranted) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied. Please enter location manually.", Toast.LENGTH_LONG).show();
            }

            if (!cameraGranted) {
                Toast.makeText(this, "Camera permission denied. Cannot upload photos/videos.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Initiates a request for a single, high-accuracy location update.
     */
    // Suppressing 'MissingPermission' as permission check is done in checkPermissionsAndGetLocation()
    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {
        if (isLocationRequested) return; // Prevent multiple requests

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000)
                .setNumUpdates(1); // Only need one update

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

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */);
        isLocationRequested = true;
        Toast.makeText(this, "Attempting to auto-capture location...", Toast.LENGTH_SHORT).show();
    }

    /**
     * Converts GPS coordinates into a readable street address using Geocoder.
     */
    private void reverseGeocodeLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Construct a readable address string
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
                // Fallback to coordinates if geocoding fails
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

    /**
     * Opens the system's media chooser (Gallery/Files) for the user to select
     * one image or video.
     */
    private void openMediaChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // Set type to accept both image and video files
        intent.setType("image/* video/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});

        Intent chooserIntent = Intent.createChooser(intent, "Select Photo or Video");

        startActivityForResult(chooserIntent, REQUEST_MEDIA_PICK);
    }

    /**
     * Handles the result returned after the user selects media from the gallery.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MEDIA_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedMediaUri = data.getData();

            // Update the UI status text to show successful selection
            uploadStatusText.setText("1 Media Item Selected. Ready to Upload.");
            uploadStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            Toast.makeText(this, "Media selected: " + selectedMediaUri.getLastPathSegment(), Toast.LENGTH_LONG).show();
        } else if (requestCode == REQUEST_MEDIA_PICK) {
            // User cancelled selection
            Toast.makeText(this, "No media selected.", Toast.LENGTH_SHORT).show();
            uploadStatusText.setText("Supporting Photo/Video");
            // Note: R.color.colorPrimary may not exist; using a default dark color as fallback
            uploadStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }


    // ------------------------------------
    // --- Form Handling Methods ---
    // ------------------------------------

    /**
     * Loads the string array defined in strings.xml into the AutoCompleteTextView.
     */
    private void setupCategoryDropdown() {
        String[] categories = getResources().getStringArray(R.array.damage_categories_array);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );

        autoCompleteCategory.setAdapter(adapter);
    }

    /**
     * Handles form data retrieval, validation, and submission logic.
     */
    private void submitReport() {
        String selectedCategory = autoCompleteCategory.getText().toString().trim();
        String issueType = editTextIssueType.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();

        if (selectedCategory.isEmpty() || issueType.isEmpty() || location.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button and change text
        buttonSubmitReport.setEnabled(false);
        buttonSubmitReport.setText("Uploading...");

        if (selectedMediaUri != null) {
            // 1. Upload photo to Firebase Storage
            StorageReference fileRef = storageRef.child("reports/" + System.currentTimeMillis() + ".jpg");

            fileRef.putFile(selectedMediaUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // 2. Get the URL of the uploaded photo
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            ArrayList<String> urls = new ArrayList<>();
                            urls.add(uri.toString());
                            // 3. Save to Firestore with the image URL
                            saveDataToFirestore(selectedCategory, issueType, location, description, urls);
                        });
                    })
                    .addOnFailureListener(e -> {
                        buttonSubmitReport.setEnabled(true);
                        buttonSubmitReport.setText("Submit Report");
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // No photo selected, save with an empty list
            saveDataToFirestore(selectedCategory, issueType, location, description, new ArrayList<>());
        }
    }

    // Helper method to keep the code clean
    private void saveDataToFirestore(String cat, String type, String loc, String desc, ArrayList<String> urls) {
        buttonSubmitReport.setText("Saving Data...");

        String currentUserId = "CyRpYE4HAqcx0GyTZqr7TM8MeJ32"; // Retained as requested

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("userId", currentUserId);
        reportData.put("damage_Category", cat);
        reportData.put("specific_Issue_Type", type);
        reportData.put("location", loc);
        reportData.put("description", desc);
        reportData.put("photoVideoUrls", urls); // This now contains the link from Storage
        reportData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("reports")
                .add(reportData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainActivity.this, "Report Submitted!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    buttonSubmitReport.setEnabled(true);
                    buttonSubmitReport.setText("Submit Report");
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}