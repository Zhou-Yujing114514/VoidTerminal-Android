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
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.User;
import com.example.chatapp.websocket.WebSocketManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {
    private List<ChatRoom> rooms;
    private OnItemClickListener listener;
    private String serverBase;

    public interface OnItemClickListener {
        void onItemClick(ChatRoom room);
    }

    public ChatListAdapter(List<ChatRoom> rooms, String serverBase, OnItemClickListener listener) {
        this.rooms = rooms;
        this.serverBase = serverBase;
        this.listener = listener;
    }

    private String getAvatar(String roomId, boolean isGroup) {
        if ("global".equals(roomId)) return "";
        if (isGroup) {
            for (com.example.chatapp.model.Group g : WebSocketManager.getInstance().groups) {
                if (g.id.equals(roomId)) return g.avatar != null ? g.avatar : "";
            }
            return "";
        }
        for (User u : WebSocketManager.getInstance().friends) {
            if (u.id.equals(roomId)) return u.avatar != null ? u.avatar : "";
        }
        return "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatRoom room = rooms.get(position);
        holder.tvName.setText(room.name);
        holder.tvLastMsg.setText(room.lastMsg != null ? room.lastMsg : "暂无消息");
        if (room.lastTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(room.lastTime)));
        } else {
            holder.tvTime.setText("");
        }
        String avatar = getAvatar(room.id, room.isGroup);
        if (avatar != null && !avatar.isEmpty()) {
            String url = avatar.startsWith("/") ? serverBase + avatar : avatar;
            Glide.with(holder.ivAvatar.getContext()).load(url).circleCrop().placeholder(R.drawable.bg_avatar).error(R.drawable.bg_avatar).into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(0);
            holder.ivAvatar.setBackgroundResource(R.drawable.bg_avatar);
        }
        holder.itemView.setOnClickListener(v -> listener.onItemClick(room));

        // 在线状态（仅私聊好友）
        boolean isOnline = !"global".equals(room.id) && !room.isGroup
                && WebSocketManager.getInstance().isUserOnline(room.id);
        holder.vOnline.setVisibility(isOnline ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMsg, tvTime;
        ImageView ivAvatar;
        View vOnline;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLastMsg = itemView.findViewById(R.id.tv_last_msg);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            vOnline = itemView.findViewById(R.id.v_online);
        }
    }
}
