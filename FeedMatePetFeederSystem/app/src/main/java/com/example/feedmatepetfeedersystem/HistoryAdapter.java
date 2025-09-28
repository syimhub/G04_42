package com.example.feedmatepetfeedersystem;

import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATE = 0;
    private static final int TYPE_ENTRY = 1;

    private final List<Object> historyList;

    public HistoryAdapter(List<Object> historyList) {
        this.historyList = historyList;
    }

    @Override
    public int getItemViewType(int position) {
        if (historyList.get(position) instanceof String) {
            return TYPE_DATE;
        } else {
            return TYPE_ENTRY;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_DATE) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_date, parent, false);
            return new DateViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new HistoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateViewHolder) {
            String date = (String) historyList.get(position);
            DateViewHolder dateHolder = (DateViewHolder) holder;

            // Format today’s date differently
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().getTime());
            if (date.equals(today)) {
                dateHolder.txtDate.setText("Today (" + date + ")");
                dateHolder.txtDate.setBackgroundColor(Color.parseColor("#FF9800")); // orange
            } else {
                dateHolder.txtDate.setText(date);
                dateHolder.txtDate.setBackgroundColor(Color.parseColor("#3d3e40")); // default gray
            }

        } else if (holder instanceof HistoryViewHolder) {
            History entry = (History) historyList.get(position);
            HistoryViewHolder entryHolder = (HistoryViewHolder) holder;

            // Source + icon
            if (entry.source != null) {
                entryHolder.txtOperation.setText(entry.source);
                if (entry.source.equalsIgnoreCase("manual")) {
                    entryHolder.imgSource.setImageResource(R.drawable.ic_manual); // 🍽 custom icon
                } else if (entry.source.equalsIgnoreCase("scheduled")) {
                    entryHolder.imgSource.setImageResource(R.drawable.ic_scheduled); // 🕒 custom icon
                } else {
                    entryHolder.imgSource.setImageResource(R.drawable.ic_system); // ⚙️ custom icon
                }
            } else {
                entryHolder.txtOperation.setText("Unknown");
                entryHolder.imgSource.setImageResource(R.drawable.ic_system);
            }

            entryHolder.txtFoodLevel.setText("Food Level: " + entry.level + "%");
            entryHolder.txtFoodWeight.setText("Food Weight: " + entry.weight + " g");

            // Format time to 12-hour
            entryHolder.txtTimestamp.setText("Time: " + formatTime(entry.time));
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    // Convert HH:mm → h:mm a
    private String formatTime(String time) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(time);
            if (date != null) {
                return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time; // fallback
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txtDateHeader);
        }
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtOperation, txtFoodLevel, txtFoodWeight, txtTimestamp;
        ImageView imgSource;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtOperation = itemView.findViewById(R.id.txtOperation);
            txtFoodLevel = itemView.findViewById(R.id.txtFoodLevel);
            txtFoodWeight = itemView.findViewById(R.id.txtFoodWeight);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
            imgSource = itemView.findViewById(R.id.imgSource);
        }
    }
}
