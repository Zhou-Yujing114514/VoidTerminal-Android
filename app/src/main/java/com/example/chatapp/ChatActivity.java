package com.example.chatapp;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatapp.adapter.MessageAdapter;
import com.example.chatapp.api.ApiClient;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.Message;
import com.example.chatapp.util.MessageStore;
import com.example.chatapp.util.SharedPrefs;
import com.example.chatapp.websocket.WebSocketManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
public class ChatActivity extends AppCompatActivity implements WebSocketManager.WSListener {
    private static final String[] STICKERS = {
            "😀","😂","🤣","😊","😍","🥰","😘","😎","🤔","😴",
            "😭","😡","👍","👎","👏","🙏","💪","🎉","❤️","🔥",
            "✨","🌹","☕","🍺","🎂","⚽","🎮","📱","💯","🌈"
    };
    private RecyclerView rvMessages;
    private EditText etMessage;
    private MessageAdapter adapter;
    private String roomId;
    private String roomName;
    private boolean isGlobal;
    private boolean isGroup;
    private ChatRoom currentRoom;
    private String serverBase;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> bgPickerLauncher;
    private Message quotedMessage;
    private LinearLayout layoutQuote;
    private TextView tvQuoteFrom, tvQuoteContent;
    private HorizontalScrollView layoutStickers;
    private LinearLayout stickerContainer;
    private LinearLayout layoutRoot;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        roomId = getIntent().getStringExtra("room_id");
        roomName = getIntent().getStringExtra("room_name");
        isGlobal = getIntent().getBooleanExtra("is_global", false);
        isGroup = getIntent().getBooleanExtra("is_group", false);
        serverBase = SharedPrefs.getServer(this);
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        TextView btnSend = findViewById(R.id.btn_send);
        TextView btnBack = findViewById(R.id.btn_back);
        TextView btnImage = findViewById(R.id.btn_image);
        TextView btnSticker = findViewById(R.id.btn_sticker);
        TextView btnMore = findViewById(R.id.btn_more);
        TextView tvTitle = findViewById(R.id.tv_chat_title);
        TextView tvOnline = findViewById(R.id.tv_online_status);
        layoutQuote = findViewById(R.id.layout_quote);
        tvQuoteFrom = findViewById(R.id.tv_quote_from);
        tvQuoteContent = findViewById(R.id.tv_quote_content);
        TextView btnCancelQuote = findViewById(R.id.btn_cancel_quote);
        layoutStickers = findViewById(R.id.layout_stickers);
        stickerContainer = findViewById(R.id.sticker_container);
        layoutRoot = findViewById(R.id.layout_chat_root);
        tvTitle.setText(roomName);
        if (isGlobal) {
            tvOnline.setVisibility(View.VISIBLE);
            tvOnline.setText("公共频道");
        } else if (isGroup) {
            tvOnline.setVisibility(View.VISIBLE);
            tvOnline.setText("群聊");
            btnMore.setVisibility(View.VISIBLE);
        }
        applyTheme();
        loadCustomBackground();
        currentRoom = WebSocketManager.getInstance().chatRooms.get(roomId);
        if (currentRoom == null) {
            currentRoom = new ChatRoom(roomId, roomName, isGlobal || isGroup);
            WebSocketManager.getInstance().chatRooms.put(roomId, currentRoom);
        }
        mergeLocalMessages();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        String myAvatar = getSharedPreferences("chatapp_prefs", 0).getString("avatar", "");
        adapter = new MessageAdapter(currentRoom.messages, isGlobal || isGroup, serverBase, myAvatar, this::onMessageLongPress, this::onAvatarLongPress);
        rvMessages.setAdapter(adapter);
        scrollToBottom();
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        btnImage.setOnClickListener(v -> pickImage());
        btnSticker.setOnClickListener(v -> toggleStickerPanel());
        btnCancelQuote.setOnClickListener(v -> clearQuote());
        btnMore.setOnClickListener(v -> showMoreMenu());
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
        initStickers();
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) uploadAndSendImage(uri);
                    }
                });
        bgPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) saveCustomBackground(uri);
                    }
                });
    }
    private void mergeLocalMessages() {
        List<Message> local = MessageStore.loadRoom(this, roomId);
        if (local.isEmpty()) return;
        List<Message> merged = new ArrayList<>(local);
        for (Message sm : currentRoom.messages) {
            boolean exists = false;
            for (Message lm : merged) {
                if (lm.id != null && lm.id.equals(sm.id)) { exists = true; break; }
            }
            if (!exists) merged.add(sm);
        }
        merged.sort((a, b) -> Long.compare(a.time, b.time));
        currentRoom.messages.clear();
        currentRoom.messages.addAll(merged);
    }
    private void initStickers() {
        stickerContainer.removeAllViews();
        for (String emoji : STICKERS) {
            TextView tv = new TextView(this);
            tv.setText(emoji);
            tv.setTextSize(28);
            tv.setPadding(20, 16, 20, 16);
            tv.setOnClickListener(v -> etMessage.getText().insert(etMessage.getSelectionStart(), emoji));
            stickerContainer.addView(tv);
        }
    }
    private void toggleStickerPanel() {
        layoutStickers.setVisibility(layoutStickers.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }
    private void onAvatarLongPress(Message msg) {
        if (msg == null || msg.from == null) return;
        // 不@自己
        if (WebSocketManager.getInstance().currentUser != null &&
            msg.from.equals(WebSocketManager.getInstance().currentUser.id)) return;
        String name = msg.fromName != null && !msg.fromName.isEmpty() ? msg.fromName : msg.from;
        EditText etInput = findViewById(R.id.et_message);
        if (etInput != null) {
            String current = etInput.getText().toString();
            String mention = "@" + name + " ";
            if (!current.contains(mention.trim())) {
                etInput.setText(current + mention);
                etInput.setSelection(etInput.getText().length());
            }
            Toast.makeText(this, "已@" + name, Toast.LENGTH_SHORT).show();
        }
    }

    private void showMoreMenu() {
        if (!isGroup) return;
        new AlertDialog.Builder(this)
                .setTitle("更多")
                .setItems(new String[]{"群设置"}, (dialog, which) -> {
                    Intent intent = new Intent(this, GroupSettingsActivity.class);
                    intent.putExtra("group_id", roomId);
                    startActivity(intent);
                })
                .show();
    }
    private void loadCustomBackground() {
        SharedPreferences sp = getSharedPreferences("chatapp_prefs", 0);
        String bgBase64 = sp.getString("chat_bg_global", "");
        if (!bgBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(bgBase64, Base64.DEFAULT);
                layoutRoot.setBackground(new BitmapDrawable(getResources(),
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.length)));
            } catch (Exception e) {
                layoutRoot.setBackgroundResource(R.color.bg_dark);
            }
        }
    }
    private void applyTheme() {
        // 固定深色模式
    }
    private void pickBackground() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        bgPickerLauncher.launch(intent);
    }
    private void saveCustomBackground(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
            is.close();
            String base64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            getSharedPreferences("chatapp_prefs", 0).edit()
                    .putString("chat_bg_global", base64).apply();
            loadCustomBackground();
            Toast.makeText(this, "背景已设置", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void clearCustomBackground() {
        getSharedPreferences("chatapp_prefs", 0).edit()
                .remove("chat_bg_global").apply();
        layoutRoot.setBackgroundResource(R.color.bg_dark);
        Toast.makeText(this, "背景已清除", Toast.LENGTH_SHORT).show();
    }
    private void onMessageLongPress(Message msg) {
        String myId = getSharedPreferences("chatapp_prefs", 0).getString("user_id", "");
        boolean isMe = msg.from != null && msg.from.equals(myId);
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add("引用");
        if (isMe) items.add("撤回");
        String[] options = items.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (options[which].equals("引用")) setQuote(msg);
                    else if (options[which].equals("撤回")) handleRecall(msg);
                })
                .show();
    }
    private void setQuote(Message msg) {
        quotedMessage = msg;
        layoutQuote.setVisibility(View.VISIBLE);
        String fromName = msg.fromName != null && !msg.fromName.isEmpty() ? msg.fromName : msg.from;
        tvQuoteFrom.setText(fromName);
        String content = msg.hasImage() ? "[图片]" : (msg.content != null ? msg.content : "");
        tvQuoteContent.setText(content);
    }
    private void clearQuote() {
        quotedMessage = null;
        layoutQuote.setVisibility(View.GONE);
    }
    private JSONObject buildQuote() {
        if (quotedMessage == null) return null;
        try {
            JSONObject q = new JSONObject();
            q.put("msgId", quotedMessage.id);
            q.put("content", quotedMessage.hasImage() ? "[图片]" : (quotedMessage.content != null ? quotedMessage.content : ""));
            q.put("from", quotedMessage.from);
            q.put("fromName", quotedMessage.fromName != null ? quotedMessage.fromName : "");
            return q;
        } catch (Exception e) {
            return null;
        }
    }
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
    private void uploadAndSendImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
            is.close();
            String base64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            String token = SharedPrefs.getToken(this);
            String textContent = etMessage.getText().toString().trim();
            JSONObject quote = buildQuote();
            ApiClient.uploadImage(token, base64, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        String url = result.getString("url");
                        JSONArray imgs = new JSONArray();
                        imgs.put(url);
                        if (isGlobal) {
                            WebSocketManager.getInstance().sendGlobalWithImages(textContent, imgs, quote);
                        } else if (isGroup) {
                            WebSocketManager.getInstance().sendGroupWithImages(roomId, textContent, imgs, quote);
                        } else {
                            WebSocketManager.getInstance().sendDmWithImages(roomId, textContent, imgs, quote);
                        }
                    } catch (Exception e) {
                        Toast.makeText(ChatActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "上传失败: " + error, Toast.LENGTH_SHORT).show());
                }
            });
            etMessage.setText("");
            clearQuote();
        } catch (Exception e) {
            Toast.makeText(this, "读取图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void handleRecall(Message msg) {
        String room;
        if (isGlobal) room = "global";
        else if (isGroup) room = "group:" + roomId;
        else room = "dm:" + roomId;
        WebSocketManager.getInstance().recallMessage(msg.id, room);
    }
    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty() && quotedMessage == null) return;
        JSONObject quote = buildQuote();
        if (isGlobal) {
            WebSocketManager.getInstance().sendGlobal(content, quote);
        } else if (isGroup) {
            WebSocketManager.getInstance().sendGroup(roomId, content, quote);
        } else {
            WebSocketManager.getInstance().sendDm(roomId, content, quote);
        }
        etMessage.setText("");
        clearQuote();
    }
    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            rvMessages.post(() -> rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1));
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        WebSocketManager.getInstance().addListener(this);
        WebSocketManager.getInstance().setCurrentVisibleRoom(roomId);
    }
    @Override
    protected void onPause() {
        super.onPause();
        WebSocketManager.getInstance().removeListener(this);
        WebSocketManager.getInstance().setCurrentVisibleRoom(null);
    }
    @Override
    public void onConnected() {
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }
    @Override
    public void onDisconnected() {}
    @Override
    public void onMessage(Message msg, String msgRoomId) {
        if (msgRoomId.equals(roomId)) {
            runOnUiThread(() -> {
                adapter.notifyItemInserted(currentRoom.messages.size() - 1);
                scrollToBottom();
            });
        }
    }
    @Override
    public void onMessageRecalled(String msgId, String recalledRoomId) {
        if (recalledRoomId.equals(roomId)) {
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        }
    }
    @Override
    public void onAvatarUpdate(String userId, String avatar) {
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }
    @Override
    public void onMomentsUpdated() {}
    @Override
    public void onFriendListUpdated() {
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }
    @Override
    public void onFriendRequestReceived() {}
    @Override
    public void onFriendRequestResult(boolean ok, String error) {}
}
