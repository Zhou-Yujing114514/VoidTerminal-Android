package com.example.chatapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.model.Group;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {
    private List<Group> groups;
    private String serverBase;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Group group);
    }

    public GroupAdapter(List<Group> groups, String serverBase, OnItemClickListener listener) {
        this.groups = groups;
        this.serverBase = serverBase;
        this.listener = listener;
    }

    public void updateData(List<Group> groups) {
        this.groups = groups;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group group = groups.get(position);
        holder.tvName.setText(group.name);
        holder.tvStatus.setText(group.memberCount + "人");

        if (group.avatar != null && !group.avatar.isEmpty()) {
            String url = group.avatar.startsWith("/") ? serverBase + group.avatar : group.avatar;
            Glide.with(holder.ivAvatar.getContext()).load(url).circleCrop().into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(0);
            holder.ivAvatar.setBackgroundResource(R.drawable.bg_avatar);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(group));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;
        TextView tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
