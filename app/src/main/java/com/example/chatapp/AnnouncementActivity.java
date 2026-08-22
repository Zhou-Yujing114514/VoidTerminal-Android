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

        // 从公共大厅提取公告
        List<Message> announcements = new ArrayList<>();
        for (Message msg : WebSocketManager.getInstance().globalRoom.messages) {
            if (msg.content != null && msg.content.startsWith("【站内公告】")) {
                announcements.add(0, msg);
            }
        }

        if (announcements.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无公告");
            empty.setTextColor(0xFF888888);
            empty.setPadding(48, 48, 48, 48);
            empty.setGravity(android.view.Gravity.CENTER);
            ((ViewGroup) rv.getParent()).addView(empty);
            rv.setVisibility(View.GONE);
        } else {
            rv.setAdapter(new AnnouncementAdapter(announcements));
        }
    }

    private static class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.VH> {
        private List<Message> list;
        AnnouncementAdapter(List<Message> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Message msg = list.get(position);
            holder.text1.setText(msg.content.replace("【站内公告】", "").trim());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            holder.text2.setText(sdf.format(new Date(msg.time)));
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView text1, text2;
            VH(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
                text1.setTextColor(0xFFEAEAEA);
                text2.setTextColor(0xFF888888);
                text1.setMaxLines(10);
            }
        }
    }
}
