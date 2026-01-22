package com.example.maintenance_dashboard.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class NotificationRepository {

    private NotificationDao notificationDao;
    private LiveData<List<AppNotification>> allNotifications;

    public NotificationRepository(Application application) {
        NotificationDatabase db = NotificationDatabase.getDatabase(application);
        notificationDao = db.notificationDao();
        allNotifications = notificationDao.getAllNotificationsSortedByNewest();
    }

    public LiveData<List<AppNotification>> getAllNotifications() {
        return allNotifications;
    }

    public void insert(AppNotification notification) {
        NotificationDatabase.databaseWriteExecutor.execute(() -> {
            notificationDao.insert(notification);
        });
    }

    public void markAsRead(long id) {
        NotificationDatabase.databaseWriteExecutor.execute(() -> {
            notificationDao.markAsRead(id);
        });
    }

    public void deleteByIds(List<Long> ids) {
        NotificationDatabase.databaseWriteExecutor.execute(() -> {
            notificationDao.deleteByIds(ids);
        });
    }

    public void deleteRead() {
        NotificationDatabase.databaseWriteExecutor.execute(() -> {
            notificationDao.deleteRead();
        });
    }

    public void deleteAll() {
        NotificationDatabase.databaseWriteExecutor.execute(() -> {
            notificationDao.deleteAll();
        });
    }
}
