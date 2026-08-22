package com.example.chatapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.model.User;
import com.example.chatapp.websocket.WebSocketManager;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupActivity extends AppCompatActivity {
    private EditText etGroupName;
    private FriendSelectAdapter adapter;
    private List<User> friends = new ArrayList<>();
    private List<String> selectedIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        etGroupName = findViewById(R.id.et_group_name);
        RecyclerView rvFriends = findViewById(R.id.rv_friends);
        TextView btnCancel = findViewById(R.id.btn_cancel);
        TextView btnCreate = findViewById(R.id.btn_create);

        friends.addAll(WebSocketManager.getInstance().friends);
        adapter = new FriendSelectAdapter();
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> finish());
        btnCreate.setOnClickListener(v -> createGroup());
    }

    private void createGroup() {
        String name = etGroupName.getText().toString().trim();
        if (name.isEmpty() || name.length() > 20) {
            Toast.makeText(this, "群名称需为1-20位字符", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "请至少选择一位好友", Toast.LENGTH_SHORT).show();
            return;
        }
        WebSocketManager.getInstance().createGroup(name, selectedIds);
        Toast.makeText(this, "群聊创建请求已发送", Toast.LENGTH_SHORT).show();
        finish();
    }

    private class FriendSelectAdapter extends RecyclerView.Adapter<FriendSelectAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            User user = friends.get(position);
            holder.text.setText(user.username);
            holder.checkBox.setChecked(selectedIds.contains(user.id));
            holder.itemView.setOnClickListener(v -> {
                if (selectedIds.contains(user.id)) {
                    selectedIds.remove(user.id);
                } else {
                    selectedIds.add(user.id);
                }
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView text;
            CheckBox checkBox;
            VH(View itemView) {
                super(itemView);
                text = itemView.findViewById(android.R.id.text1);
                checkBox = itemView.findViewById(android.R.id.checkbox);
                text.setTextColor(0xFFEAEAEA);
            }
        }
    }
}
