package com.example.maintenance_dashboard.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    LiveData<List<AppNotification>> getAllNotificationsSortedByNewest();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(AppNotification notif);

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    void markAsRead(long id);

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    void deleteByIds(List<Long> ids);

    @Query("DELETE FROM notifications WHERE isRead = 1")
    void deleteRead();

    @Query("DELETE FROM notifications")
    void deleteAll();
}
