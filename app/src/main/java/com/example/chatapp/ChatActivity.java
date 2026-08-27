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
    private final java.util.Set<String> processedMsgIds = new java.util.HashSet<>();
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
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> audioPickerLauncher;
    private ActivityResultLauncher<Intent> stickerPickerLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> bgPickerLauncher;
    private Message quotedMessage;
    private LinearLayout layoutQuote;
    private TextView tvQuoteFrom, tvQuoteContent;
    private HorizontalScrollView layoutStickers;
    private LinearLayout stickerContainer;
    private LinearLayout layoutMorePanel;
    private TextView tvTypingStatus;
    private android.widget.HorizontalScrollView layoutMention;
    private LinearLayout mentionContainer;
    private LinearLayout layoutGroupAnnouncement;
    private androidx.appcompat.app.AlertDialog announcementDialog;
    private TextView tvGroupAnnouncement;
    private String currentGroupAnnouncement = "";
    // 文件分块上传回调
    private final java.util.Map<String, java.util.function.Consumer<org.json.JSONObject>> pendingUploads = new java.util.HashMap<>();
    // 块确认机制
    private final Object chunkAckLock = new Object();
    private String waitingAckFileId = null;
    private int waitingAckChunkIndex = -1;
    private boolean chunkAckReceived = false;
    private java.util.Map<String, java.util.function.Consumer<Integer>> chunkAckCallbacks = new java.util.concurrent.ConcurrentHashMap<>();
    private String lastReadMsgId = "";
    private boolean isTyping = false;
    private LinearLayout layoutRoot;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // 应用主题背景（如果没有自定义聊天背景的话）
        
        roomId = getIntent().getStringExtra("room_id");
        roomName = getIntent().getStringExtra("room_name");
        isGlobal = getIntent().getBooleanExtra("is_global", false);
        isGroup = getIntent().getBooleanExtra("is_group", false);
        serverBase = SharedPrefs.getServer(this);
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        TextView btnSend = findViewById(R.id.btn_send);
        TextView btnBack = findViewById(R.id.btn_back);
        TextView btnMore = findViewById(R.id.btn_more);
        TextView btnMoreInput = findViewById(R.id.btn_more_input);
        TextView tvTitle = findViewById(R.id.tv_chat_title);
        TextView tvOnline = findViewById(R.id.tv_online_status);
        tvTypingStatus = findViewById(R.id.tv_typing_status);
        layoutGroupAnnouncement = findViewById(R.id.layout_group_announcement);
        tvGroupAnnouncement = findViewById(R.id.tv_group_announcement);
        layoutQuote = findViewById(R.id.layout_quote);
        tvQuoteFrom = findViewById(R.id.tv_quote_from);
        tvQuoteContent = findViewById(R.id.tv_quote_content);
        TextView btnCancelQuote = findViewById(R.id.btn_cancel_quote);
        layoutStickers = findViewById(R.id.layout_stickers);
        layoutMorePanel = findViewById(R.id.layout_more_panel);
        stickerContainer = findViewById(R.id.sticker_container);
        layoutMention = findViewById(R.id.layout_mention);
        mentionContainer = findViewById(R.id.mention_container);
        layoutRoot = findViewById(R.id.layout_chat_root);
        tvTitle.setText(roomName);
        if (isGlobal) {
            tvOnline.setVisibility(View.VISIBLE);
            tvOnline.setText("公共频道");
        } else if (isGroup) {
            tvOnline.setVisibility(View.VISIBLE);
            tvOnline.setText("群聊");
            btnMore.setVisibility(View.VISIBLE);
            // 从 WebSocketManager 获取群公告
            String groupAnn = WebSocketManager.getInstance().getGroupAnnouncement(roomId);
            android.util.Log.d("ChatActivity", "群公告 roomId=" + roomId + " ann=" + groupAnn + " size=" + WebSocketManager.getInstance().groupAnnouncements.size());
            if (groupAnn != null && !groupAnn.isEmpty()) {
                currentGroupAnnouncement = groupAnn;
                showGroupAnnouncementBanner(currentGroupAnnouncement);
            }
        } else {
            // 私聊：显示对方在线状态和自定义状态
            tvOnline.setVisibility(View.VISIBLE);
            com.example.chatapp.model.User peer = WebSocketManager.getInstance().findFriend(roomId);
            if (peer != null) {
                boolean online = WebSocketManager.getInstance().isUserOnline(roomId);
                String statusText = online ? "在线" : "离线";
                if (peer.status != null && !peer.status.isEmpty()) {
                    statusText += " · " + peer.status;
                }
                tvOnline.setText(statusText);
            } else {
                tvOnline.setText("私聊");
            }
        }
        applyTheme();
        loadCustomBackground();
        if (isGlobal) {
            // 公共大厅用 globalRoom，确保历史消息加载
            currentRoom = WebSocketManager.getInstance().globalRoom;
            if (currentRoom == null) {
                currentRoom = new ChatRoom("global", "公共大厅", true);
                WebSocketManager.getInstance().globalRoom = currentRoom;
            }
        } else {
            currentRoom = WebSocketManager.getInstance().chatRooms.get(roomId);
            if (currentRoom == null) {
                currentRoom = new ChatRoom(roomId, roomName, isGlobal || isGroup);
                WebSocketManager.getInstance().chatRooms.put(roomId, currentRoom);
            }
        }
        mergeLocalMessages();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        String myAvatar = getSharedPreferences("chatapp_prefs", 0).getString("avatar", "");
        adapter = new MessageAdapter(currentRoom.messages, isGlobal || isGroup, serverBase, myAvatar, this::onMessageLongPress, this::onAvatarLongPress);
        rvMessages.setAdapter(adapter);
        scrollToBottom();
        // 延迟刷新，等待服务器推送历史消息
        rvMessages.postDelayed(() -> {
            if (adapter != null) adapter.notifyDataSetChanged();
        }, 500);
        rvMessages.postDelayed(() -> {
            if (adapter != null) adapter.notifyDataSetChanged();
        }, 1500);
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        btnCancelQuote.setOnClickListener(v -> clearQuote());
        btnMore.setOnClickListener(v -> showMoreMenu());
        btnMoreInput.setOnClickListener(v -> toggleMorePanel());
        findViewById(R.id.btn_more_sticker).setOnClickListener(v -> { toggleMorePanel(); toggleStickerPanel(); });
        findViewById(R.id.btn_more_image).setOnClickListener(v -> { toggleMorePanel(); pickImage(); });
        findViewById(R.id.btn_more_file).setOnClickListener(v -> { toggleMorePanel(); pickFile(); });
        findViewById(R.id.btn_more_audio).setOnClickListener(v -> { toggleMorePanel(); pickAudio(); });
        View btnVoice = findViewById(R.id.btn_more_voice);
        btnVoice.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 2001);
                    return false;
                }
                startRecording();
                return true;
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (isRecording) stopRecordingAndSend();
                return true;
            } else if (event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                cancelRecording();
                return true;
            }
            return false;
        });
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isGlobal && !isGroup && !isTyping && s.length() > 0) {
                    isTyping = true;
                    WebSocketManager.getInstance().sendTyping(roomId);
                    new android.os.Handler().postDelayed(() -> isTyping = false, 3000);
                }
                // 检测@符号，显示成员选择列表
                String text = s.toString();
                int selStart = etMessage.getSelectionStart();
                if (selStart > 0 && text.charAt(selStart - 1) == '@') {
                    showMentionList();
                } else if (layoutMention != null && layoutMention.getVisibility() == View.VISIBLE) {
                    // 检查光标前是否还有@
                    boolean hasAt = false;
                    for (int i = selStart - 1; i >= 0; i--) {
                        if (text.charAt(i) == '@') { hasAt = true; break; }
                        if (text.charAt(i) == ' ' || text.charAt(i) == 10) break;
                    }
                    if (!hasAt) layoutMention.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
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
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.get("data") instanceof android.graphics.Bitmap) {
                            android.graphics.Bitmap bitmap = (android.graphics.Bitmap) extras.get("data");
                            uploadAndSendBitmap(bitmap);
                        }
                    }
                });
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) uploadAndSendFile(uri);
                    }
                });
        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) uploadAndSendAudio(uri);
                    }
                });
        stickerPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                getContentResolver().takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Exception e) {}
                            android.content.SharedPreferences prefs = getSharedPreferences("custom_stickers", 0);
                            String stickersStr = prefs.getString("stickers", "");
                            org.json.JSONArray arr;
                            try {
                                arr = new org.json.JSONArray(stickersStr);
                            } catch (Exception e) {
                                arr = new org.json.JSONArray();
                            }
                            arr.put(uri.toString());
                            prefs.edit().putString("stickers", arr.toString()).apply();
                            initStickers();
                            Toast.makeText(this, "表情包已添加", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
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
        // 内置 emoji 表情
        for (String emoji : STICKERS) {
            TextView tv = new TextView(this);
            tv.setText(emoji);
            tv.setTextSize(28);
            tv.setPadding(20, 16, 20, 16);
            tv.setOnClickListener(v -> etMessage.getText().insert(etMessage.getSelectionStart(), emoji));
            stickerContainer.addView(tv);
        }
        // 自定义图片表情包
        android.content.SharedPreferences prefs = getSharedPreferences("custom_stickers", 0);
        String stickersStr = prefs.getString("stickers", "");
        if (!stickersStr.isEmpty()) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(stickersStr);
                for (int i = 0; i < arr.length(); i++) {
                    String uriStr = arr.getString(i);
                    android.net.Uri uri = android.net.Uri.parse(uriStr);
                    try {
                        getContentResolver().takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) {}
                    android.widget.ImageView iv = new android.widget.ImageView(this);
                    iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(100, 100));
                    iv.setPadding(8, 8, 8, 8);
                    iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    try {
                        iv.setImageURI(uri);
                    } catch (Exception e) {}
                    iv.setOnClickListener(v -> sendStickerImage(uri));
                    iv.setOnLongClickListener(v -> {
                        // 长按删除自定义表情包
                        new android.app.AlertDialog.Builder(this)
                            .setTitle("删除表情包")
                            .setMessage("确定删除这个表情包吗？")
                            .setPositiveButton("删除", (d, w) -> removeCustomSticker(uriStr))
                            .setNegativeButton("取消", null)
                            .show();
                        return true;
                    });
                    stickerContainer.addView(iv);
                }
            } catch (Exception e) {}
        }
        // 添加按钮
        TextView addBtn = new TextView(this);
        addBtn.setText("➕");
        addBtn.setTextSize(28);
        addBtn.setPadding(20, 16, 20, 16);
        addBtn.setOnClickListener(v -> pickCustomSticker());
        stickerContainer.addView(addBtn);
    }
    private void pickCustomSticker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        stickerPickerLauncher.launch(Intent.createChooser(intent, "选择表情包图片"));
    }
    private void sendStickerImage(android.net.Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
            is.close();
            String base64 = android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP);
            String token = SharedPrefs.getToken(this);
            ApiClient.uploadImage(token, base64, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        String url = result.getString("url");
                        JSONArray imgs = new JSONArray();
                        imgs.put(url);
                        if (isGlobal) {
                            WebSocketManager.getInstance().sendGlobalWithImages("", imgs, null);
                        } else if (isGroup) {
                            WebSocketManager.getInstance().sendGroupWithImages(roomId, "", imgs, null);
                        } else {
                            WebSocketManager.getInstance().sendDmWithImages(roomId, "", imgs, null);
                        }
                    } catch (Exception e) {}
                }
                @Override
                public void onError(String error) {}
            });
        } catch (Exception e) {}
    }
    private void removeCustomSticker(String uriStr) {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("custom_stickers", 0);
            String stickersStr = prefs.getString("stickers", "");
            org.json.JSONArray arr = new org.json.JSONArray(stickersStr);
            org.json.JSONArray newArr = new org.json.JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                if (!arr.getString(i).equals(uriStr)) newArr.put(arr.getString(i));
            }
            prefs.edit().putString("stickers", newArr.toString()).apply();
            initStickers();
            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {}
    }
    private void toggleMorePanel() {
        if (layoutMorePanel != null) {
            layoutMorePanel.setVisibility(layoutMorePanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            if (layoutStickers != null && layoutMorePanel.getVisibility() == View.VISIBLE) {
                layoutStickers.setVisibility(View.GONE);
            }
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
        // 获取用户名：优先用 fromName，其次从好友列表/用户列表查找，最后用 from
        String name = msg.fromName;
        if (name == null || name.isEmpty() || name.startsWith("u_")) {
            com.example.chatapp.model.User u = WebSocketManager.getInstance().getUserById(msg.from);
            if (u != null && u.username != null && !u.username.isEmpty()) {
                name = u.username;
            } else {
                name = msg.from;
            }
        }
        EditText etInput = findViewById(R.id.et_message);
        if (etInput != null) {
            String current = etInput.getText().toString();
            String mention = "@" + name + " ";
            if (!current.contains("@" + name)) {
                etInput.setText(current + mention);
                etInput.setSelection(etInput.getText().length());
            }
            Toast.makeText(this, "已@" + name, Toast.LENGTH_SHORT).show();
        }
    }

    private void showMoreMenu() {
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add("搜索消息");
        if (isGroup) options.add("发布群公告");
        if (isGroup && WebSocketManager.getInstance().isAdmin) options.add("设置成员头衔");
        if (isGroup) options.add("群设置");
        options.add("清空聊天记录");
        options.add("自定义背景");
        String[] opts = options.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("更多")
                .setItems(opts, (dialog, which) -> {
                    String opt = opts[which];
                    if (opt.equals("搜索消息")) showSearchDialog();
                    else if (opt.equals("发布群公告")) showGroupAnnouncementDialog();
                    else if (opt.equals("设置成员头衔")) showSetTitleDialog();
                    else if (opt.equals("群设置")) {
                        Intent intent = new Intent(this, GroupSettingsActivity.class);
                        intent.putExtra("group_id", roomId);
                        startActivity(intent);
                    } else if (opt.equals("清空聊天记录")) {
                        currentRoom.messages.clear();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show();
                    } else if (opt.equals("自定义背景")) {
                        new AlertDialog.Builder(this)
                                .setItems(new String[]{"选择背景图片", "清除背景"}, (d2, w2) -> {
                                    if (w2 == 0) pickBackground();
                                    else clearCustomBackground();
                                })
                                .show();
                    }
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
        String myId = "";
        if (WebSocketManager.getInstance().currentUser != null) {
            myId = WebSocketManager.getInstance().currentUser.id;
        }
        if (myId.isEmpty()) {
            myId = getSharedPreferences("chatapp_prefs", 0).getString("user_id", "");
        }
        boolean isMe = msg.from != null && msg.from.equals(myId);
        java.util.List<String> items = new java.util.ArrayList<>();
        if (msg.content != null && !msg.content.isEmpty()) items.add("复制");
        items.add("转发");
        items.add("引用");
        if (msg.hasAudio()) items.add("转文字");
        if (isMe) items.add("撤回");
        String[] options = items.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    String opt = options[which];
                    if (opt.equals("复制")) {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        cm.setText(msg.content != null ? msg.content : "");
                        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
                    } else if (opt.equals("转发")) forwardMessage(msg);
                    else if (opt.equals("引用")) setQuote(msg);
                    else if (opt.equals("转文字")) startVoiceToText();
                    else if (opt.equals("撤回")) handleRecall(msg);
                })
                .show();
    }
    private void forwardMessage(Message msg) {
        // 收集可转发的目标：好友 + 群聊
        java.util.List<String> names = new java.util.ArrayList<>();
        final java.util.List<String> ids = new java.util.ArrayList<>();
        final java.util.List<Boolean> isGroupList = new java.util.ArrayList<>();
        // 好友
        for (com.example.chatapp.model.User u : WebSocketManager.getInstance().friends) {
            names.add(u.username);
            ids.add(u.id);
            isGroupList.add(false);
        }
        // 群聊
        for (com.example.chatapp.model.Group g : WebSocketManager.getInstance().groups) {
            names.add("[群] " + g.name);
            ids.add(g.id);
            isGroupList.add(true);
        }
        if (names.isEmpty()) {
            Toast.makeText(this, "没有可转发的对象", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("转发给")
                .setItems(names.toArray(new String[0]), (d, which) -> {
                    String targetId = ids.get(which);
                    boolean targetIsGroup = isGroupList.get(which);
                    String text = msg.content != null ? msg.content : "";
                    // 转发图片
                    if (msg.hasImage() && msg.images != null && !msg.images.isEmpty()) {
                        for (String imgUrl : msg.images) {
                            try {
                                java.net.URL url = new java.net.URL(imgUrl.startsWith("http") ? imgUrl : serverBase + imgUrl);
                                java.io.InputStream is = url.openStream();
                                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
                                is.close();
                                String base64 = android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP);
                                org.json.JSONArray imgs = new org.json.JSONArray();
                                imgs.put(base64);
                                if (targetIsGroup) WebSocketManager.getInstance().sendGroupWithImages(targetId, text, imgs);
                                else WebSocketManager.getInstance().sendDmWithImages(targetId, text, imgs);
                            } catch (Exception e) {
                                Toast.makeText(this, "转发图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        // 转发文字
                        if (targetIsGroup) WebSocketManager.getInstance().sendGroup(targetId, text, null);
                        else WebSocketManager.getInstance().sendDm(targetId, text, null);
                    }
                    Toast.makeText(this, "已转发", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    private void showSearchDialog() {
        final EditText etSearch = new EditText(this);
        etSearch.setHint("输入关键词搜索消息");
        new AlertDialog.Builder(this)
                .setTitle("搜索消息")
                .setView(etSearch)
                .setPositiveButton("搜索", (d, w) -> {
                    String keyword = etSearch.getText().toString().trim().toLowerCase();
                    if (keyword.isEmpty()) return;
                    final java.util.List<Message> results = new java.util.ArrayList<>();
                    for (Message m : currentRoom.messages) {
                        if (m.content != null && m.content.toLowerCase().contains(keyword)) {
                            results.add(m);
                        }
                    }
                    if (results.isEmpty()) {
                        Toast.makeText(this, "未找到相关消息", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] items = new String[results.size()];
                    for (int i = 0; i < results.size(); i++) {
                        Message m = results.get(i);
                        String name = m.fromName != null && !m.fromName.isEmpty() ? m.fromName : m.from;
                        String preview = m.content != null && m.content.length() > 30 ? m.content.substring(0, 30) + "..." : (m.content != null ? m.content : "");
                        items[i] = name + ": " + preview;
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("搜索结果 (" + results.size() + "条)")
                            .setItems(items, (d2, w2) -> {
                                // 跳转到该消息位置
                                int pos = currentRoom.messages.indexOf(results.get(w2));
                                if (pos >= 0) rvMessages.scrollToPosition(pos);
                            })
                            .show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void showSetTitleDialog() {
        // 获取群成员列表
        final java.util.List<String> memberNames = new java.util.ArrayList<>();
        final java.util.List<String> memberIds = new java.util.ArrayList<>();
        com.example.chatapp.model.Group targetGroup = null;
        for (com.example.chatapp.model.Group g : WebSocketManager.getInstance().groups) {
            if (g.id.equals(roomId)) { targetGroup = g; break; }
        }
        if (targetGroup != null && targetGroup.members != null) {
            for (String uid : targetGroup.members) {
                    com.example.chatapp.model.User u = WebSocketManager.getInstance().findFriend(uid);
                    if (u == null && WebSocketManager.getInstance().currentUser != null && uid.equals(WebSocketManager.getInstance().currentUser.id)) {
                        u = WebSocketManager.getInstance().currentUser;
                    }
                    if (u != null) {
                        memberNames.add(u.username + (u.title != null && !u.title.isEmpty() ? " (" + u.title + ")" : ""));
                        memberIds.add(uid);
                    }
                }
            }
        if (memberNames.isEmpty()) {
            String debug = "groups数量: " + WebSocketManager.getInstance().groups.size();
            if (targetGroup == null) debug += ", 未找到该群";
            else if (targetGroup.members == null) debug += ", members为null";
            else debug += ", members数量: " + targetGroup.members.size();
            Toast.makeText(this, "暂无成员(" + debug + ")", Toast.LENGTH_LONG).show();
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择成员")
            .setItems(memberNames.toArray(new String[0]), (d, which) -> {
                String selectedId = memberIds.get(which);
                com.example.chatapp.model.User selectedUser = null;
                for (com.example.chatapp.model.User u : WebSocketManager.getInstance().friends) {
                    if (u.id.equals(selectedId)) { selectedUser = u; break; }
                }
                if (selectedUser == null && WebSocketManager.getInstance().currentUser != null && selectedId.equals(WebSocketManager.getInstance().currentUser.id)) {
                    selectedUser = WebSocketManager.getInstance().currentUser;
                }
                final String currentTitle = selectedUser != null && selectedUser.title != null ? selectedUser.title : "";
                final android.widget.EditText etTitle = new android.widget.EditText(this);
                etTitle.setText(currentTitle);
                etTitle.setHint("输入头衔（留空清除）");
                etTitle.setMaxLines(1);
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("设置头衔")
                    .setView(etTitle)
                    .setPositiveButton("确定", (d2, w) -> {
                        String title = etTitle.getText().toString().trim();
                        String token = com.example.chatapp.util.SharedPrefs.getToken(this);
                        android.util.Log.d("ChatActivity", "设置头衔 token=" + (token != null && !token.isEmpty() ? "有" : "无") + " userId=" + selectedId + " title=" + title);
                        com.example.chatapp.api.ApiClient.setTitle(token, selectedId, title, new com.example.chatapp.api.ApiClient.Callback() {
                            @Override
                            public void onSuccess(org.json.JSONObject result) {
                                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "头衔设置成功", Toast.LENGTH_SHORT).show());
                            }
                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "头衔设置失败: " + error, Toast.LENGTH_LONG).show());
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    private void showGroupAnnouncementDialog() {
        final EditText etAnn = new EditText(this);
        etAnn.setHint("输入群公告内容");
        etAnn.setMinLines(2);
        new AlertDialog.Builder(this)
                .setTitle("发布群公告")
                .setView(etAnn)
                .setPositiveButton("发布", (d, w) -> {
                    String text = etAnn.getText().toString().trim();
                    if (!text.isEmpty()) {
                        WebSocketManager.getInstance().sendGroupAnnouncement(roomId, text);
                        Toast.makeText(this, "公告已发布", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private android.speech.SpeechRecognizer voiceToTextRecognizer;
    private void startVoiceToText() {
        try {
            if (android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
                if (voiceToTextRecognizer == null) {
                    voiceToTextRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
                    voiceToTextRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
                        @Override public void onReadyForSpeech(android.os.Bundle params) {
                            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "请说话...", Toast.LENGTH_SHORT).show());
                        }
                        @Override public void onBeginningOfSpeech() {}
                        @Override public void onRmsChanged(float rmsdB) {}
                        @Override public void onBufferReceived(byte[] buffer) {}
                        @Override public void onEndOfSpeech() {}
                        @Override public void onError(int error) {
                            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "识别失败，请重试", Toast.LENGTH_SHORT).show());
                        }
                        @Override public void onResults(android.os.Bundle results) {
                            java.util.ArrayList<String> matches = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                            if (matches != null && !matches.isEmpty()) {
                                String text = matches.get(0);
                                EditText etMessage = findViewById(R.id.et_message);
                                etMessage.setText(text);
                                etMessage.setSelection(etMessage.getText().length());
                                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "识别结果已填入输入框", Toast.LENGTH_SHORT).show());
                            }
                        }
                        @Override public void onPartialResults(android.os.Bundle partialResults) {}
                        @Override public void onEvent(int eventType, android.os.Bundle params) {}
                    });
                }
                Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
                voiceToTextRecognizer.startListening(intent);
            } else {
                Toast.makeText(this, "语音识别不可用", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "语音转文字失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
    private void scrollToBottom() {
        if (adapter != null && adapter.getItemCount() > 0) {
            rvMessages.post(() -> rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1));
        }
    }
    private void sendMessage() {
        try {
            String content = etMessage.getText().toString().trim();
            if (content.isEmpty() && quotedMessage == null) return;
            if (!WebSocketManager.getInstance().isConnected()) {
                Toast.makeText(this, "连接中，请稍候...", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentRoom == null) {
                Toast.makeText(this, "房间未初始化", Toast.LENGTH_SHORT).show();
                return;
            }
            JSONObject quote = buildQuote();
            // 本地立即显示消息
            Message localMsg = new Message();
            localMsg.id = "local_" + System.currentTimeMillis();
            localMsg.content = content;
            localMsg.time = System.currentTimeMillis();
            if (WebSocketManager.getInstance().currentUser != null) {
                localMsg.from = WebSocketManager.getInstance().currentUser.id;
                localMsg.fromName = WebSocketManager.getInstance().currentUser.username;
                localMsg.fromAvatar = WebSocketManager.getInstance().currentUser.avatar;
            }
            if (quotedMessage != null) {
                localMsg.quoteMsgId = quotedMessage.id;
                localMsg.quoteContent = quotedMessage.content;
                localMsg.quoteFrom = quotedMessage.from;
                localMsg.quoteFromName = quotedMessage.fromName;
            }
            currentRoom.messages.add(localMsg);
            if (adapter != null) {
                adapter.notifyItemInserted(currentRoom.messages.size() - 1);
                scrollToBottom();
            }
            if (isGlobal) {
                WebSocketManager.getInstance().sendGlobal(content, quote);
            } else if (isGroup) {
                WebSocketManager.getInstance().sendGroup(roomId, content, quote);
            } else {
                WebSocketManager.getInstance().sendDm(roomId, content, quote);
            }
            etMessage.setText("");
            clearQuote();
        } catch (Exception e) {
            Toast.makeText(this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void showMentionList() {
        if (!isGroup) return;
        try {
            // 从群对象获取成员ID列表
            com.example.chatapp.model.Group group = null;
            for (com.example.chatapp.model.Group g : WebSocketManager.getInstance().groups) {
                if (g.id != null && g.id.equals(roomId)) {
                    group = g;
                    break;
                }
            }
            if (group == null || group.members == null || group.members.isEmpty()) return;
            // 根据成员ID查找用户名
            java.util.List<String> names = new java.util.ArrayList<>();
            for (String uid : group.members) {
                com.example.chatapp.model.User u = WebSocketManager.getInstance().findFriend(uid);
                if (u != null) names.add(u.username);
                else names.add(uid);
            }
            if (names.isEmpty()) return;
            String[] nameArray = names.toArray(new String[0]);
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("选择要@的成员")
                .setItems(nameArray, (d, which) -> {
                    String mention = "@" + nameArray[which] + " ";
                    int start = etMessage.getSelectionStart();
                    etMessage.getText().insert(start, mention);
                })
                .show();
        } catch (Exception e) {}
    }
    private void handleRecall(Message msg) {
        if (msg == null) return;
        String myId = WebSocketManager.getInstance().currentUser != null ? WebSocketManager.getInstance().currentUser.id : "";
        if (!msg.from.equals(myId)) {
            Toast.makeText(this, "只能撤回自己的消息", Toast.LENGTH_SHORT).show();
            return;
        }
        // 本地乐观更新
        msg.recalled = true;
        if (adapter != null) adapter.notifyDataSetChanged();
        // 发送撤回请求
        WebSocketManager.getInstance().recallMessage(msg.id, roomId);
    }
    private void uploadAndSendBitmap(android.graphics.Bitmap bitmap) {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, bos);
            String base64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            String token = SharedPrefs.getToken(this);
            String textContent = etMessage.getText().toString().trim();
            JSONObject quote = buildQuote();
            ApiClient.uploadImage(token, base64, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        String imageUrl = result.getString("url");
                        if (isGlobal) {
                            WebSocketManager.getInstance().sendGlobalWithImages(textContent, new org.json.JSONArray().put(imageUrl), quote);
                        } else if (isGroup) {
                            WebSocketManager.getInstance().sendGroupWithImages(roomId, textContent, new org.json.JSONArray().put(imageUrl), quote);
                        } else {
                            WebSocketManager.getInstance().sendDmWithImages(roomId, textContent, new org.json.JSONArray().put(imageUrl), quote);
                        }
                        etMessage.setText("");
                        clearQuote();
                    } catch (Exception e) {
                        Toast.makeText(ChatActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(ChatActivity.this, "上传失败: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void pickAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", "audio/aac", "audio/flac", "audio/x-m4a"
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        audioPickerLauncher.launch(Intent.createChooser(intent, "选择音频"));
    }
    private void uploadAndSendAudio(android.net.Uri uri) {
        try {
            String filename = "audio.mp3";
            try {
                android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) filename = cursor.getString(nameIndex);
                    cursor.close();
                }
            } catch (Exception e) {}
            if (filename == null || filename.isEmpty()) filename = "audio.mp3";
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                Toast.makeText(this, "无法读取音频文件", Toast.LENGTH_SHORT).show();
                return;
            }
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
            is.close();
            byte[] audioBytes = bos.toByteArray();
            if (audioBytes.length > 30 * 1024 * 1024) {
                Toast.makeText(this, "音频不能超过 30MB", Toast.LENGTH_SHORT).show();
                return;
            }
            if (audioBytes.length == 0) {
                Toast.makeText(this, "音频文件为空", Toast.LENGTH_SHORT).show();
                return;
            }
            final String finalFilename = filename;
            final byte[] finalBytes = audioBytes;
            String base64 = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP);
            String token = SharedPrefs.getToken(this);
            final android.app.ProgressDialog audioProgress = new android.app.ProgressDialog(this);
            audioProgress.setMessage("正在上传音频 0%");
            audioProgress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
            audioProgress.setMax(100);
            audioProgress.setCancelable(false);
            audioProgress.show();
            uploadFileByHttp(audioBytes, finalFilename, "audio", audioProgress, result -> {
                audioProgress.dismiss();
                try {
                    String audioUrl = result.getString("url");
                    String aname = result.optString("filename", finalFilename);
                    long asize = result.optLong("size", finalBytes.length);
                    JSONObject audioMsg = new JSONObject();
                    audioMsg.put("audioUrl", audioUrl);
                    audioMsg.put("audioName", aname);
                    audioMsg.put("audioSize", asize);
                    if (isGlobal) {
                        WebSocketManager.getInstance().sendGlobal(audioMsg.toString(), null);
                    } else if (isGroup) {
                        WebSocketManager.getInstance().sendGroup(roomId, audioMsg.toString(), null);
                    } else {
                        WebSocketManager.getInstance().sendDm(roomId, audioMsg.toString(), null);
                    }
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "音频发送成功", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "处理音频失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "application/zip", "application/x-rar-compressed"
        });
        filePickerLauncher.launch(Intent.createChooser(intent, "选择文件"));
    }
    private android.media.MediaRecorder mediaRecorder;
    private String voiceFilePath;
    private boolean isRecording = false;
    private long recordStartTime;
    private void startVoiceInput() {
        // 检查权限
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 2001);
            Toast.makeText(this, "请先授予录音权限", Toast.LENGTH_SHORT).show();
            return;
        }
        // 显示按住说话提示
        Toast.makeText(this, "按住语音按钮说话，松开发送", Toast.LENGTH_LONG).show();
    }
    private void startRecording() {
        try {
            voiceFilePath = getExternalCacheDir().getAbsolutePath() + "/voice_" + System.currentTimeMillis() + ".mp3";
            mediaRecorder = new android.media.MediaRecorder();
            mediaRecorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(voiceFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordStartTime = System.currentTimeMillis();
            Toast.makeText(this, "正在录音...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void stopRecordingAndSend() {
        if (!isRecording || mediaRecorder == null) return;
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            long duration = System.currentTimeMillis() - recordStartTime;
            if (duration < 1000) {
                Toast.makeText(this, "说话时间太短", Toast.LENGTH_SHORT).show();
                new java.io.File(voiceFilePath).delete();
                return;
            }
            // 上传语音文件
            java.io.File voiceFile = new java.io.File(voiceFilePath);
            if (!voiceFile.exists()) return;
            java.io.FileInputStream fis = new java.io.FileInputStream(voiceFile);
            byte[] buffer = new byte[8192];
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            int len;
            while ((len = fis.read(buffer)) != -1) bos.write(buffer, 0, len);
            fis.close();
            byte[] voiceBytes = bos.toByteArray();
            // 使用 WebSocket 分块上传语音
            final android.app.ProgressDialog voiceProgress = new android.app.ProgressDialog(this);
            voiceProgress.setMessage("正在发送语音 0%");
            voiceProgress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
            voiceProgress.setMax(100);
            voiceProgress.setCancelable(false);
            voiceProgress.show();
            uploadFileByHttp(voiceBytes, "voice.mp3", "audio", voiceProgress, result -> {
                voiceProgress.dismiss();
                try {
                    String audioUrl = result.getString("url");
                    JSONObject audioMsg = new JSONObject();
                    audioMsg.put("audioUrl", audioUrl);
                    audioMsg.put("audioName", "语音消息");
                    audioMsg.put("audioSize", voiceBytes.length);
                    audioMsg.put("audioDuration", duration);
                    if (isGlobal) {
                        WebSocketManager.getInstance().sendGlobal(audioMsg.toString(), null);
                    } else if (isGroup) {
                        WebSocketManager.getInstance().sendGroup(roomId, audioMsg.toString(), null);
                    } else {
                        WebSocketManager.getInstance().sendDm(roomId, audioMsg.toString(), null);
                    }
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "语音发送成功", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
            voiceFile.delete();
        } catch (Exception e) {
            Toast.makeText(this, "停止录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isRecording = false;
        }
    }
    private void cancelRecording() {
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {}
            mediaRecorder = null;
            isRecording = false;
            new java.io.File(voiceFilePath).delete();
            Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show();
        }
    }
    // 通用分块上传方法（通过 WebSocket，绕过 Cloudflare 524 超时）
    private final java.util.Set<Integer> ackedChunks = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    private void uploadFileByHttp(byte[] fileBytes, String filename, String fileType, android.app.ProgressDialog progress, java.util.function.Consumer<org.json.JSONObject> onSuccess) {
        final String fileId = java.util.UUID.randomUUID().toString();
        final int chunkSize = 1024 * 1024; // 1MB一块
        final int totalChunks = (int) Math.ceil((double) fileBytes.length / chunkSize);
        final String serverBase = "https://buer.kdns.fr";
        new Thread(() -> {
            try {
                for (int i = 0; i < totalChunks; i++) {
                    int start = i * chunkSize;
                    int end = Math.min(start + chunkSize, fileBytes.length);
                    byte[] chunk = new byte[end - start];
                    System.arraycopy(fileBytes, start, chunk, 0, chunk.length);
                    // 上传这块，最多重发3次
                    boolean uploaded = false;
                    for (int retry = 0; retry < 3 && !uploaded; retry++) {
                        try {
                            java.net.URL url = new java.net.URL(serverBase + "/api/upload_chunk?fileId=" + fileId + "&chunkIndex=" + i);
                            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type", "application/octet-stream");
                            conn.setDoOutput(true);
                            conn.setConnectTimeout(30000);
                            conn.setReadTimeout(60000);
                            try (java.io.OutputStream os = conn.getOutputStream()) {
                                os.write(chunk);
                                os.flush();
                            }
                            int code = conn.getResponseCode();
                            if (code >= 200 && code < 300) {
                                uploaded = true;
                            }
                            conn.disconnect();
                        } catch (Exception e) {
                            if (retry == 2) throw e;
                            Thread.sleep(1000);
                        }
                    }
                    if (!uploaded) throw new Exception("块 " + (i+1) + "/" + totalChunks + " 上传失败");
                    final int percent = (int) (((i + 1) * 100L) / totalChunks);
                    runOnUiThread(() -> {
                        if (progress != null) {
                            progress.setMessage("正在上传 " + percent + "%");
                            progress.setProgress(percent);
                        }
                    });
                }
                // 所有块上传完成，请求合并
                org.json.JSONObject mergeBody = new org.json.JSONObject();
                mergeBody.put("fileId", fileId);
                mergeBody.put("totalChunks", totalChunks);
                mergeBody.put("filename", filename);
                mergeBody.put("fileType", fileType);
                java.net.URL mergeUrl = new java.net.URL(serverBase + "/api/merge_chunks");
                java.net.HttpURLConnection mergeConn = (java.net.HttpURLConnection) mergeUrl.openConnection();
                mergeConn.setRequestMethod("POST");
                mergeConn.setRequestProperty("Content-Type", "application/json");
                mergeConn.setDoOutput(true);
                mergeConn.setConnectTimeout(30000);
                mergeConn.setReadTimeout(60000);
                try (java.io.OutputStream os = mergeConn.getOutputStream()) {
                    os.write(mergeBody.toString().getBytes("UTF-8"));
                    os.flush();
                }
                int mergeCode = mergeConn.getResponseCode();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(mergeCode >= 400 ? mergeConn.getErrorStream() : mergeConn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                mergeConn.disconnect();
                if (mergeCode < 200 || mergeCode >= 300) {
                    throw new Exception("合并失败: " + sb.toString());
                }
                org.json.JSONObject result = new org.json.JSONObject(sb.toString());
                runOnUiThread(() -> {
                    if (progress != null) progress.dismiss();
                    onSuccess.accept(result);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (progress != null && !isFinishing()) progress.dismiss();
                    if (!isFinishing()) {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("上传失败")
                            .setMessage(e.getMessage())
                            .setPositiveButton("确定", null)
                            .show();
                    }
                });
            }
        }).start();
    }
    private void uploadAndSendFile(android.net.Uri uri) {
        try {
            final String[] filenameHolder = {"file"};
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) filenameHolder[0] = cursor.getString(nameIndex);
                cursor.close();
            }
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
            is.close();
            byte[] fileBytes = bos.toByteArray();
            if (fileBytes.length > 30 * 1024 * 1024) {
                Toast.makeText(this, "文件不能超过 10MB", Toast.LENGTH_SHORT).show();
                return;
            }
            final String finalFilename = filenameHolder[0];
            String base64 = android.util.Base64.encodeToString(fileBytes, android.util.Base64.NO_WRAP);
            String token = SharedPrefs.getToken(this);
            final android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
            progressDialog.setMessage("正在上传文件 0%");
            progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.show();
            uploadFileByHttp(fileBytes, finalFilename, "file", progressDialog, result -> {
                progressDialog.dismiss();
                try {
                    String fileUrl = result.getString("url");
                    String fname = result.optString("filename", finalFilename);
                    long fsize = result.optLong("size", fileBytes.length);
                    JSONObject fileMsg = new JSONObject();
                    fileMsg.put("fileUrl", fileUrl);
                    fileMsg.put("fileName", fname);
                    fileMsg.put("fileSize", fsize);
                    if (isGlobal) {
                        WebSocketManager.getInstance().sendGlobal(fileMsg.toString(), null);
                    } else if (isGroup) {
                        WebSocketManager.getInstance().sendGroup(roomId, fileMsg.toString(), null);
                    } else {
                        WebSocketManager.getInstance().sendDm(roomId, fileMsg.toString(), null);
                    }
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "文件发送成功", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "处理文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchCamera() {
        try {
            Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(takePictureIntent);
            } else {
                Toast.makeText(this, "无法启动相机", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "相机启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void pickImage() {
        String[] options = {"拍照", "从相册选择"};
        new AlertDialog.Builder(this)
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        // 拍照 - 先检查权限
                        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                        } else {
                            launchCamera();
                        }
                    } else {
                        // 从相册选择（支持GIF）
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"});
                        imagePickerLauncher.launch(Intent.createChooser(intent, "选择图片"));
                    }
                })
                .show();
    }
    private void uploadAndSendImage(Uri uri) {
        try {
            // 先解码图片尺寸
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream is = getContentResolver().openInputStream(uri);
            android.graphics.BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();
            
            // 计算采样率，限制最大尺寸为1280px
            int maxDim = 1280;
            int sampleSize = 1;
            if (options.outWidth > maxDim || options.outHeight > maxDim) {
                int halfWidth = options.outWidth / 2;
                int halfHeight = options.outHeight / 2;
                while ((halfWidth / sampleSize) >= maxDim && (halfHeight / sampleSize) >= maxDim) {
                    sampleSize *= 2;
                }
            }
            
            // 解码压缩后的图片
            android.graphics.BitmapFactory.Options decodeOptions = new android.graphics.BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;
            is = getContentResolver().openInputStream(uri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is, null, decodeOptions);
            if (is != null) is.close();
            
            if (bitmap == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 压缩为JPEG，质量80%
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] imgBytes = bos.toByteArray();
            bitmap.recycle();
            
            if (imgBytes.length > 5 * 1024 * 1024) {
                Toast.makeText(this, "图片压缩后仍超过 5MB", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String base64 = Base64.encodeToString(imgBytes, Base64.NO_WRAP);
            String token = SharedPrefs.getToken(this);
            String textContent = etMessage.getText().toString().trim();
            JSONObject quote = buildQuote();
            
            final android.app.ProgressDialog imgProgress = new android.app.ProgressDialog(this);
            imgProgress.setMessage("正在上传图片...");
            imgProgress.setCancelable(false);
            imgProgress.show();
            
            ApiClient.uploadImage(token, base64, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    runOnUiThread(() -> {
                        imgProgress.dismiss();
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
                            etMessage.setText("");
                            clearQuote();
                        } catch (Exception e) {
                            Toast.makeText(ChatActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        imgProgress.dismiss();
                        Toast.makeText(ChatActivity.this, "上传失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        WebSocketManager.getInstance().addListener(this);
        WebSocketManager.getInstance().setCurrentVisibleRoom(roomId);
        // 刷新消息列表（确保历史消息和新消息都显示）
        if (adapter != null && currentRoom != null) {
            adapter.notifyDataSetChanged();
            scrollToBottom();
        }
        // 发送已读回执（只在对方发的消息时发送）
        if (!isGlobal && !isGroup && currentRoom != null && !currentRoom.messages.isEmpty()) {
            String myId = WebSocketManager.getInstance().currentUser != null ? WebSocketManager.getInstance().currentUser.id : "";
            for (int i = currentRoom.messages.size() - 1; i >= 0; i--) {
                Message lastMsg = currentRoom.messages.get(i);
                if (lastMsg != null && lastMsg.id != null && !lastMsg.id.startsWith("local_") &&
                    lastMsg.from != null && !lastMsg.from.equals(myId) &&
                    !lastMsg.id.equals(lastReadMsgId)) {
                    lastReadMsgId = lastMsg.id;
                    WebSocketManager.getInstance().sendRead(roomId, lastMsg.id);
                    break;
                }
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        // 退出对话时停止音频播放
        com.example.chatapp.adapter.MessageAdapter.stopAudioPlayback();
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
        if (msgRoomId != null && msgRoomId.equals(roomId) && currentRoom != null && adapter != null) {
            runOnUiThread(() -> {
                try {
                    String myId = WebSocketManager.getInstance().currentUser != null ? WebSocketManager.getInstance().currentUser.id : "";
                    
                    // 1. 如果是自己发的消息，尝试替换本地 local_ 消息
                    boolean replaced = false;
                    if (msg.from != null && msg.from.equals(myId)) {
                        for (int i = currentRoom.messages.size() - 1; i >= 0; i--) {
                            Message old = currentRoom.messages.get(i);
                            if (old.id != null && old.id.startsWith("local_") &&
                                old.content != null && old.content.equals(msg.content) &&
                                old.from != null && old.from.equals(msg.from)) {
                                // 保留引用信息
                                if (old.hasQuote() && !msg.hasQuote()) {
                                    msg.quoteMsgId = old.quoteMsgId;
                                    msg.quoteContent = old.quoteContent;
                                    msg.quoteFrom = old.quoteFrom;
                                    msg.quoteFromName = old.quoteFromName;
                                }
                                currentRoom.messages.set(i, msg);
                                adapter.notifyItemChanged(i);
                                replaced = true;
                                break;
                            }
                        }
                    }
                    
                    // 2. 检查是否已经存在相同ID的消息
                    if (!replaced && msg.id != null && !msg.id.startsWith("local_")) {
                        for (int i = 0; i < currentRoom.messages.size(); i++) {
                            Message existing = currentRoom.messages.get(i);
                            if (existing.id != null && existing.id.equals(msg.id)) {
                                replaced = true;
                                break;
                            }
                        }
                    }
                    
                    // 3. 不重复才添加
                    if (!replaced) {
                        currentRoom.messages.add(msg);
                        adapter.notifyItemInserted(currentRoom.messages.size() - 1);
                    }
                    
                    // 4. 记录已处理消息ID
                    if (msg.id != null && !msg.id.startsWith("local_")) {
                        processedMsgIds.add(msg.id);
                    }
                    
                    scrollToBottom();
                } catch (Exception e) {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }
    @Override
    public void onMessageRecalled(String msgId, String recalledRoomId) {
        if (recalledRoomId != null && recalledRoomId.equals(roomId) && adapter != null) {
            runOnUiThread(() -> {
                try { adapter.notifyDataSetChanged(); } catch (Exception e) {}
            });
        }
    }
    @Override
    public void onAvatarUpdate(String userId, String avatar) {
        runOnUiThread(() -> {
            try {
                // 如果是自己的头像更新，更新 adapter 中的 myAvatar
                String myId = getSharedPreferences("chatapp_prefs", 0).getString("user_id", "");
                if (userId != null && userId.equals(myId)) {
                    getSharedPreferences("chatapp_prefs", 0).edit().putString("avatar", avatar).apply();
                    adapter.updateMyAvatar(avatar);
                } else {
                    adapter.notifyDataSetChanged();
                }
            } catch (Exception e) {}
        });
    }

    @Override
    public void onTyping(String fromUid) {
        runOnUiThread(() -> {
            if (tvTypingStatus != null && fromUid != null && fromUid.equals(roomId)) {
                tvTypingStatus.setText("对方正在输入...");
                tvTypingStatus.setVisibility(View.VISIBLE);
                new android.os.Handler().postDelayed(() -> {
                    if (tvTypingStatus != null) tvTypingStatus.setVisibility(View.GONE);
                }, 3000);
            }
        });
    }
    @Override
    public void onMessageRead(String fromUid, String msgId) {
        runOnUiThread(() -> {
            if (fromUid != null && fromUid.equals(roomId) && adapter != null && currentRoom != null) {
                // 查找消息的时间戳
                long msgTime = 0;
                for (com.example.chatapp.model.Message m : currentRoom.messages) {
                    if (m.id != null && m.id.equals(msgId)) {
                        msgTime = m.time;
                        break;
                    }
                }
                if (msgTime > 0) {
                    adapter.setLastReadInfo(msgId, msgTime);
                } else {
                    adapter.setLastReadMsgId(msgId);
                }
            }
        });
    }
    private void showGroupAnnouncementBanner(String text) {
        // 以弹窗形式呈现群公告
        if (text == null || text.isEmpty()) return;
        // 避免重复弹窗
        if (announcementDialog != null && announcementDialog.isShowing()) return;
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("📢 群公告");
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(0xFF333333);
        title.setPadding(0, 0, 0, 24);
        layout.addView(title);
        
        android.widget.TextView contentTv = new android.widget.TextView(this);
        contentTv.setText(text);
        contentTv.setTextSize(15);
        contentTv.setTextColor(0xFF555555);
        contentTv.setLineSpacing(1.2f, 1.2f);
        layout.addView(contentTv);
        
        scrollView.addView(layout);
        
        announcementDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(scrollView)
                .setPositiveButton("我知道了", (d, w) -> {
                    // 同时显示横幅
                    if (layoutGroupAnnouncement != null && tvGroupAnnouncement != null) {
                        tvGroupAnnouncement.setText(text);
                        layoutGroupAnnouncement.setVisibility(View.VISIBLE);
                    }
                })
                .setCancelable(true)
                .show();
    }
    @Override
    public void onTitleUpdate(String userId, String title) {
        // 更新消息列表中的用户头衔
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    @Override
    public void onStatusUpdate(String userId, String status) {
        runOnUiThread(() -> {
            // 更新好友列表中的状态
            for (com.example.chatapp.model.User u : WebSocketManager.getInstance().friends) {
                if (u.id.equals(userId)) { u.status = status; break; }
            }
            // 如果是当前聊天对象，更新在线状态显示
            if (!isGlobal && !isGroup && userId.equals(roomId)) {
                TextView tvOnline = findViewById(R.id.tv_online_status);
                if (tvOnline != null) {
                    com.example.chatapp.model.User peer = WebSocketManager.getInstance().findFriend(roomId);
                    if (peer != null) {
                        boolean online = WebSocketManager.getInstance().isUserOnline(roomId);
                        String statusText = online ? "在线" : "离线";
                        if (peer.status != null && !peer.status.isEmpty()) {
                            statusText += " · " + peer.status;
                        }
                        tvOnline.setText(statusText);
                    }
                }
            }
        });
    }
    @Override
    public void onPresenceUpdate() {
        runOnUiThread(() -> {
            if (!isGlobal && !isGroup) {
                TextView tvOnline = findViewById(R.id.tv_online_status);
                if (tvOnline != null) {
                    com.example.chatapp.model.User peer = WebSocketManager.getInstance().findFriend(roomId);
                    if (peer != null) {
                        boolean online = WebSocketManager.getInstance().isUserOnline(roomId);
                        String statusText = online ? "在线" : "离线";
                        if (peer.status != null && !peer.status.isEmpty()) {
                            statusText += " · " + peer.status;
                        }
                        tvOnline.setText(statusText);
                    }
                }
            }
        });
    }
    @Override
    public void onFileUploadComplete(String fileId, String url, String filename, long size) {
        runOnUiThread(() -> {
            java.util.function.Consumer<org.json.JSONObject> cb = pendingUploads.remove(fileId);
            if (cb != null) {
                try {
                    org.json.JSONObject result = new org.json.JSONObject();
                    result.put("url", url);
                    result.put("filename", filename);
                    result.put("size", size);
                    cb.accept(result);
                } catch (Exception e) {
                    Toast.makeText(this, "上传结果处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    @Override
    public void onFileUploadError(String fileId, String error) {
        runOnUiThread(() -> {
            pendingUploads.remove(fileId);
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("上传失败")
                .setMessage(error)
                .setPositiveButton("确定", null)
                .show();
        });
    }
    @Override
    public void onFileChunkAck(String fileId, int chunkIndex) {
        ackedChunks.add(chunkIndex);
        synchronized (chunkAckLock) {
            chunkAckReceived = true;
            chunkAckLock.notifyAll();
        }
    }
    public void onGroupAnnouncement(String gid, String text, long time) {
        runOnUiThread(() -> {
            android.util.Log.d("ChatActivity", "收到群公告 gid=" + gid + " roomId=" + roomId + " isGroup=" + isGroup + " text=" + text);
            if (isGroup && gid != null && gid.equals(roomId)) {
                currentGroupAnnouncement = text;
                showGroupAnnouncementBanner(text);
                Toast.makeText(this, "群公告: " + text, Toast.LENGTH_LONG).show();
            }
        });
    }
    @Override
    public void onMomentsUpdated() {}
    @Override
    public void onFriendListUpdated() {
        runOnUiThread(() -> {
            try { if (adapter != null) adapter.notifyDataSetChanged(); } catch (Exception e) {}
        });
    }

    @Override
    public void onMomentNotify(String action, String fromName, String momentText, String commentText) {}
    @Override
    public void onFriendRequestReceived() {}
    @Override
    public void onFriendRequestResult(boolean ok, String error) {}
}
