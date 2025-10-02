package com.example.feedmatepetfeedersystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FeedingTimeAdapter extends RecyclerView.Adapter<FeedingTimeAdapter.ViewHolder> {

    private List<String> feedingTimes;

    public FeedingTimeAdapter(List<String> feedingTimes) {
        this.feedingTimes = feedingTimes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvTime.setText(feedingTimes.get(position));
    }

    @Override
    public int getItemCount() {
        return feedingTimes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(android.R.id.text1);
        }
    }
}

