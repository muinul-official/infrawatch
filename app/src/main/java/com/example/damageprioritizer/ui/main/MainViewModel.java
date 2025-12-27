package com.example.damageprioritizer.ui.main;

import android.graphics.Bitmap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.damageprioritizer.ai.DamageAnalyzer;
import com.example.damageprioritizer.data.DamageAnalysisResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<DamageAnalysisResult> _analysisResult = new MutableLiveData<>();
    public LiveData<DamageAnalysisResult> analysisResult = _analysisResult;

    private final MutableLiveData<Boolean> _isAnalyzing = new MutableLiveData<>();
    public LiveData<Boolean> isAnalyzing = _isAnalyzing;

    private final DamageAnalyzer damageAnalyzer;
    private final ExecutorService executorService;

    public MainViewModel(DamageAnalyzer damageAnalyzer) {
        this.damageAnalyzer = damageAnalyzer;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void analyzeImage(Bitmap bitmap) {
        _isAnalyzing.postValue(true);
        executorService.execute(() -> {
            DamageAnalysisResult result = damageAnalyzer.analyze(bitmap);
            _analysisResult.postValue(result);
            _isAnalyzing.postValue(false);
        });
    }
}
