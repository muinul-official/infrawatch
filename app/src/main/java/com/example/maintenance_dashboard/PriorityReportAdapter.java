package com.example.maintenance_dashboard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maintenance_dashboard.data.DamageAnalysisResult;

import java.util.List;

public class PriorityReportAdapter extends RecyclerView.Adapter<PriorityReportAdapter.ViewHolder> {

    private List<Report> reports;
    private Context context;

    public PriorityReportAdapter(Context context, List<Report> reports) {
        this.context = context;
        this.reports = reports;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_priority_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reports.get(position);
        holder.tvCategory.setText(report.getDamageCategory());
        holder.tvLocation.setText(report.getLocation());
        holder.tvStatus.setText(report.getStatus());
        holder.tvDate.setText(report.getFormattedDate());

        // Get severity/level for priority badge
        String displayLevel = report.getConsolidatedDamageLevel();

        holder.tvBadge.setText(displayLevel.toUpperCase());

        int badgeColor;
        if (displayLevel.equalsIgnoreCase("MAJOR") || displayLevel.equalsIgnoreCase("HIGH")) {
            badgeColor = context.getResources().getColor(R.color.priority_high);
        } else if (displayLevel.equalsIgnoreCase("MINOR") || displayLevel.equalsIgnoreCase("LOW")) {
            badgeColor = context.getResources().getColor(R.color.priority_low);
        } else {
            badgeColor = context.getResources().getColor(R.color.priority_medium);
        }
        holder.tvBadge.setBackgroundColor(badgeColor);

        // Load image
        String imageUrl = report.getFirstImageUrl();
        if (imageUrl != null) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.color.light_gray)
                    .centerCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.color.light_gray);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReportDetailActivity.class);
            intent.putExtra(ReportDetailActivity.EXTRA_DOCUMENT_ID, report.getDocumentId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvCategory, tvLocation, tvStatus, tvDate, tvBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivPriorityReportImage);
            tvCategory = itemView.findViewById(R.id.tvPriorityCategory);
            tvLocation = itemView.findViewById(R.id.tvPriorityLocation);
            tvStatus = itemView.findViewById(R.id.tvPriorityStatus);
            tvDate = itemView.findViewById(R.id.tvPriorityDate);
            tvBadge = itemView.findViewById(R.id.tvPriorityBadge);
        }
    }
}
