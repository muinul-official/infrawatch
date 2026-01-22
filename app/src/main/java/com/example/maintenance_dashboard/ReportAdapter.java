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

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private Context context;
    private List<Report> reportList;

    public ReportAdapter(Context context, List<Report> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);
        if (report != null) {
            // Use document ID as report ID for display (truncated)
            String displayId = report.getDocumentId() != null ? 
                    report.getDocumentId().substring(0, Math.min(8, report.getDocumentId().length())) : "N/A";
            holder.tvReportId.setText(displayId);
            
            // Display damage category
            holder.tvCategory.setText(truncateText(report.getDamageCategory(), 12));
            
            // Display location
            holder.tvLocation.setText(truncateText(report.getLocation(), 12));
            
            // Display status
            String status = report.getStatus() != null ? report.getStatus() : "N/A";
            holder.tvStatus.setText(status);
            
            // Set status color
            int statusColor = getStatusColor(status);
            holder.tvStatus.setTextColor(statusColor);

            // Handle click on details icon
            holder.ivDetails.setOnClickListener(v -> {
                Intent intent = new Intent(context, ReportDetailActivity.class);
                intent.putExtra(ReportDetailActivity.EXTRA_DOCUMENT_ID, report.getDocumentId());
                context.startActivity(intent);
            });

            // Also allow clicking on the entire row
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ReportDetailActivity.class);
                intent.putExtra(ReportDetailActivity.EXTRA_DOCUMENT_ID, report.getDocumentId());
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    private int getStatusColor(String status) {
        if (status == null) return Color.GRAY;
        
        switch (status) {
            case Report.STATUS_NEW:
            case Report.STATUS_PENDING:
                return Color.parseColor("#2196F3"); // Blue
            case Report.STATUS_IN_PROGRESS:
                return Color.parseColor("#FF9800"); // Orange
            case Report.STATUS_COMPLETED:
                return Color.parseColor("#4CAF50"); // Green
            case Report.STATUS_CLOSED:
                return Color.parseColor("#9E9E9E"); // Gray
            default:
                return Color.parseColor("#2196F3"); // Default blue for unknown status
        }
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportId, tvCategory, tvLocation, tvStatus;
        ImageView ivDetails;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportId = itemView.findViewById(R.id.tvReportId);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivDetails = itemView.findViewById(R.id.ivDetails);
        }
    }
}
