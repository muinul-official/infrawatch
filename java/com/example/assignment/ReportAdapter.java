package com.example.assignment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip; // <-- Must import the Chip class
import java.util.List;

/**
 * Adapter for the RecyclerView, now using the specific IDs from item_report.xml.
 */
public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private final List<ReportModel> reportList;
    private final OnItemClickListener listener;

    // Defines the interface for handling clicks (Resolves 'OnItemClickListener' symbol error)
    public interface OnItemClickListener {
        void onItemClick(ReportModel report);
    }

    public ReportAdapter(List<ReportModel> reportList, OnItemClickListener listener) {
        this.reportList = reportList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ReportModel currentItem = reportList.get(position);

        // Binding to your specific XML IDs:
        holder.idTextView.setText(currentItem.getId());
        holder.categoryTextView.setText(currentItem.getCategory());
        holder.locationTextView.setText(currentItem.getLocation());

        // Status is set on the Chip component
        holder.statusChip.setText(currentItem.getStatus());

        // You would typically set the Chip background color here based on the status, e.g.:
        // if (currentItem.getStatus().equals("New")) { holder.statusChip.setChipBackgroundColorResource(R.color.blue_light); }

        // Attach the click listener
        holder.itemView.setOnClickListener(v -> listener.onItemClick(currentItem));
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    /**
     * Inner class to hold and manage the views from the 'item_report.xml' layout.
     */
    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        public TextView idTextView;
        public TextView categoryTextView;
        public TextView locationTextView;
        public Chip statusChip; // <-- Changed to Chip

        public ReportViewHolder(View itemView) {
            super(itemView);

            // MAPPING YOUR XML IDs TO JAVA VARIABLES:
            // R.id.text_report_id from item_report.xml
            idTextView = itemView.findViewById(R.id.text_report_id);
            // R.id.text_report_category from item_report.xml
            categoryTextView = itemView.findViewById(R.id.text_report_category);
            // R.id.text_report_location from item_report.xml
            locationTextView = itemView.findViewById(R.id.text_report_location);
            // R.id.chip_report_status from item_report.xml (This resolves the final symbol error)
            statusChip = itemView.findViewById(R.id.chip_report_status);
        }
    }
}
