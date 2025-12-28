package com.example.infrawatchmapdev;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentActivitiesAdapter extends RecyclerView.Adapter<RecentActivitiesAdapter.ViewHolder> {

    private List<RepairReport> reports;

    public RecentActivitiesAdapter(List<RepairReport> reports) {
        this.reports = reports;
    }

    public void updateReports(List<RepairReport> newReports) {
        this.reports = newReports;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RepairReport report = reports.get(position);

        // Title: category
        holder.title.setText(
                report.damage_Category != null ? report.damage_Category : "Unknown Category"
        );

        // Subtitle: location
        holder.location.setText(
                report.location != null ? report.location : "Unknown Location"
        );
    }

    @Override
    public int getItemCount() {
        return reports == null ? 0 : reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, location;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_recent_title);
            location = itemView.findViewById(R.id.tv_recent_location);
        }
    }
}
