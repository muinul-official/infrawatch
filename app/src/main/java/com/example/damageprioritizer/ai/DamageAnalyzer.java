package com.example.damageprioritizer.ai;

import android.graphics.Bitmap;

import com.example.damageprioritizer.data.DamageAnalysisResult;

public interface DamageAnalyzer {
    DamageAnalysisResult analyze(Bitmap bitmap);
}
