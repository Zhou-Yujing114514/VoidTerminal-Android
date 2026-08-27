package com.example.chatapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.model.Group;
import com.example.chatapp.model.User;
import com.example.chatapp.util.SharedPrefs;
import com.example.chatapp.websocket.WebSocketManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GroupSettingsActivity extends AppCompatActivity {
    private String groupId;
    private Group group;
    private String myId;
    private boolean isOwner;
    private boolean isGroupAdmin;
    private MemberAdapter adapter;
    private List<String> memberIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_settings);

        groupId = getIntent().getStringExtra("group_id");
        myId = SharedPrefs.getUserId(this);
        group = WebSocketManager.getInstance().groups.stream()
                .filter(g -> g.id.equals(groupId))
                .findFirst().orElse(null);
        if (group == null) {
            Toast.makeText(this, "群不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        isOwner = group.owner != null && group.owner.equals(myId);
        isGroupAdmin = isOwner || (group.admins != null && group.admins.contains(myId));

        EditText etName = findViewById(R.id.et_group_name);
        Button btnRename = findViewById(R.id.btn_rename);
        Button btnAdd = findViewById(R.id.btn_add_member);
        Button btnLeave = findViewById(R.id.btn_leave);
        Button btnDissolve = findViewById(R.id.btn_dissolve);
        Button btnAllMute = findViewById(R.id.btn_all_mute);
        TextView btnBack = findViewById(R.id.btn_back);

        etName.setText(group.name);
        if (isOwner) {
            etName.setEnabled(true);
            btnRename.setVisibility(View.VISIBLE);
            btnAdd.setVisibility(View.VISIBLE);
            btnDissolve.setVisibility(View.VISIBLE);
            btnLeave.setVisibility(View.GONE);
            btnAllMute.setVisibility(View.VISIBLE);
        } else if (isGroupAdmin) {
            etName.setEnabled(true);
            btnRename.setVisibility(View.VISIBLE);
            btnAdd.setVisibility(View.VISIBLE);
            btnAllMute.setVisibility(View.VISIBLE);
        }

        // 全体禁言按钮状态
        updateAllMuteButton(btnAllMute);

        btnBack.setOnClickListener(v -> finish());
        btnRename.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (newName.isEmpty() || newName.length() > 20) {
                Toast.makeText(this, "群名称需为1-20位", Toast.LENGTH_SHORT).show();
                return;
            }
            WebSocketManager.getInstance().renameGroup(groupId, newName);
            group.name = newName;
            Toast.makeText(this, "已修改", Toast.LENGTH_SHORT).show();
        });
        btnAdd.setOnClickListener(v -> showAddMemberDialog());
        btnAllMute.setOnClickListener(v -> {
            boolean newMuted = !group.allMuted;
            WebSocketManager.getInstance().setGroupAllMuted(groupId, newMuted);
            group.allMuted = newMuted;
            updateAllMuteButton(btnAllMute);
            Toast.makeText(this, newMuted ? "已开启全体禁言" : "已关闭全体禁言", Toast.LENGTH_SHORT).show();
        });
        btnLeave.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("退出群聊")
                    .setMessage("确定退出「" + group.name + "」吗？")
                    .setPositiveButton("退出", (d, w) -> {
                        WebSocketManager.getInstance().leaveGroup(groupId);
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        btnDissolve.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("解散群聊")
                    .setMessage("确定解散「" + group.name + "」吗？此操作不可恢复！")
                    .setPositiveButton("解散", (d, w) -> {
                        WebSocketManager.getInstance().dissolveGroup(groupId);
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 成员列表
        memberIds.clear();
        if (group.members != null) memberIds.addAll(group.members);
        RecyclerView rv = findViewById(R.id.rv_members);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemberAdapter();
        rv.setAdapter(adapter);
    }

    private void updateAllMuteButton(Button btn) {
        if (group.allMuted) {
            btn.setText("关闭全体禁言");
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE53935));
        } else {
            btn.setText("开启全体禁言");
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF9800));
        }
    }

    private void showAddMemberDialog() {
        List<User> friends = WebSocketManager.getInstance().friends;
        if (friends.isEmpty()) {
            Toast.makeText(this, "暂无好友可添加", Toast.LENGTH_SHORT).show();
            return;
        }
        List<User> available = new ArrayList<>();
        for (User u : friends) {
            if (!memberIds.contains(u.id)) available.add(u);
        }
        if (available.isEmpty()) {
            Toast.makeText(this, "所有好友都已在群中", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[available.size()];
        boolean[] checked = new boolean[available.size()];
        for (int i = 0; i < available.size(); i++) {
            names[i] = available.get(i).username;
        }
        new AlertDialog.Builder(this)
                .setTitle("选择要添加的好友")
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("添加", (d, w) -> {
                    JSONArray ids = new JSONArray();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) ids.put(available.get(i).id);
                    }
                    if (ids.length() > 0) {
                        WebSocketManager.getInstance().addGroupMembers(groupId, ids);
                        Toast.makeText(this, "已发送添加请求", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String getMemberName(String uid) {
        for (User u : WebSocketManager.getInstance().friends) {
            if (u.id.equals(uid)) return u.username;
        }
        if (uid.equals(myId)) return SharedPrefs.getUsername(this);
        if (uid.equals(group.owner)) return "群主";
        return uid;
    }

    private void showMemberActions(String uid) {
        if (uid.equals(myId)) {
            Toast.makeText(this, "不能对自己操作", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = getMemberName(uid);
        boolean isAdmin = group.admins != null && group.admins.contains(uid);
        boolean isMuted = group.muted != null && group.muted.contains(uid);

        List<String> options = new ArrayList<>();
        options.add(isMuted ? "解除禁言" : "禁言该成员");
        if (isOwner) {
            options.add(isAdmin ? "取消管理员" : "设为管理员");
        }
        if (isGroupAdmin && !uid.equals(group.owner)) {
            options.add("移出群聊");
        }

        new AlertDialog.Builder(this)
            .setTitle(name + " (" + uid + ")")
            .setItems(options.toArray(new String[0]), (d, which) -> {
                String opt = options.get(which);
                if (opt.equals("禁言该成员") || opt.equals("解除禁言")) {
                    boolean mute = opt.equals("禁言该成员");
                    WebSocketManager.getInstance().muteGroupUser(groupId, uid, mute);
                    if (mute) {
                        if (!group.muted.contains(uid)) group.muted.add(uid);
                    } else {
                        group.muted.remove(uid);
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, mute ? "已禁言" : "已解除禁言", Toast.LENGTH_SHORT).show();
                } else if (opt.equals("设为管理员") || opt.equals("取消管理员")) {
                    boolean setAdmin = opt.equals("设为管理员");
                    WebSocketManager.getInstance().setGroupAdmin(groupId, uid, setAdmin);
                    if (setAdmin) {
                        if (!group.admins.contains(uid)) group.admins.add(uid);
                    } else {
                        group.admins.remove(uid);
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, setAdmin ? "已设为管理员" : "已取消管理员", Toast.LENGTH_SHORT).show();
                } else if (opt.equals("移出群聊")) {
                    new AlertDialog.Builder(this)
                        .setTitle("确认移出")
                        .setMessage("确定将「" + name + "」移出群聊？")
                        .setPositiveButton("移出", (d2, w2) -> {
                            WebSocketManager.getInstance().removeGroupMember(groupId, uid);
                            memberIds.remove(uid);
                            adapter.notifyDataSetChanged();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String uid = memberIds.get(position);
            StringBuilder sb = new StringBuilder();
            sb.append(getMemberName(uid));
            if (uid.equals(group.owner)) sb.append("（群主）");
            else if (group.admins != null && group.admins.contains(uid)) sb.append("（管理员）");
            if (group.muted != null && group.muted.contains(uid)) sb.append("（已禁言）");
            holder.tv.setText(sb.toString());
            holder.tv.setTextColor(0xFF333333);
            holder.tv.setPadding(40, 30, 40, 30);
            holder.itemView.setOnClickListener(v -> {
                if (isGroupAdmin) showMemberActions(uid);
            });
        }

        @Override
        public int getItemCount() {
            return memberIds.size();
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
