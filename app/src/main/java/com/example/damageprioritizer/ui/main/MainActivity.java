package com.example.damageprioritizer.ui.main;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.damageprioritizer.R;
import com.example.damageprioritizer.ai.TFLiteDamageAnalyzer;
import com.example.damageprioritizer.ui.result.ResultActivity;
import com.example.damageprioritizer.utils.BitmapUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MainViewModel viewModel;
    private Uri imageUri;

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), result -> {
                if (result) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        if (viewModel != null) {
                            viewModel.analyzeImage(BitmapUtils.resizeBitmap(bitmap, 640, 480));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading captured image", e);
                        Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    this.imageUri = uri; // Set the imageUri for gallery selections
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                         if (viewModel != null) {
                            viewModel.analyzeImage(BitmapUtils.resizeBitmap(bitmap, 640, 480));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading selected image", e);
                        Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button capturePhotoButton = findViewById(R.id.btn_capture_photo);
        Button selectGalleryButton = findViewById(R.id.btn_select_gallery);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        try {
            // This can fail with Errors (like OutOfMemoryError) or Exceptions.
            TFLiteDamageAnalyzer analyzer = new TFLiteDamageAnalyzer(this);
            viewModel = new ViewModelProvider(this, new MainViewModelFactory(analyzer)).get(MainViewModel.class);

            // Set up observers and listeners only if ViewModel is successfully created.
            capturePhotoButton.setOnClickListener(v -> checkCameraPermissionAndLaunch());
            selectGalleryButton.setOnClickListener(v -> selectImageLauncher.launch("image/*"));

            viewModel.isAnalyzing.observe(this, isAnalyzing -> {
                progressBar.setVisibility(isAnalyzing ? View.VISIBLE : View.GONE);
                capturePhotoButton.setEnabled(!isAnalyzing);
                selectGalleryButton.setEnabled(!isAnalyzing);
            });

            viewModel.analysisResult.observe(this, result -> {
                if (result != null) {
                    Intent intent = new Intent(this, ResultActivity.class);
                    intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, imageUri);
                    intent.putExtra(ResultActivity.EXTRA_SEVERITY, result.getSeverity().name());
                    intent.putExtra(ResultActivity.EXTRA_PRIORITY, result.getPriority().name());
                    intent.putExtra(ResultActivity.EXTRA_EXPLANATION, result.getExplanation());
                    startActivity(intent);
                }
            });

        } catch (Throwable t) { // Catch Throwable to handle Errors as well as Exceptions.
            Log.e(TAG, "Failed to initialize TFLiteDamageAnalyzer", t);
            Toast.makeText(this, "Failed to load AI model. Analysis is disabled.", Toast.LENGTH_LONG).show();
            // Disable the buttons if the model can't be loaded.
            capturePhotoButton.setEnabled(false);
            selectGalleryButton.setEnabled(false);
        }
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (imageUri != null) {
            takePictureLauncher.launch(imageUri);
        } else {
            Toast.makeText(this, "Failed to create image file.", Toast.LENGTH_SHORT).show();
        }
    }
}