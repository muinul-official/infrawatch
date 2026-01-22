package com.example.maintenance_dashboard.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    public static String formatTimestamp(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            if (days < 7) {
                return days + "d ago";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "just now";
        }
    }
}
