package com.example.damageprioritizer.ui.result;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.damageprioritizer.R;
import com.example.damageprioritizer.data.DamageAnalysisResult;
import com.google.android.material.chip.Chip;

import java.io.IOException;

public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_SEVERITY = "extra_severity";
    public static final String EXTRA_PRIORITY = "extra_priority";
    public static final String EXTRA_EXPLANATION = "extra_explanation";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ImageView imagePreview = findViewById(R.id.image_preview);
        Chip severityChip = findViewById(R.id.chip_severity);
        TextView priorityTextView = findViewById(R.id.tv_priority);
        TextView explanationTextView = findViewById(R.id.tv_explanation);
        Button analyzeAnotherButton = findViewById(R.id.btn_analyze_another);

        Uri imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        String severity = getIntent().getStringExtra(EXTRA_SEVERITY);
        String priority = getIntent().getStringExtra(EXTRA_PRIORITY);
        String explanation = getIntent().getStringExtra(EXTRA_EXPLANATION);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            imagePreview.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        severityChip.setText(severity);
        priorityTextView.setText("Priority: " + priority);
        explanationTextView.setText(explanation);

        analyzeAnotherButton.setOnClickListener(v -> finish());
    }
}
