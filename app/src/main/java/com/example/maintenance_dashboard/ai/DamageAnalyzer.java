package com.example.maintenance_dashboard.ai; // Changed Package

import android.graphics.Bitmap;
import com.example.maintenance_dashboard.data.DamageAnalysisResult; // Changed Import

public interface DamageAnalyzer {
    DamageAnalysisResult analyze(Bitmap bitmap);
}
