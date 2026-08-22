package com.example.chatapp.fragment;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatapp.ChatActivity;
import com.example.chatapp.R;
import com.example.chatapp.adapter.FriendAdapter;
import com.example.chatapp.api.ApiClient;
import com.example.chatapp.model.User;
import com.example.chatapp.util.SharedPrefs;
import com.example.chatapp.websocket.WebSocketManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
public class ContactsFragment extends Fragment implements WebSocketManager.WSListener {
    private RecyclerView rvFriends;
    private FriendAdapter adapter;
    private TextView btnFriendRequests;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        rvFriends = view.findViewById(R.id.rv_friends);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        String serverBase = SharedPrefs.getServer(getContext());
        adapter = new FriendAdapter(WebSocketManager.getInstance().friends, serverBase, user -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("room_id", user.id);
            intent.putExtra("room_name", user.username);
            intent.putExtra("is_global", false);
            intent.putExtra("is_group", false);
            startActivity(intent);
        });
        rvFriends.setAdapter(adapter);
        view.findViewById(R.id.btn_add_friend).setOnClickListener(v -> showAddFriendDialog());
        view.findViewById(R.id.btn_search_group).setOnClickListener(v -> showSearchGroupDialog());
        btnFriendRequests = view.findViewById(R.id.btn_friend_requests);
        btnFriendRequests.setOnClickListener(v -> showFriendRequestsDialog());
        updateFriendRequestBadge();
        return view;
    }
    private void showAddFriendDialog() {
        EditText input = new EditText(getContext());
        input.setHint("请输入对方用户名");
        input.setTextColor(0xFFEAEAEA);
        input.setHintTextColor(0xFF8892B0);
        input.setPadding(48, 32, 48, 32);
        new AlertDialog.Builder(getContext())
                .setTitle("添加好友")
                .setView(input)
                .setPositiveButton("发送请求", (d, w) -> {
                    String username = input.getText().toString().trim();
                    if (username.isEmpty()) {
                        Toast.makeText(getContext(), "请输入用户名", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    WebSocketManager.getInstance().sendFriendRequest(username);
                    Toast.makeText(getContext(), "好友请求已发送", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void showSearchGroupDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_search_group, null);
        EditText etKeyword = dialogView.findViewById(R.id.et_keyword);
        LinearLayout layoutResults = dialogView.findViewById(R.id.layout_results);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("搜索群聊")
                .setView(dialogView)
                .setPositiveButton("搜索", null)
                .setNegativeButton("关闭", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String keyword = etKeyword.getText().toString().trim();
                if (keyword.isEmpty()) {
                    Toast.makeText(getContext(), "请输入关键词", Toast.LENGTH_SHORT).show();
                    return;
                }
                layoutResults.removeAllViews();
                TextView tvLoading = new TextView(getContext());
                tvLoading.setText("搜索中...");
                tvLoading.setTextColor(0xFF8892B0);
                tvLoading.setPadding(0, 16, 0, 16);
                layoutResults.addView(tvLoading);
                ApiClient.searchGroups(keyword, new ApiClient.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            layoutResults.removeAllViews();
                            try {
                                JSONArray groups = result.getJSONArray("groups");
                                if (groups.length() == 0) {
                                    TextView tvEmpty = new TextView(getContext());
                                    tvEmpty.setText("未找到相关群聊");
                                    tvEmpty.setTextColor(0xFF8892B0);
                                    tvEmpty.setPadding(0, 16, 0, 16);
                                    layoutResults.addView(tvEmpty);
                                    return;
                                }
                                for (int i = 0; i < groups.length(); i++) {
                                    JSONObject g = groups.getJSONObject(i);
                                    String gid = g.getString("id");
                                    String gname = g.getString("name");
                                    int memberCount = g.getInt("memberCount");
                                    String ownerName = g.optString("ownerName", "未知");
                                    View itemView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, null);
                                    TextView tv1 = itemView.findViewById(android.R.id.text1);
                                    TextView tv2 = itemView.findViewById(android.R.id.text2);
                                    tv1.setText(gname + " (" + memberCount + "人)");
                                    tv1.setTextColor(0xFFEAEAEA);
                                    tv2.setText("群主: " + ownerName);
                                    tv2.setTextColor(0xFF8892B0);
                                    itemView.setOnClickListener(v2 -> {
                                        new AlertDialog.Builder(getContext())
                                                .setTitle("申请加入")
                                                .setMessage("确定申请加入「" + gname + "」吗？")
                                                .setPositiveButton("申请", (d2, w2) -> {
                                                    WebSocketManager.getInstance().applyGroup(gid);
                                                    Toast.makeText(getContext(), "已发送申请，请等待群主审批", Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                })
                                                .setNegativeButton("取消", null)
                                                .show();
                                    });
                                    layoutResults.addView(itemView);
                                }
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    @Override
                    public void onError(String error) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            layoutResults.removeAllViews();
                            TextView tvError = new TextView(getContext());
                            tvError.setText("搜索失败: " + error);
                            tvError.setTextColor(0xFFE53935);
                            tvError.setPadding(0, 16, 0, 16);
                            layoutResults.addView(tvError);
                        });
                    }
                });
            });
        });
        dialog.show();
    }
    private void showFriendRequestsDialog() {
        List<WebSocketManager.FriendRequest> requests = WebSocketManager.getInstance().friendRequests;
        if (requests.isEmpty()) {
            Toast.makeText(getContext(), "暂无好友请求", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = new String[requests.size()];
        for (int i = 0; i < requests.size(); i++) {
            items[i] = requests.get(i).fromName + " 请求添加你为好友";
        }
        new AlertDialog.Builder(getContext())
                .setTitle("好友请求")
                .setItems(items, (d, which) -> {
                    WebSocketManager.FriendRequest req = requests.get(which);
                    new AlertDialog.Builder(getContext())
                            .setTitle(req.fromName)
                            .setMessage("是否接受 " + req.fromName + " 的好友请求？")
                            .setPositiveButton("接受", (d2, w2) -> {
                                WebSocketManager.getInstance().acceptFriendRequest(req.id);
                                Toast.makeText(getContext(), "已接受好友请求", Toast.LENGTH_SHORT).show();
                                updateFriendRequestBadge();
                            })
                            .setNegativeButton("拒绝", (d2, w2) -> {
                                WebSocketManager.getInstance().denyFriendRequest(req.id);
                                Toast.makeText(getContext(), "已拒绝好友请求", Toast.LENGTH_SHORT).show();
                                updateFriendRequestBadge();
                            })
                            .show();
                })
                .show();
    }
    private void updateFriendRequestBadge() {
        if (btnFriendRequests != null) {
            int count = WebSocketManager.getInstance().friendRequests.size();
            if (count > 0) {
                btnFriendRequests.setText("好友请求(" + count + ")");
            } else {
                btnFriendRequests.setText("好友请求");
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        WebSocketManager.getInstance().addListener(this);
        if (adapter != null) adapter.notifyDataSetChanged();
        updateFriendRequestBadge();
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
    public void onAvatarUpdate(String userId, String avatar) {
        if (getActivity() != null && adapter != null)
            getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
    }
    @Override
    public void onMomentsUpdated() {}
    @Override
    public void onFriendListUpdated() {
        if (getActivity() != null && adapter != null)
            getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
    }
    @Override
    public void onFriendRequestReceived() {
        if (getActivity() != null)
            getActivity().runOnUiThread(this::updateFriendRequestBadge);
    }
    @Override
    public void onFriendRequestResult(boolean ok, String error) {
        if (getActivity() != null)
            getActivity().runOnUiThread(() -> {
                if (!ok && error != null && !error.isEmpty()) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
    }
}
