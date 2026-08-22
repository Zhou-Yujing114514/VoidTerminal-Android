package com.example.chatapp.adapter;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chatapp.ImageViewerActivity;
import com.example.chatapp.R;
import com.example.chatapp.model.Message;
import com.example.chatapp.model.User;
import com.example.chatapp.websocket.WebSocketManager;
import java.util.List;
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<Message> messages;
    private boolean showSenderName;
    private String serverBase;
    private String myAvatar;
    private OnMessageLongPressListener longPressListener;
    public interface OnMessageLongPressListener {
        void onLongPress(Message msg);
    }
    public MessageAdapter(List<Message> messages, boolean showSenderName, String serverBase, String myAvatar, OnMessageLongPressListener listener) {
        this.messages = messages;
        this.showSenderName = showSenderName;
        this.serverBase = serverBase;
        this.myAvatar = myAvatar;
        this.longPressListener = listener;
    }
    @Override
    public int getItemViewType(int position) {
        return 0;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = messages.get(position);
        String myId = holder.itemView.getContext().getSharedPreferences("chatapp_prefs", 0).getString("user_id", "");
        boolean isMe = msg.from != null && msg.from.equals(myId);
        holder.layoutMe.setVisibility(isMe ? View.VISIBLE : View.GONE);
        holder.layoutOther.setVisibility(isMe ? View.GONE : View.VISIBLE);
        if (msg.recalled) {
            if (isMe) {
                holder.tvContentMe.setVisibility(View.GONE);
                holder.ivImageMe.setVisibility(View.GONE);
                holder.layoutQuoteMe.setVisibility(View.GONE);
                holder.tvRecalledMe.setVisibility(View.VISIBLE);
            } else {
                holder.tvContentOther.setVisibility(View.GONE);
                holder.ivImageOther.setVisibility(View.GONE);
                holder.layoutQuoteOther.setVisibility(View.GONE);
                holder.tvRecalledOther.setVisibility(View.VISIBLE);
            }
            return;
        }
        // 发送者名称 - 优先用fromName，其次从好友列表查找
        if (showSenderName && !isMe) {
            String senderName = resolveUserName(msg.from, msg.fromName);
            if (senderName != null && !senderName.isEmpty()) {
                holder.tvSenderName.setText(senderName);
                holder.tvSenderName.setVisibility(View.VISIBLE);
            } else {
                holder.tvSenderName.setVisibility(View.GONE);
            }
        } else {
            holder.tvSenderName.setVisibility(View.GONE);
        }
        // 图片
        String imageUrl = null;
        if (msg.hasImage()) {
            imageUrl = msg.images.get(0);
        } else if (msg.isImageUrl()) {
            imageUrl = msg.content;
        }
        final String fullImageUrl = imageUrl != null ? (imageUrl.startsWith("/") ? serverBase + imageUrl : imageUrl) : null;
        if (isMe) {
            holder.tvRecalledMe.setVisibility(View.GONE);
            // 引用
            if (msg.hasQuote()) {
                holder.layoutQuoteMe.setVisibility(View.VISIBLE);
                holder.tvQuoteFromMe.setText(msg.quoteFromName != null ? msg.quoteFromName : "");
                holder.tvQuoteContentMe.setText(msg.quoteContent != null ? msg.quoteContent : "");
            } else {
                holder.layoutQuoteMe.setVisibility(View.GONE);
            }
            // 图片
            if (imageUrl != null) {
                holder.ivImageMe.setVisibility(View.VISIBLE);
                Glide.with(holder.ivImageMe.getContext()).load(fullImageUrl).into(holder.ivImageMe);
                holder.ivImageMe.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), ImageViewerActivity.class);
                    intent.putExtra("image_url", fullImageUrl);
                    v.getContext().startActivity(intent);
                });
            } else {
                holder.ivImageMe.setVisibility(View.GONE);
                holder.ivImageMe.setOnClickListener(null);
            }
            // 文字（图文同发时也显示）
            if (msg.content != null && !msg.content.isEmpty() && !msg.isImageUrl()) {
                holder.tvContentMe.setText(msg.content);
                holder.tvContentMe.setVisibility(View.VISIBLE);
            } else {
                holder.tvContentMe.setVisibility(View.GONE);
            }
            // 头像
            if (myAvatar != null && !myAvatar.isEmpty()) {
                String url = myAvatar.startsWith("/") ? serverBase + myAvatar : myAvatar;
                Glide.with(holder.ivAvatarMe.getContext()).load(url).circleCrop().into(holder.ivAvatarMe);
            }
        } else {
            holder.tvRecalledOther.setVisibility(View.GONE);
            // 引用
            if (msg.hasQuote()) {
                holder.layoutQuoteOther.setVisibility(View.VISIBLE);
                holder.tvQuoteFromOther.setText(msg.quoteFromName != null ? msg.quoteFromName : "");
                holder.tvQuoteContentOther.setText(msg.quoteContent != null ? msg.quoteContent : "");
            } else {
                holder.layoutQuoteOther.setVisibility(View.GONE);
            }
            // 图片
            if (imageUrl != null) {
                holder.ivImageOther.setVisibility(View.VISIBLE);
                Glide.with(holder.ivImageOther.getContext()).load(fullImageUrl).into(holder.ivImageOther);
                holder.ivImageOther.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), ImageViewerActivity.class);
                    intent.putExtra("image_url", fullImageUrl);
                    v.getContext().startActivity(intent);
                });
            } else {
                holder.ivImageOther.setVisibility(View.GONE);
                holder.ivImageOther.setOnClickListener(null);
            }
            // 文字
            if (msg.content != null && !msg.content.isEmpty() && !msg.isImageUrl()) {
                holder.tvContentOther.setText(msg.content);
                holder.tvContentOther.setVisibility(View.VISIBLE);
            } else {
                holder.tvContentOther.setVisibility(View.GONE);
            }
            // 头像
            if (msg.fromAvatar != null && !msg.fromAvatar.isEmpty()) {
                String url = msg.fromAvatar.startsWith("/") ? serverBase + msg.fromAvatar : msg.fromAvatar;
                Glide.with(holder.ivAvatarOther.getContext()).load(url).circleCrop().into(holder.ivAvatarOther);
            }
        }
        // 长按消息
        holder.itemView.setOnLongClickListener(v -> {
            if (!msg.recalled && longPressListener != null) {
                longPressListener.onLongPress(msg);
                return true;
            }
            return false;
        });
    }
    private String resolveUserName(String uid, String fromName) {
        if (fromName != null && !fromName.isEmpty() && !fromName.startsWith("u_")) {
            return fromName;
        }
        if (uid != null) {
            for (User u : WebSocketManager.getInstance().friends) {
                if (u.id.equals(uid) && u.username != null && !u.username.isEmpty()) {
                    return u.username;
                }
            }
            if (WebSocketManager.getInstance().currentUser != null && uid.equals(WebSocketManager.getInstance().currentUser.id)) {
                return WebSocketManager.getInstance().currentUser.username;
            }
        }
        return fromName != null ? fromName : "";
    }
    @Override
    public int getItemCount() {
        return messages.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        View layoutMe, layoutOther;
        LinearLayout layoutQuoteMe, layoutQuoteOther;
        TextView tvSenderName, tvContentMe, tvContentOther, tvRecalledMe, tvRecalledOther;
        TextView tvQuoteFromMe, tvQuoteContentMe, tvQuoteFromOther, tvQuoteContentOther;
        ImageView ivAvatarMe, ivAvatarOther, ivImageMe, ivImageOther;
        ViewHolder(View itemView) {
            super(itemView);
            layoutMe = itemView.findViewById(R.id.layout_me);
            layoutOther = itemView.findViewById(R.id.layout_other);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvContentMe = itemView.findViewById(R.id.tv_content_me);
            tvContentOther = itemView.findViewById(R.id.tv_content_other);
            tvRecalledMe = itemView.findViewById(R.id.tv_recalled_me);
            tvRecalledOther = itemView.findViewById(R.id.tv_recalled_other);
            ivAvatarMe = itemView.findViewById(R.id.iv_avatar_me);
            ivAvatarOther = itemView.findViewById(R.id.iv_avatar_other);
            ivImageMe = itemView.findViewById(R.id.iv_image_me);
            ivImageOther = itemView.findViewById(R.id.iv_image_other);
            layoutQuoteMe = itemView.findViewById(R.id.layout_quote_me);
            layoutQuoteOther = itemView.findViewById(R.id.layout_quote_other);
            tvQuoteFromMe = itemView.findViewById(R.id.tv_quote_from_me);
            tvQuoteContentMe = itemView.findViewById(R.id.tv_quote_content_me);
            tvQuoteFromOther = itemView.findViewById(R.id.tv_quote_from_other);
            tvQuoteContentOther = itemView.findViewById(R.id.tv_quote_content_other);
        }
    }
}
