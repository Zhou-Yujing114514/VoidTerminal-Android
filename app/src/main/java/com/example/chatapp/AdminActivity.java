package com.example.chatapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.util.SharedPrefs;
import com.example.chatapp.websocket.WebSocketManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {
    private List<JSONObject> userList = new ArrayList<>();
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        WebSocketManager ws = WebSocketManager.getInstance();

        EditText etAnnounce = findViewById(R.id.et_announce);
        EditText etBanUsername = findViewById(R.id.et_ban_username);
        EditText etKickUserid = findViewById(R.id.et_kick_userid);
        EditText etHallName = findViewById(R.id.et_hall_name);
        EditText etMaxOnline = findViewById(R.id.et_max_online);

        ((TextView) findViewById(R.id.btn_announce)).setOnClickListener(v -> {
            String content = etAnnounce.getText().toString().trim();
            if (content.isEmpty() || content.length() > 500) {
                Toast.makeText(this, "公告内容需为1-500字", Toast.LENGTH_SHORT).show();
                return;
            }
            ws.adminAnnounce(content);
            Toast.makeText(this, "公告已发布", Toast.LENGTH_SHORT).show();
            etAnnounce.setText("");
        });

        ((TextView) findViewById(R.id.btn_ban)).setOnClickListener(v -> {
            String name = etBanUsername.getText().toString().trim();
            if (name.isEmpty()) { Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show(); return; }
            ws.adminBanUser(name);
            Toast.makeText(this, "已发送封禁请求", Toast.LENGTH_SHORT).show();
        });

        ((TextView) findViewById(R.id.btn_unban)).setOnClickListener(v -> {
            String name = etBanUsername.getText().toString().trim();
            if (name.isEmpty()) { Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show(); return; }
            ws.adminUnbanUser(name);
            Toast.makeText(this, "已发送解封请求", Toast.LENGTH_SHORT).show();
        });

        ((TextView) findViewById(R.id.btn_kick)).setOnClickListener(v -> {
            String uid = etKickUserid.getText().toString().trim();
            if (uid.isEmpty()) { Toast.makeText(this, "请输入用户ID", Toast.LENGTH_SHORT).show(); return; }
            ws.adminKickUser(uid);
            Toast.makeText(this, "已发送踢出请求", Toast.LENGTH_SHORT).show();
        });

        ((TextView) findViewById(R.id.btn_rename_hall)).setOnClickListener(v -> {
            String name = etHallName.getText().toString().trim();
            if (name.isEmpty() || name.length() > 20) { Toast.makeText(this, "大厅名称需为1-20字", Toast.LENGTH_SHORT).show(); return; }
            ws.adminRenameHall(name);
            Toast.makeText(this, "已发送重命名请求", Toast.LENGTH_SHORT).show();
        });

        ((TextView) findViewById(R.id.btn_clear_hall)).setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("确认清空")
                    .setMessage("确定清空公共大厅聊天记录吗？")
                    .setPositiveButton("清空", (d, w) -> {
                        ws.adminClearHall();
                        Toast.makeText(this, "已发送清空请求", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        ((TextView) findViewById(R.id.btn_set_max)).setOnClickListener(v -> {
            String val = etMaxOnline.getText().toString().trim();
            if (val.isEmpty()) { Toast.makeText(this, "请输入人数", Toast.LENGTH_SHORT).show(); return; }
            try {
                int n = Integer.parseInt(val);
                if (n < 0) { Toast.makeText(this, "不能为负数", Toast.LENGTH_SHORT).show(); return; }
                ws.adminSetMaxOnline(n);
                Toast.makeText(this, "已设置上限为 " + n, Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
            }
        });

        // 用户列表 - 使用 RecyclerView
        RecyclerView rvUsers = findViewById(R.id.rv_users);
        if (rvUsers != null) {
            rvUsers.setLayoutManager(new LinearLayoutManager(this));
            userAdapter = new UserAdapter();
            rvUsers.setAdapter(userAdapter);
        }

        // 用户列表按钮
        ((TextView) findViewById(R.id.btn_user_list)).setOnClickListener(v -> loadUserList());

        // 批量发消息
        EditText etBroadcast = findViewById(R.id.et_broadcast);
        ((TextView) findViewById(R.id.btn_broadcast)).setOnClickListener(v -> {
            String text = etBroadcast.getText().toString().trim();
            if (text.isEmpty()) { Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show(); return; }
            String token = SharedPrefs.getToken(this);
            com.example.chatapp.api.ApiClient.adminBroadcast(token, text, new com.example.chatapp.api.ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    runOnUiThread(() -> {
                        Toast.makeText(AdminActivity.this, "批量消息已发送", Toast.LENGTH_SHORT).show();
                        etBroadcast.setText("");
                    });
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(AdminActivity.this, "失败: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }

    private void loadUserList() {
        String token = SharedPrefs.getToken(this);
        com.example.chatapp.api.ApiClient.getAdminUsers(token, new com.example.chatapp.api.ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> {
                    try {
                        JSONArray users = result.getJSONArray("users");
                        userList.clear();
                        for (int i = 0; i < users.length(); i++) {
                            userList.add(users.getJSONObject(i));
                        }
                        if (userAdapter != null) userAdapter.notifyDataSetChanged();
                        Toast.makeText(AdminActivity.this, "已加载 " + userList.size() + " 个用户", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(AdminActivity.this, "解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(AdminActivity.this, "加载失败: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showUserActions(JSONObject user) {
        try {
            String username = user.getString("username");
            String userId = user.getString("id");
            boolean isAdmin = user.getBoolean("isAdmin");
            boolean banned = user.getBoolean("banned");
            String myId = SharedPrefs.getUserId(this);

            if (userId.equals(myId)) {
                Toast.makeText(this, "不能对自己操作", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> options = new ArrayList<>();
            options.add(banned ? "解封用户" : "封禁用户");
            if (!isAdmin) options.add("删除用户账号");

            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(username + " (" + userId + ")")
                .setItems(options.toArray(new String[0]), (d, which) -> {
                    String opt = options.get(which);
                    if (opt.equals("封禁用户") || opt.equals("解封用户")) {
                        WebSocketManager.getInstance().adminBanUser(username);
                        Toast.makeText(this, "已发送" + (banned ? "解封" : "封禁") + "请求", Toast.LENGTH_SHORT).show();
                    } else if (opt.equals("删除用户账号")) {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("确认删除")
                            .setMessage("确定删除用户「" + username + "」吗？此操作不可恢复！")
                            .setPositiveButton("删除", (d2, w2) -> {
                                String token = SharedPrefs.getToken(this);
                                com.example.chatapp.api.ApiClient.adminDeleteUser(token, userId, new com.example.chatapp.api.ApiClient.Callback() {
                                    @Override
                                    public void onSuccess(JSONObject result) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(AdminActivity.this, "用户已删除", Toast.LENGTH_SHORT).show();
                                            loadUserList();
                                        });
                                    }
                                    @Override
                                    public void onError(String error) {
                                        runOnUiThread(() -> Toast.makeText(AdminActivity.this, "删除失败: " + error, Toast.LENGTH_SHORT).show());
                                    }
                                });
                            })
                            .setNegativeButton("取消", null)
                            .show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            try {
                JSONObject u = userList.get(position);
                StringBuilder sb = new StringBuilder();
                sb.append(u.getString("username"))
                  .append(" (").append(u.getString("id")).append(")")
                  .append(u.getBoolean("online") ? " [在线]" : " [离线]")
                  .append(u.getBoolean("banned") ? " [已封禁]" : "")
                  .append(u.getBoolean("isAdmin") ? " [管理员]" : "");
                if (u.has("title") && !u.isNull("title") && !u.getString("title").isEmpty()) {
                    sb.append(" [").append(u.getString("title")).append("]");
                }
                holder.tv.setText(sb.toString());
                holder.tv.setTextColor(0xFF333333);
                holder.tv.setPadding(40, 30, 40, 30);
                holder.itemView.setOnClickListener(v -> showUserActions(u));
            } catch (Exception e) {}
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(View itemView) {
                super(itemView);
                tv = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
