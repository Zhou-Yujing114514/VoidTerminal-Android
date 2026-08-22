package com.example.chatapp;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatapp.model.Message;
import com.example.chatapp.websocket.WebSocketManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class AnnouncementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcement);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        RecyclerView rv = findViewById(R.id.rv_announcements);
        rv.setLayoutManager(new LinearLayoutManager(this));
        TextView tvEmpty = findViewById(R.id.tv_empty);
        try {
            List<Message> announcements = new ArrayList<>();
            if (WebSocketManager.getInstance().globalRoom != null) {
                for (Message msg : WebSocketManager.getInstance().globalRoom.messages) {
                    if (msg != null && msg.content != null && msg.content.startsWith("【站内公告】")) {
                        announcements.add(0, msg);
                    }
                }
            }
            if (announcements.isEmpty()) {
                rv.setVisibility(View.GONE);
                if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
            } else {
                if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                rv.setAdapter(new AnnouncementAdapter(announcements));
            }
        } catch (Exception e) {
            rv.setVisibility(View.GONE);
            if (tvEmpty != null) {
                tvEmpty.setText("加载失败: " + e.getMessage());
                tvEmpty.setVisibility(View.VISIBLE);
            }
        }
    }
    private static class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.VH> {
        private List<Message> list;
        AnnouncementAdapter(List<Message> list) { this.list = list; }
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Message msg = list.get(position);
            holder.tvContent.setText(msg.content.replace("【站内公告】", "").trim());
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                holder.tvTime.setText(sdf.format(new Date(msg.time)));
            } catch (Exception e) {
                holder.tvTime.setText("");
            }
        }
        @Override
        public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvContent, tvTime;
            VH(View itemView) {
                super(itemView);
                tvContent = itemView.findViewById(R.id.tv_announcement_content);
                tvTime = itemView.findViewById(R.id.tv_announcement_time);
            }
        }
    }
}
