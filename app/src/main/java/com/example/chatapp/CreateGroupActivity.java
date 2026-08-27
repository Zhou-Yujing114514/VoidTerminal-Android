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
        try {
            friends.addAll(WebSocketManager.getInstance().friends);
        } catch (Exception e) {
            // friends 列表为空时不崩溃
        }
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
        try {
            WebSocketManager.getInstance().createGroup(name, selectedIds);
            Toast.makeText(this, "群聊创建请求已发送", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "创建失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private class FriendSelectAdapter extends RecyclerView.Adapter<FriendSelectAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_select, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            User user = friends.get(position);
            holder.tvName.setText(user.username);
            holder.cbFriend.setChecked(selectedIds.contains(user.id));
            holder.cbFriend.setClickable(false);
            holder.cbFriend.setFocusable(false);
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
            TextView tvName;
            CheckBox cbFriend;
            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_friend_name);
                cbFriend = itemView.findViewById(R.id.cb_friend);
            }
        }
    }
}
