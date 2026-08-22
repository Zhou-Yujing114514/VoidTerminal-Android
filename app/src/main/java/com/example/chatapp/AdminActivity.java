package com.example.chatapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.websocket.WebSocketManager;

public class AdminActivity extends AppCompatActivity {
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
    }
}
