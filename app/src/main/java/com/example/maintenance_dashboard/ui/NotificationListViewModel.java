package com.example.maintenance_dashboard.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.maintenance_dashboard.data.AppNotification;
import com.example.maintenance_dashboard.data.NotificationRepository;

import java.util.List;

public class NotificationListViewModel extends AndroidViewModel {

    private NotificationRepository repository;
    private LiveData<List<AppNotification>> allNotifications;

    public NotificationListViewModel(Application application) {
        super(application);
        repository = new NotificationRepository(application);
        allNotifications = repository.getAllNotifications();
    }

    public LiveData<List<AppNotification>> getNotifications() {
        return allNotifications;
    }

    public void markAsRead(long id) {
        repository.markAsRead(id);
    }

    public void deleteNotificationsByIds(List<Long> ids) {
        repository.deleteByIds(ids);
    }

    public void clearSeenNotifications() {
        repository.deleteRead();
    }

    public void clearAllNotifications() {
        repository.deleteAll();
    }
}
