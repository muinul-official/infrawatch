package com.example.notification_system.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notification_system.R;
import com.example.notification_system.data.AppNotification;
import com.example.notification_system.utils.TimeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationAdapter extends ListAdapter<AppNotification, NotificationAdapter.NotificationViewHolder> {

    private final OnNotificationClickListener listener;
    private final Set<Long> selectedIds = new HashSet<>();

    public NotificationAdapter(OnNotificationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<AppNotification> DIFF_CALLBACK = new DiffUtil.ItemCallback<AppNotification>() {
        @Override
        public boolean areItemsTheSame(@NonNull AppNotification oldItem, @NonNull AppNotification newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull AppNotification oldItem, @NonNull AppNotification newItem) {
            return oldItem.title.equals(newItem.title) &&
                    oldItem.message.equals(newItem.message) &&
                    oldItem.isRead == newItem.isRead;
        }
    };

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification currentNotification = getItem(position);
        holder.bind(currentNotification, listener, selectedIds.contains(currentNotification.id));
    }

    public List<Long> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView messageTextView;
        private final TextView timestampTextView;
        private final View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.notification_title);
            messageTextView = itemView.findViewById(R.id.notification_message);
            timestampTextView = itemView.findViewById(R.id.notification_timestamp);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }

        public void bind(final AppNotification notification, final OnNotificationClickListener listener, boolean isSelected) {
            titleTextView.setText(notification.title);
            messageTextView.setText(notification.message);
            timestampTextView.setText(TimeUtils.formatTimestamp(notification.timestamp));
            unreadIndicator.setVisibility(notification.isRead ? View.GONE : View.VISIBLE);
            itemView.setActivated(isSelected);

            itemView.setOnClickListener(v -> {
                if (!selectedIds.isEmpty()) {
                    toggleSelection(notification.id);
                } else {
                    listener.onNotificationClick(notification);
                }
            });

            itemView.setOnLongClickListener(v -> {
                toggleSelection(notification.id);
                return true;
            });
        }

        private void toggleSelection(long id) {
            if (selectedIds.contains(id)) {
                selectedIds.remove(id);
            } else {
                selectedIds.add(id);
            }
            notifyItemChanged(getAdapterPosition());
            listener.onSelectionChanged();
        }
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
        void onSelectionChanged();
    }
}
