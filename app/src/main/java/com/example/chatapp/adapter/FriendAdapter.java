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
import com.example.chatapp.model.User;
import com.example.chatapp.websocket.WebSocketManager;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    private List<User> friends;
    private String serverBase;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public FriendAdapter(List<User> friends, String serverBase, OnItemClickListener listener) {
        this.friends = friends;
        this.serverBase = serverBase;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = friends.get(position);
        holder.tvName.setText(user.username);
        boolean isOnline = WebSocketManager.getInstance().isUserOnline(user.id);
        holder.tvStatus.setText(isOnline ? "在线" : "离线");
        holder.vOnline.setVisibility(isOnline ? View.VISIBLE : View.GONE);

        if (user.avatar != null && !user.avatar.isEmpty()) {
            String url = user.avatar.startsWith("/") ? serverBase + user.avatar : user.avatar;
            Glide.with(holder.ivAvatar.getContext()).load(url).circleCrop().into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(0);
            holder.ivAvatar.setBackgroundResource(R.drawable.bg_avatar);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(user));
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvStatus;
        View vOnline;
        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            vOnline = itemView.findViewById(R.id.v_online);
        }
    }
}
