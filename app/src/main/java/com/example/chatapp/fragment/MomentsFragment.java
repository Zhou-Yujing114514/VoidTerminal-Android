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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.chatapp.PostMomentActivity;
import com.example.chatapp.R;
import com.example.chatapp.adapter.MomentAdapter;
import com.example.chatapp.util.SharedPrefs;
import com.example.chatapp.websocket.WebSocketManager;
public class MomentsFragment extends Fragment implements WebSocketManager.WSListener {
    private RecyclerView rvMoments;
    private MomentAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moments, container, false);
        rvMoments = view.findViewById(R.id.rv_moments);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        TextView btnPost = view.findViewById(R.id.btn_post_moment);
        rvMoments.setLayoutManager(new LinearLayoutManager(getContext()));
        String serverBase = SharedPrefs.getServer(getContext());
        String myId = SharedPrefs.getUserId(getContext());
        adapter = new MomentAdapter(WebSocketManager.getInstance().moments, serverBase, myId);
        rvMoments.setAdapter(adapter);
        btnPost.setOnClickListener(v -> startActivity(new Intent(getContext(), PostMomentActivity.class)));
        swipeRefresh.setOnRefreshListener(() -> {
            adapter.notifyDataSetChanged();
            swipeRefresh.setRefreshing(false);
        });
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        WebSocketManager.getInstance().addListener(this);
        if (adapter != null) adapter.notifyDataSetChanged();
    }
    @Override
    public void onPause() {
        super.onPause();
        WebSocketManager.getInstance().removeListener(this);
    }
    @Override
    public void onConnected() {
        if (getActivity() != null && adapter != null)
            getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
    }
    @Override
    public void onDisconnected() {}
    @Override
    public void onMessage(com.example.chatapp.model.Message msg, String roomId) {}
    @Override
    public void onMessageRecalled(String msgId, String roomId) {}
    @Override
    public void onAvatarUpdate(String userId, String avatar) {}
    @Override public void onTyping(String fromUid) {}
    @Override public void onMessageRead(String fromUid, String msgId) {}
    @Override public void onGroupAnnouncement(String gid, String text, long time) {}
    @Override public void onFileUploadComplete(String fileId, String url, String filename, long size) {}
    @Override public void onFileUploadError(String fileId, String error) {}
    @Override public void onFileChunkAck(String fileId, int chunkIndex) {}
    @Override
    public void onMomentsUpdated() {
        if (getActivity() != null && adapter != null)
            getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
    }
    @Override
    public void onFriendListUpdated() {}
    @Override
    public void onFriendRequestReceived() {}
    @Override
    public void onFriendRequestResult(boolean ok, String error) {}

    @Override
    public void onMomentNotify(String action, String fromName, String momentText, String commentText) {
        if (getActivity() != null) getActivity().runOnUiThread(() -> {
            String title = action.equals("reply") ? fromName + " 回复了你的评论" : fromName + " 评论了你的动态";
            String msg = commentText;
            if (momentText != null && !momentText.isEmpty()) {
                msg = "动态: " + momentText + "\n评论: " + commentText;
            }
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("查看", (d, w) -> {
                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .setNegativeButton("关闭", null)
                .show();
        });
    }
    @Override
    public void onTitleUpdate(String userId, String title) {}
    @Override
    public void onStatusUpdate(String userId, String status) {}
    @Override
    public void onPresenceUpdate() {}

}
