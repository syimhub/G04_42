package com.example.feedmatepetfeedersystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SetScheduleUserAdapter extends RecyclerView.Adapter<SetScheduleUserAdapter.ScheduleViewHolder> {

    private List<User> userList;
    private OnUserSelectListener selectListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    // Callback when user is selected
    public interface OnUserSelectListener {
        void onSelect(User user, int position);
    }

    public SetScheduleUserAdapter(List<User> userList, OnUserSelectListener selectListener) {
        this.userList = userList;
        this.selectListener = selectListener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        User user = userList.get(position);

        String displayName = (user.getFullName() != null && !user.getFullName().trim().isEmpty())
                ? user.getFullName() : "Unknown User";

        String displayEmail = (user.getEmail() != null && !user.getEmail().trim().isEmpty())
                ? user.getEmail() : "No Email";

        holder.tvName.setText(displayName);
        holder.tvEmail.setText(displayEmail);

        // Highlight if selected
        holder.itemView.setBackgroundColor(
                position == selectedPosition ? 0xFFE0F7FA : 0xFFFFFFFF
        );

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(oldPos);
            notifyItemChanged(position);

            if (selectListener != null) selectListener.onSelect(user, position);
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;

        ScheduleViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
        }
    }
}
