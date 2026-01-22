package com.example.maintenance_dashboard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ReportGridAdapter extends RecyclerView.Adapter<ReportGridAdapter.ViewHolder> {

    private List<Report> reports;
    private Context context;

    public ReportGridAdapter(Context context, List<Report> reports) {
        this.context = context;
        this.reports = reports;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reports.get(position);
        holder.tvCategory.setText(report.getDamageCategory());
        holder.tvLocation.setText(report.getLocation());

        String status = report.getStatus() != null ? report.getStatus() : "New";
        holder.tvStatus.setText(status);

        // Update status badge color
        GradientDrawable background = (GradientDrawable) holder.tvStatus.getBackground();
        int color;
        switch (status) {
            case Report.STATUS_NEW:
            case Report.STATUS_PENDING:
                color = Color.parseColor("#2196F3"); // Blue
                break;
            case Report.STATUS_IN_PROGRESS:
                color = Color.parseColor("#FF9800"); // Orange
                break;
            case Report.STATUS_COMPLETED:
                color = Color.parseColor("#4CAF50"); // Green
                break;
            case Report.STATUS_CLOSED:
                color = Color.parseColor("#9E9E9E"); // Gray
                break;
            default:
                color = Color.parseColor("#2196F3");
        }
        if (background != null) {
            background.setColor(color);
        }

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
        TextView tvCategory, tvLocation, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivReportImage);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
