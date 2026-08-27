package com.example.chatapp.fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatapp.AnnouncementActivity;
import com.example.chatapp.ChatActivity;
import com.example.chatapp.R;
import com.example.chatapp.adapter.ChatListAdapter;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.Message;
import com.example.chatapp.util.SharedPrefs;
import com.example.chatapp.websocket.WebSocketManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class ChatListFragment extends Fragment implements WebSocketManager.WSListener {
    private RecyclerView rvChatList;
    private ChatListAdapter adapter;
    private List<ChatRoom> roomList = new ArrayList<>();
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private List<ChatRoom> allRoomList = new ArrayList<>();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        rvChatList = view.findViewById(R.id.rv_chat_list);
        rvChatList.setLayoutManager(new LinearLayoutManager(getContext()));
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(() -> {
            // 重新连接 WebSocket 拉取历史消息
            if (WebSocketManager.getInstance().isConnected()) {
                WebSocketManager.getInstance().disconnect();
            }
            String token = com.example.chatapp.util.SharedPrefs.getToken(getContext());
            if (token != null) {
                WebSocketManager.getInstance().connect(token);
            }
            // 2秒后停止刷新动画
            new android.os.Handler().postDelayed(() -> {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                refreshList();
            }, 2000);
        });
        etSearch = view.findViewById(R.id.et_search);
        // +按钮：加好友/建群菜单
        View btnAdd = view.findViewById(R.id.btn_add_friend_chat);
        btnAdd.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("选择操作")
                .setItems(new String[]{"添加好友", "创建群聊"}, (d, which) -> {
                    if (which == 0) {
                        // 加好友
                        android.widget.EditText input = new android.widget.EditText(getContext());
                        input.setHint("请输入对方用户名");
                        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                            .setTitle("添加好友")
                            .setView(input)
                            .setPositiveButton("发送请求", (d2, w2) -> {
                                String username = input.getText().toString().trim();
                                if (!username.isEmpty()) {
                                    WebSocketManager.getInstance().sendFriendRequest(username);
                                    android.widget.Toast.makeText(getContext(), "好友请求已发送", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                    } else {
                        // 建群
                        startActivity(new Intent(getContext(), com.example.chatapp.CreateGroupActivity.class));
                    }
                })
                .setNegativeButton("取消", null)
                .show();
        });
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRooms(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        String serverBase = SharedPrefs.getServer(getContext());
        adapter = new ChatListAdapter(roomList, serverBase, room -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("room_id", room.id);
            intent.putExtra("room_name", room.name);
            boolean isGlobalRoom = "global".equals(room.id);
            intent.putExtra("is_global", isGlobalRoom);
            intent.putExtra("is_group", room.isGroup && !isGlobalRoom);
            startActivity(intent);
        });
        rvChatList.setAdapter(adapter);
        refreshList();
        // 聊天列表长按菜单
        adapter.setOnItemLongClickListener(room -> {
            String[] options;
            if (room.isGroup || "global".equals(room.id)) {
                options = new String[]{isTopRoom(room.id) ? "取消置顶" : "置顶", isMutedRoom(room.id) ? "取消免打扰" : "消息免打扰"};
            } else {
                options = new String[]{isTopRoom(room.id) ? "取消置顶" : "置顶", isMutedRoom(room.id) ? "取消免打扰" : "消息免打扰", "设置备注名"};
            }
            new android.app.AlertDialog.Builder(getContext())
                    .setItems(options, (d, which) -> {
                        android.content.SharedPreferences prefs = getContext().getSharedPreferences("chat_settings", 0);
                        if (which == 0) {
                            prefs.edit().putBoolean("top_" + room.id, !isTopRoom(room.id)).apply();
                            refreshList();
                        } else if (which == 1) {
                            prefs.edit().putBoolean("mute_" + room.id, !isMutedRoom(room.id)).apply();
                            android.widget.Toast.makeText(getContext(), isMutedRoom(room.id) ? "已开启免打扰" : "已关闭免打扰", android.widget.Toast.LENGTH_SHORT).show();
                        } else if (which == 2) {
                            // 设置备注名
                            final android.widget.EditText et = new android.widget.EditText(getContext());
                            et.setText(getRemark(room.id) != null ? getRemark(room.id) : "");
                            et.setHint("输入备注名");
                            new android.app.AlertDialog.Builder(getContext())
                                    .setTitle("设置备注名")
                                    .setView(et)
                                    .setPositiveButton("确定", (d2, w) -> {
                                        String remark = et.getText().toString().trim();
                                        if (remark.isEmpty()) {
                                            prefs.edit().remove("remark_" + room.id).apply();
                                        } else {
                                            prefs.edit().putString("remark_" + room.id, remark).apply();
                                        }
                                        refreshList();
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        }
                    })
                    .show();
        });
        return view;
    }
    private boolean isTopRoom(String roomId) {
        return getContext() != null && getContext().getSharedPreferences("chat_settings", 0).getBoolean("top_" + roomId, false);
    }
    private boolean isMutedRoom(String roomId) {
        return getContext() != null && getContext().getSharedPreferences("chat_settings", 0).getBoolean("mute_" + roomId, false);
    }
    private String getRemark(String uid) {
        if (getContext() == null) return null;
        return getContext().getSharedPreferences("chat_settings", 0).getString("remark_" + uid, null);
    }
    private void checkAndShowAnnouncement() {
        try {
            WebSocketManager ws = WebSocketManager.getInstance();
            if (ws.globalRoom == null || ws.globalRoom.messages == null) return;
            String latestAnnouncement = null;
            for (int i = ws.globalRoom.messages.size() - 1; i >= 0; i--) {
                Message msg = ws.globalRoom.messages.get(i);
                if (msg.content != null && msg.content.startsWith("【站内公告】")) {
                    latestAnnouncement = msg.content.replace("【站内公告】", "").trim();
                    break;
                }
            }
            if (latestAnnouncement == null || latestAnnouncement.isEmpty()) return;
            String confirmed = getContext().getSharedPreferences("announcement_prefs", 0).getString("confirmed_announcement", "");
            if (!latestAnnouncement.equals(confirmed)) {
                final String ann = latestAnnouncement;
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("📢 站内公告")
                    .setMessage(ann)
                    .setPositiveButton("我知道了", (d, w) -> {
                        getContext().getSharedPreferences("announcement_prefs", 0).edit().putString("confirmed_announcement", ann).apply();
                    })
                    .setCancelable(false)
                    .show();
            }
        } catch (Exception e) {}
    }

    private void refreshList() {
        // 更新头像和自定义状态
        try {
            ImageView ivMyAvatar = getView() != null ? (ImageView) getView().findViewById(R.id.iv_my_avatar) : null;
            TextView tvMyUsername = getView() != null ? (TextView) getView().findViewById(R.id.tv_my_username) : null;
            TextView tvMyStatus = getView() != null ? (TextView) getView().findViewById(R.id.tv_my_status) : null;
            if (WebSocketManager.getInstance().currentUser != null) {
                String avatar = WebSocketManager.getInstance().currentUser.avatar;
                String username = WebSocketManager.getInstance().currentUser.username;
                String status = WebSocketManager.getInstance().currentUser.status;
                if (ivMyAvatar != null) {
                    if (avatar != null && !avatar.isEmpty()) {
                        String fullUrl = avatar.startsWith("http") ? avatar : (SharedPrefs.getServer(getContext()) + avatar);
                        com.bumptech.glide.Glide.with(ivMyAvatar.getContext()).load(fullUrl).circleCrop().placeholder(R.drawable.bg_avatar).error(R.drawable.bg_avatar).into(ivMyAvatar);
                    } else {
                        ivMyAvatar.setImageResource(R.drawable.bg_avatar);
                    }
                }
                if (tvMyUsername != null && username != null) {
                    tvMyUsername.setText(username);
                }
                if (tvMyStatus != null) {
                    if (status != null && !status.isEmpty()) {
                        tvMyStatus.setText(status);
                        tvMyStatus.setVisibility(View.VISIBLE);
                    } else {
                        tvMyStatus.setVisibility(View.GONE);
                    }
                }
            }
        } catch (Exception e) {}
        // 检查并弹出公告
        checkAndShowAnnouncement();
        allRoomList.clear();
        WebSocketManager ws = WebSocketManager.getInstance();
        // 公共大厅
        allRoomList.add(ws.globalRoom);
        // 群聊（始终显示）和有消息的私聊
        for (ChatRoom room : ws.chatRooms.values()) {
            if (room.id.equals("global")) continue;
            if (room.isGroup) {
                allRoomList.add(room);
            } else if (room.messages.size() > 0) {
                allRoomList.add(room);
            }
        }
        // 按置顶状态和最后消息时间排序（置顶的排前面）
        Collections.sort(allRoomList, (a, b) -> {
            boolean aTop = isTopRoom(a.id);
            boolean bTop = isTopRoom(b.id);
            if (aTop && !bTop) return -1;
            if (!aTop && bTop) return 1;
            return Long.compare(b.lastTime, a.lastTime);
        });
        // 应用搜索过滤
        if (etSearch != null && etSearch.getText().toString().trim().length() > 0) {
            filterRooms(etSearch.getText().toString());
        } else {
            roomList.clear();
            roomList.addAll(allRoomList);
            if (adapter != null) adapter.notifyDataSetChanged();
        }


    }
    private void filterRooms(String keyword) {
        roomList.clear();
        if (keyword == null || keyword.trim().isEmpty()) {
            roomList.addAll(allRoomList);
        } else {
            String kw = keyword.toLowerCase();
            for (ChatRoom room : allRoomList) {
                if (room.name != null && room.name.toLowerCase().contains(kw)) {
                    roomList.add(room);
                } else if (room.id != null && room.id.toLowerCase().contains(kw)) {
                    roomList.add(room);
                } else {
                    if (room.messages != null && !room.messages.isEmpty()) {
                        Message lastMsg = room.messages.get(room.messages.size() - 1);
                        if (lastMsg.content != null && lastMsg.content.toLowerCase().contains(kw)) {
                            roomList.add(room);
                        }
                    }
                }
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        WebSocketManager.getInstance().addListener(this);
        refreshList();
    }
    @Override
    public void onPause() {
        super.onPause();
        WebSocketManager.getInstance().removeListener(this);
    }
    @Override
    public void onConnected() {
        if (getActivity() != null) getActivity().runOnUiThread(() -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            refreshList();
        });
    }
    @Override
    public void onDisconnected() {}
    @Override
    public void onMessage(Message msg, String roomId) {
        if (getActivity() != null) getActivity().runOnUiThread(this::refreshList);
    }
    @Override
    public void onMessageRecalled(String msgId, String roomId) {
        if (getActivity() != null) getActivity().runOnUiThread(this::refreshList);
    }
    @Override
    public void onAvatarUpdate(String userId, String avatar) {
        if (getActivity() != null) getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
    }
    @Override public void onTyping(String fromUid) {}
    @Override public void onMessageRead(String fromUid, String msgId) {}
    @Override public void onGroupAnnouncement(String gid, String text, long time) {}
    @Override public void onFileUploadComplete(String fileId, String url, String filename, long size) {}
    @Override public void onFileUploadError(String fileId, String error) {}
    @Override public void onFileChunkAck(String fileId, int chunkIndex) {}
    @Override
    public void onMomentsUpdated() {}
    @Override
    public void onFriendListUpdated() {
        if (getActivity() != null) getActivity().runOnUiThread(this::refreshList);
    }

    @Override
    public void onMomentNotify(String action, String fromName, String momentText, String commentText) {}
    @Override
    public void onFriendRequestReceived() {}
    @Override
    public void onFriendRequestResult(boolean ok, String error) {}
    @Override
    public void onTitleUpdate(String userId, String title) {}
    @Override
    public void onStatusUpdate(String userId, String status) {}
    @Override
    public void onPresenceUpdate() {}

}
