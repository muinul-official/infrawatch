package com.example.damageprioritizer.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.damageprioritizer.ai.DamageAnalyzer;

public class MainViewModelFactory implements ViewModelProvider.Factory {

    private final DamageAnalyzer damageAnalyzer;

    public MainViewModelFactory(DamageAnalyzer damageAnalyzer) {
        this.damageAnalyzer = damageAnalyzer;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(damageAnalyzer);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
