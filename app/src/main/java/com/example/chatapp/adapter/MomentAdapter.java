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
import com.example.chatapp.model.Moment;
import com.example.chatapp.websocket.WebSocketManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class MomentAdapter extends RecyclerView.Adapter<MomentAdapter.ViewHolder> {
    private List<Moment> moments;
    private String serverBase;
    private String myId;
    public MomentAdapter(List<Moment> moments, String serverBase, String myId) {
        this.moments = moments;
        this.serverBase = serverBase;
        this.myId = myId;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_moment, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Moment m = moments.get(position);
        holder.tvAuthor.setText(m.authorName);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(m.time)));
        holder.tvContent.setText(m.text);
        // 头像
        if (m.authorAvatar != null && !m.authorAvatar.isEmpty()) {
            String url = m.authorAvatar.startsWith("/") ? serverBase + m.authorAvatar : m.authorAvatar;
            Glide.with(holder.ivAvatar.getContext()).load(url).circleCrop().into(holder.ivAvatar);
        }
        // 图片 - 支持点击放大
        if (m.images != null && !m.images.isEmpty()) {
            holder.layoutImages.setVisibility(View.VISIBLE);
            for (int i = 0; i < 3; i++) {
                ImageView iv = i == 0 ? holder.ivImage1 : (i == 1 ? holder.ivImage2 : holder.ivImage3);
                if (i < m.images.size()) {
                    iv.setVisibility(View.VISIBLE);
                    final String imgUrl = m.images.get(i).startsWith("/") ? serverBase + m.images.get(i) : m.images.get(i);
                    Glide.with(iv.getContext()).load(imgUrl).into(iv);
                    iv.setOnClickListener(v -> {
                        Intent intent = new Intent(v.getContext(), ImageViewerActivity.class);
                        intent.putExtra("image_url", imgUrl);
                        v.getContext().startActivity(intent);
                    });
                } else {
                    iv.setVisibility(View.GONE);
                    iv.setOnClickListener(null);
                }
            }
        } else {
            holder.layoutImages.setVisibility(View.GONE);
        }
        // 点赞
        boolean liked = m.likes.contains(myId);
        holder.btnLike.setText(liked ? "已赞" : "点赞");
        holder.tvLikeCount.setText(String.valueOf(m.likes.size()));
        holder.btnLike.setOnClickListener(v -> WebSocketManager.getInstance().likeMoment(m.id));
        // 点赞列表 - 显示名称而不是uid
        if (!m.likes.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String uid : m.likes) {
                if (sb.length() > 0) sb.append("、");
                sb.append(getUserName(uid));
            }
            holder.tvLikes.setText(sb.toString());
            holder.tvLikes.setVisibility(View.VISIBLE);
        } else {
            holder.tvLikes.setVisibility(View.GONE);
        }
        // 评论
        holder.layoutComments.removeAllViews();
        for (Moment.Comment c : m.comments) {
            TextView tv = new TextView(holder.layoutComments.getContext());
            tv.setText(c.userName + ": " + c.text);
            tv.setTextSize(13);
            tv.setTextColor(0xFFEAEAEA);
            tv.setPadding(0, 4, 0, 4);
            holder.layoutComments.addView(tv);
        }
        holder.btnComment.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(v.getContext());
            builder.setTitle("评论");
            final android.widget.EditText input = new android.widget.EditText(v.getContext());
            builder.setView(input);
            builder.setPositiveButton("发送", (d, w) ->
                    WebSocketManager.getInstance().commentMoment(m.id, input.getText().toString()));
            builder.setNegativeButton("取消", null);
            builder.show();
        });
        // 删除（仅自己）
        if (m.author.equals(myId)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(v.getContext())
                        .setTitle("删除动态")
                        .setMessage("确定删除吗？")
                        .setPositiveButton("删除", (d, w) -> WebSocketManager.getInstance().deleteMoment(m.id))
                        .setNegativeButton("取消", null)
                        .show();
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }
    private String getUserName(String uid) {
        for (com.example.chatapp.model.User u : WebSocketManager.getInstance().friends) {
            if (u.id.equals(uid)) return u.username;
        }
        if (WebSocketManager.getInstance().currentUser != null && uid.equals(WebSocketManager.getInstance().currentUser.id)) {
            return WebSocketManager.getInstance().currentUser.username;
        }
        return uid;
    }
    @Override
    public int getItemCount() {
        return moments.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivImage1, ivImage2, ivImage3;
        TextView tvAuthor, tvTime, tvContent, tvLikeCount, tvLikes, btnLike, btnComment, btnDelete;
        LinearLayout layoutImages, layoutComments;
        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            ivImage1 = itemView.findViewById(R.id.iv_image1);
            ivImage2 = itemView.findViewById(R.id.iv_image2);
            ivImage3 = itemView.findViewById(R.id.iv_image3);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            tvLikes = itemView.findViewById(R.id.tv_likes);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnComment = itemView.findViewById(R.id.btn_comment);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            layoutImages = itemView.findViewById(R.id.layout_images);
            layoutComments = itemView.findViewById(R.id.layout_comments);
        }
    }
}
