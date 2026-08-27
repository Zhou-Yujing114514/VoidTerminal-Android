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
    private String lastReadMsgId = "";
    private long lastReadTime = 0;
    private OnMessageLongPressListener longPressListener;
    private OnAvatarLongPressListener avatarLongPressListener;
    public interface OnMessageLongPressListener {
        void onLongPress(Message msg);
    }
    public interface OnAvatarLongPressListener {
        void onAvatarLongPress(Message msg);
    }
    public MessageAdapter(List<Message> messages, boolean showSenderName, String serverBase, String myAvatar, OnMessageLongPressListener listener, OnAvatarLongPressListener avatarListener) {
        this.messages = messages;
        this.showSenderName = showSenderName;
        this.serverBase = serverBase;
        this.myAvatar = myAvatar;
        this.longPressListener = listener;
        this.avatarLongPressListener = avatarListener;
    }
    public void updateMyAvatar(String avatar) {
        this.myAvatar = avatar;
        notifyDataSetChanged();
    }
    public void setLastReadMsgId(String msgId) {
        this.lastReadMsgId = msgId;
        // 同时根据消息ID查找时间戳
        if (msgId != null && !msgId.isEmpty()) {
            for (Message m : messages) {
                if (msgId.equals(m.id)) {
                    this.lastReadTime = m.time;
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setLastReadInfo(String msgId, long time) {
        this.lastReadMsgId = msgId;
        this.lastReadTime = time;
        notifyDataSetChanged();
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
        // 发送者名称 - 优先用fromName，其次从好友列表查找，头衔用不同颜色显示
        if (showSenderName && !isMe) {
            String senderName = resolveUserName(holder.itemView.getContext(), msg.from, msg.fromName);
            String senderTitle = resolveUserTitle(msg.from);
            if (senderName != null && !senderName.isEmpty()) {
                if (senderTitle != null && !senderTitle.isEmpty()) {
                    // 用 SpannableString 显示名称（白色）+ 头衔（金色）
                    android.text.SpannableString spannable = new android.text.SpannableString(senderName + "  " + senderTitle);
                    spannable.setSpan(new android.text.style.ForegroundColorSpan(0xFFCCCCCC), 0, senderName.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new android.text.style.ForegroundColorSpan(0xFFFFD700), senderName.length() + 2, spannable.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new android.text.style.AbsoluteSizeSpan(11, true), senderName.length() + 2, spannable.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    holder.tvSenderName.setText(spannable);
                } else {
                    holder.tvSenderName.setText(senderName);
                }
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
                holder.ivImageMe.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                holder.ivImageMe.setAdjustViewBounds(true);
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
            // 检测是否是文件消息（content为JSON包含fileUrl）
            boolean isFileMsg = false;
            String fileUrl = null, fileName = null;
            long fileSize = 0;
            if (msg.hasFile()) {
                isFileMsg = true;
                fileUrl = msg.fileUrl;
                fileName = msg.fileName;
                fileSize = msg.fileSize;
            } else if (msg.content != null && msg.content.startsWith("{") && msg.content.contains("fileUrl")) {
                try {
                    org.json.JSONObject fObj = new org.json.JSONObject(msg.content);
                    isFileMsg = true;
                    fileUrl = fObj.getString("fileUrl");
                    fileName = fObj.optString("fileName", "文件");
                    fileSize = fObj.optLong("fileSize", 0);
                } catch (Exception e) {}
            }
            // 检测音频消息
            boolean isAudioMsg = false;
            String audioUrl = null, audioName = null;
            long audioSize = 0;
            if (msg.hasAudio()) {
                isAudioMsg = true;
                audioUrl = msg.audioUrl;
                audioName = msg.audioName;
                audioSize = msg.audioSize;
            } else if (msg.content != null && msg.content.startsWith("{") && msg.content.contains("audioUrl")) {
                try {
                    org.json.JSONObject aObj = new org.json.JSONObject(msg.content);
                    isAudioMsg = true;
                    audioUrl = aObj.getString("audioUrl");
                    audioName = aObj.optString("audioName", "音频");
                    audioSize = aObj.optLong("audioSize", 0);
                } catch (Exception e) {}
            }
            if (isAudioMsg) {
                holder.tvContentMe.setText("🎵 " + audioName + "\n" + formatFileSize(audioSize) + " (点击播放)");
                holder.tvContentMe.setVisibility(View.VISIBLE);
                final String playUrl = audioUrl;
                final String playName = audioName;
                holder.tvContentMe.setOnClickListener(v -> {
                    try {
                        String fullUrl = playUrl.startsWith("/") ? serverBase + playUrl : playUrl;
                        playAudioInApp(fullUrl, playName, v.getContext());
                    } catch (Exception e) {
                        android.widget.Toast.makeText(v.getContext(), "播放失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (isFileMsg) {
                holder.tvContentMe.setText("📄 " + fileName + "\n" + formatFileSize(fileSize) + " (点击下载)");
                holder.tvContentMe.setVisibility(View.VISIBLE);
                final String dlUrl = fileUrl;
                final String dlName = fileName;
                holder.tvContentMe.setOnClickListener(v -> {
                    try {
                        String fullUrl = dlUrl.startsWith("/") ? serverBase + dlUrl : dlUrl;
                        android.content.Intent intent = new android.content.Intent(v.getContext(), com.example.chatapp.FilePreviewActivity.class);
                        intent.putExtra("file_url", fullUrl);
                        intent.putExtra("file_name", dlName);
                        v.getContext().startActivity(intent);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(v.getContext(), "打开失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (msg.content != null && !msg.content.isEmpty() && !msg.isImageUrl()) {
                holder.tvContentMe.setText(msg.content);
                holder.tvContentMe.setVisibility(View.VISIBLE);
                holder.tvContentMe.setOnClickListener(null);
            } else {
                holder.tvContentMe.setVisibility(View.GONE);
            }
            // 已读/未读状态（仅私聊，仅自己发的消息，非撤回）
            if (isMe && !showSenderName && !msg.recalled && (msg.content != null || msg.hasImage() || msg.hasAudio() || msg.hasFile())) {
                boolean isRead = (lastReadMsgId != null && !lastReadMsgId.isEmpty() && lastReadMsgId.equals(msg.id)) || 
                                  (lastReadTime > 0 && msg.time <= lastReadTime);
                if (isRead) {
                    holder.tvStatusMe.setText("已读");
                    holder.tvStatusMe.setTextColor(0xFF4CAF50);
                } else {
                    holder.tvStatusMe.setText("未读");
                    holder.tvStatusMe.setTextColor(0xFF999999);
                }
                holder.tvStatusMe.setVisibility(View.VISIBLE);
            } else {
                holder.tvStatusMe.setVisibility(View.GONE);
            }
            // 头像
            if (myAvatar != null && !myAvatar.isEmpty()) {
                String url = myAvatar.startsWith("/") ? serverBase + myAvatar : myAvatar;
                Glide.with(holder.ivAvatarMe.getContext()).load(url).circleCrop().placeholder(R.drawable.bg_avatar).error(R.drawable.bg_avatar).into(holder.ivAvatarMe);
            } else {
                holder.ivAvatarMe.setImageResource(R.drawable.bg_avatar);
            }
            holder.ivAvatarMe.setOnLongClickListener(v -> {
                if (avatarLongPressListener != null) avatarLongPressListener.onAvatarLongPress(msg);
                return true;
            });
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
                holder.ivImageOther.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                holder.ivImageOther.setAdjustViewBounds(true);
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
            // 检测是否是文件消息
            boolean isFileMsgO = false;
            String fileUrlO = null, fileNameO = null;
            long fileSizeO = 0;
            if (msg.hasFile()) {
                isFileMsgO = true;
                fileUrlO = msg.fileUrl;
                fileNameO = msg.fileName;
                fileSizeO = msg.fileSize;
            } else if (msg.content != null && msg.content.startsWith("{") && msg.content.contains("fileUrl")) {
                try {
                    org.json.JSONObject fObj = new org.json.JSONObject(msg.content);
                    isFileMsgO = true;
                    fileUrlO = fObj.getString("fileUrl");
                    fileNameO = fObj.optString("fileName", "文件");
                    fileSizeO = fObj.optLong("fileSize", 0);
                } catch (Exception e) {}
            }
            // 检测音频消息
            boolean isAudioMsgO = false;
            String audioUrlO = null, audioNameO = null;
            long audioSizeO = 0;
            if (msg.hasAudio()) {
                isAudioMsgO = true;
                audioUrlO = msg.audioUrl;
                audioNameO = msg.audioName;
                audioSizeO = msg.audioSize;
            } else if (msg.content != null && msg.content.startsWith("{") && msg.content.contains("audioUrl")) {
                try {
                    org.json.JSONObject aObj = new org.json.JSONObject(msg.content);
                    isAudioMsgO = true;
                    audioUrlO = aObj.getString("audioUrl");
                    audioNameO = aObj.optString("audioName", "音频");
                    audioSizeO = aObj.optLong("audioSize", 0);
                } catch (Exception e) {}
            }
            if (isAudioMsgO) {
                holder.tvContentOther.setText("🎵 " + audioNameO + "\n" + formatFileSize(audioSizeO) + " (点击播放)");
                holder.tvContentOther.setVisibility(View.VISIBLE);
                final String playUrl = audioUrlO;
                final String playNameO = audioNameO;
                holder.tvContentOther.setOnClickListener(v -> {
                    try {
                        String fullUrl = playUrl.startsWith("/") ? serverBase + playUrl : playUrl;
                        playAudioInApp(fullUrl, playNameO, v.getContext());
                    } catch (Exception e) {
                        android.widget.Toast.makeText(v.getContext(), "播放失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (isFileMsgO) {
                holder.tvContentOther.setText("📄 " + fileNameO + "\n" + formatFileSize(fileSizeO) + " (点击下载)");
                holder.tvContentOther.setVisibility(View.VISIBLE);
                final String dlUrl = fileUrlO;
                final String dlNameO = fileNameO;
                holder.tvContentOther.setOnClickListener(v -> {
                    try {
                        String fullUrl = dlUrl.startsWith("/") ? serverBase + dlUrl : dlUrl;
                        android.content.Intent intent = new android.content.Intent(v.getContext(), com.example.chatapp.FilePreviewActivity.class);
                        intent.putExtra("file_url", fullUrl);
                        intent.putExtra("file_name", dlNameO);
                        v.getContext().startActivity(intent);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(v.getContext(), "打开失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (msg.content != null && !msg.content.isEmpty() && !msg.isImageUrl()) {
                holder.tvContentOther.setText(msg.content);
                holder.tvContentOther.setVisibility(View.VISIBLE);
                holder.tvContentOther.setOnClickListener(null);
            } else {
                holder.tvContentOther.setVisibility(View.GONE);
            }
            // 头像
            String otherAvatar = msg.fromAvatar;
            if (otherAvatar == null || otherAvatar.isEmpty()) {
                // 从 WebSocketManager 获取用户头像
                com.example.chatapp.model.User u = com.example.chatapp.websocket.WebSocketManager.getInstance().getUserById(msg.from);
                if (u != null && u.avatar != null && !u.avatar.isEmpty()) {
                    otherAvatar = u.avatar;
                }
            }
            if (otherAvatar != null && !otherAvatar.isEmpty()) {
                String url = otherAvatar.startsWith("/") ? serverBase + otherAvatar : otherAvatar;
                Glide.with(holder.ivAvatarOther.getContext()).load(url).circleCrop().placeholder(R.drawable.bg_avatar).error(R.drawable.bg_avatar).into(holder.ivAvatarOther);
            } else {
                holder.ivAvatarOther.setImageResource(R.drawable.bg_avatar);
            }
            holder.ivAvatarOther.setOnLongClickListener(v -> {
                if (avatarLongPressListener != null) avatarLongPressListener.onAvatarLongPress(msg);
                return true;
            });
        }
        // 长按消息（itemView 和内容视图都设置，确保能触发）
        android.view.View.OnLongClickListener longClick = v -> {
            if (!msg.recalled && longPressListener != null) {
                longPressListener.onLongPress(msg);
                return true;
            }
            return false;
        };
        holder.itemView.setOnLongClickListener(longClick);
        if (holder.tvContentMe != null) holder.tvContentMe.setOnLongClickListener(longClick);
        if (holder.tvContentOther != null) holder.tvContentOther.setOnLongClickListener(longClick);
        if (holder.ivImageMe != null) holder.ivImageMe.setOnLongClickListener(longClick);
        if (holder.ivImageOther != null) holder.ivImageOther.setOnLongClickListener(longClick);
    }
    private String resolveUserTitle(String uid) {
        if (uid == null) return "";
        for (User u : WebSocketManager.getInstance().friends) {
            if (u.id.equals(uid) && u.title != null && !u.title.isEmpty()) {
                return u.title;
            }
        }
        if (WebSocketManager.getInstance().currentUser != null && uid.equals(WebSocketManager.getInstance().currentUser.id)) {
            return WebSocketManager.getInstance().currentUser.title != null ? WebSocketManager.getInstance().currentUser.title : "";
        }
        return "";
    }
    private String resolveUserName(android.content.Context context, String uid, String fromName) {
        // 优先用备注名
        if (uid != null && !uid.isEmpty()) {
            try {
                android.content.SharedPreferences prefs = context.getSharedPreferences("chat_settings", 0);
                String remark = prefs.getString("remark_" + uid, null);
                if (remark != null && !remark.isEmpty()) {
                    return remark;
                }
            } catch (Exception e) {}
        }
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
        TextView tvSenderName, tvContentMe, tvContentOther, tvRecalledMe, tvRecalledOther, tvStatusMe;
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
            tvStatusMe = itemView.findViewById(R.id.tv_status_me);
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

    private static android.media.MediaPlayer currentPlayer;
    public static void stopAudioPlayback() {
        if (currentPlayer != null) {
            try {
                if (currentPlayer.isPlaying()) currentPlayer.stop();
                currentPlayer.release();
            } catch (Exception e) {}
            currentPlayer = null;
        }
    }
    private void playAudioInApp(String url, String name, android.content.Context ctx) {
        try {
            if (currentPlayer != null) {
                currentPlayer.stop();
                currentPlayer.release();
                currentPlayer = null;
            }
            android.widget.Toast.makeText(ctx, "正在播放: " + name, android.widget.Toast.LENGTH_SHORT).show();
            currentPlayer = new android.media.MediaPlayer();
            currentPlayer.setDataSource(url);
            currentPlayer.prepareAsync();
            currentPlayer.setOnPreparedListener(mp -> mp.start());
            currentPlayer.setOnCompletionListener(mp -> {
                mp.release();
                currentPlayer = null;
                android.widget.Toast.makeText(ctx, "播放完成", android.widget.Toast.LENGTH_SHORT).show();
            });
            currentPlayer.setOnErrorListener((mp, what, extra) -> {
                mp.release();
                currentPlayer = null;
                android.widget.Toast.makeText(ctx, "播放失败，尝试用外部播放器打开", android.widget.Toast.LENGTH_SHORT).show();
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
                    intent.setDataAndType(android.net.Uri.parse(url), "audio/*");
                    ctx.startActivity(intent);
                } catch (Exception e) {}
                return true;
            });
        } catch (Exception e) {
            android.widget.Toast.makeText(ctx, "播放失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private static String formatFileSize(long size) {
        if (size <= 0) return "未知大小";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }
}
