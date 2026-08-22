package com.example.chatapp.fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
    private View layoutAnnouncement;
    private TextView tvAnnouncement;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        rvChatList = view.findViewById(R.id.rv_chat_list);
        rvChatList.setLayoutManager(new LinearLayoutManager(getContext()));
        // 公告横幅
        layoutAnnouncement = view.findViewById(R.id.layout_announcement);
        tvAnnouncement = view.findViewById(R.id.tv_announcement);
        layoutAnnouncement.setOnClickListener(v ->
                startActivity(new Intent(getContext(), AnnouncementActivity.class)));
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
        return view;
    }
    private void refreshList() {
        roomList.clear();
        WebSocketManager ws = WebSocketManager.getInstance();
        // 公共大厅
        roomList.add(ws.globalRoom);
        // 群聊（始终显示）和有消息的私聊
        for (ChatRoom room : ws.chatRooms.values()) {
            if (room.id.equals("global")) continue;
            if (room.isGroup) {
                roomList.add(room);
            } else if (room.messages.size() > 0) {
                roomList.add(room);
            }
        }
        // 按最后消息时间排序
        Collections.sort(roomList, (a, b) -> Long.compare(b.lastTime, a.lastTime));
        if (adapter != null) adapter.notifyDataSetChanged();
        // 更新公告横幅
        if (layoutAnnouncement != null && tvAnnouncement != null) {
            String latestAnnouncement = null;
            for (int i = ws.globalRoom.messages.size() - 1; i >= 0; i--) {
                Message msg = ws.globalRoom.messages.get(i);
                if (msg.content != null && msg.content.startsWith("【站内公告】")) {
                    latestAnnouncement = msg.content.replace("【站内公告】", "").trim();
                    break;
                }
            }
            if (latestAnnouncement != null) {
                tvAnnouncement.setText(latestAnnouncement);
                layoutAnnouncement.setVisibility(View.VISIBLE);
            } else {
                layoutAnnouncement.setVisibility(View.GONE);
            }
        }
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
        if (getActivity() != null) getActivity().runOnUiThread(this::refreshList);
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
    @Override
    public void onMomentsUpdated() {}
    @Override
    public void onFriendListUpdated() {
        if (getActivity() != null) getActivity().runOnUiThread(this::refreshList);
    }
    @Override
    public void onFriendRequestReceived() {}
    @Override
    public void onFriendRequestResult(boolean ok, String error) {}
}
