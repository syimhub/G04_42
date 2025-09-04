package com.example.feedmatepetfeedersystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserActionListener listener;

    // Listener for edit & delete actions
    public interface OnUserActionListener {
        void onEdit(User user);
        void onDelete(User user);
    }

    public UserAdapter(List<User> userList, OnUserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        // Show fallback text if values are null or empty
        String displayName = (user.getFullName() != null && !user.getFullName().trim().isEmpty())
                ? user.getFullName()
                : "Unknown User";

        String displayEmail = (user.getEmail() != null && !user.getEmail().trim().isEmpty())
                ? user.getEmail()
                : "No Email";

        holder.tvName.setText(displayName);
        holder.tvEmail.setText(displayEmail);

        // If the user is an admin → hide edit & delete buttons
        if ("admin".equalsIgnoreCase(user.getRole())) {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(user);
            });

            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(user);
            });
        }
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        ImageView btnEdit, btnDelete;

        UserViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
