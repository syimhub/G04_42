package com.example.feedmatepetfeedersystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<History> historyList;

    public HistoryAdapter(List<History> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        History history = historyList.get(position);
        holder.tvHistoryTime.setText("📅 " + history.timestamp);
        holder.tvFoodLevel.setText("🍚 Food Level: " + history.foodLevel);
        holder.tvFoodWeight.setText("⚖️ Food Weight: " + history.foodWeight + " g");
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryTime, tvFoodLevel, tvFoodWeight;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryTime = itemView.findViewById(R.id.tvHistoryTime);
            tvFoodLevel = itemView.findViewById(R.id.tvFoodLevel);
            tvFoodWeight = itemView.findViewById(R.id.tvFoodWeight);
        }
    }
}
