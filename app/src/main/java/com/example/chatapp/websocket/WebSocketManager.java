package com.example.chatapp.websocket;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.chatapp.MainActivity;
import com.example.chatapp.R;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.Group;
import com.example.chatapp.model.Message;
import com.example.chatapp.model.Moment;
import com.example.chatapp.model.User;
import com.example.chatapp.util.MessageStore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
public class WebSocketManager {
    private static final String TAG = "WS";
    private static final String CHANNEL_ID = "chatapp_messages";
    private static final String CHANNEL_NAME = "消息通知";
    private static WebSocketManager instance;
    private static Context appContext;
    private WebSocket webSocket;
    private boolean isConnected = false;
    private boolean isConnecting = false;
    private int reconnectAttempts = 0;
    private android.os.Handler reconnectHandler = new android.os.Handler();
    private String serverBase = "https://buer.kdns.fr";
    private String token;
    public User currentUser;
    public List<User> friends = new ArrayList<>();
    public List<Group> groups = new ArrayList<>();
    public boolean isAdmin = false;
    private Set<String> onlineUsers = new HashSet<>();
    private int notificationId = 1;
    // 好友请求列表
    public List<FriendRequest> friendRequests = new ArrayList<>();
    public static class FriendRequest {
        public String id;
        public String from;
        public String fromName;
        public String fromAvatar;
        public long time;
        public static FriendRequest fromJson(JSONObject obj) {
            FriendRequest r = new FriendRequest();
            r.id = obj.optString("id", "");
            r.from = obj.optString("from", "");
            r.fromName = obj.optString("fromName", "");
            r.fromAvatar = obj.optString("fromAvatar", "");
            r.time = obj.optLong("time", 0);
            return r;
        }
    }
    public static void setAppContext(Context ctx) {
        appContext = ctx.getApplicationContext();
        createNotificationChannel();
    }
    private static void createNotificationChannel() {
        if (appContext == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = appContext.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("聊天消息提醒");
                channel.enableVibration(true);
                nm.createNotificationChannel(channel);
            }
        }
    }
    private void showNotification(String title, String content, String roomId) {
        if (appContext == null) return;
        try {
            Intent intent = new Intent(appContext, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pi = PendingIntent.getActivity(appContext, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pi);
            NotificationManager nm = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(notificationId++, builder.build());
                if (notificationId > 100) notificationId = 1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Show notification error", e);
        }
    }
    private String getFriendName(String uid) {
        for (User u : friends) {
            if (u.id.equals(uid)) return u.username;
        }
        return uid;
    }
    public String getGroupAnnouncement(String gid) {
        return groupAnnouncements.get(gid);
    }
    public User findFriend(String uid) {
        if (uid == null) return null;
        for (User u : friends) {
            if (uid.equals(u.id)) return u;
        }
        if (currentUser != null && uid.equals(currentUser.id)) return currentUser;
        return null;
    }

    public User getUserById(String uid) {
        return findFriend(uid);
    }
    private String getGroupName(String gid) {
        for (Group g : groups) {
            if (g.id.equals(gid)) return g.name;
        }
        return gid;
    }
    public boolean isUserOnline(String userId) {
        return onlineUsers.contains(userId);
    }
    public Map<String, ChatRoom> chatRooms = new HashMap<>();
    public ChatRoom globalRoom = new ChatRoom("global", "公共大厅", true);
    private Context context;
    public String latestAnnouncement = "";
    public java.util.Map<String, String> groupAnnouncements = new java.util.HashMap<>();
    public List<Moment> moments = new ArrayList<>();
    private String currentVisibleRoom = null;
    private List<WSListener> listeners = new ArrayList<>();
    public interface WSListener {
        void onConnected();
        void onDisconnected();
        void onMessage(Message msg, String roomId);
        void onMessageRecalled(String msgId, String roomId);
        void onTyping(String fromUid);
        void onMessageRead(String fromUid, String msgId);
        void onGroupAnnouncement(String gid, String text, long time);
        void onTitleUpdate(String userId, String title);
        void onStatusUpdate(String userId, String status);
        void onPresenceUpdate();
        void onAvatarUpdate(String userId, String avatar);
        void onMomentsUpdated();
        void onMomentNotify(String action, String fromName, String momentText, String commentText);
        void onFriendListUpdated();
        void onFriendRequestReceived();
        void onFriendRequestResult(boolean ok, String error);
        void onFileUploadComplete(String fileId, String url, String filename, long size);
        void onFileUploadError(String fileId, String error);
        void onFileChunkAck(String fileId, int chunkIndex);
    }
    public static synchronized WebSocketManager getInstance() {
        if (instance == null) instance = new WebSocketManager();
        return instance;
    }
    public void setServer(String server) {
        this.serverBase = server;
    }
    public void notifyAvatarUpdate(String userId, String avatar) {
        for (WSListener l : listeners) l.onAvatarUpdate(userId, avatar);
    }
    public void setContext(Context ctx) { this.context = ctx; }
    public void connect(String token) {
        this.token = token;
        if (isConnecting) return;
        isConnecting = true;
        if (webSocket != null) webSocket.close(1000, "reconnect");
        String wsUrl = serverBase.replace("https://", "wss://").replace("http://", "ws://") + "/ws";
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = client.newWebSocket(request, new WSListenerImpl());
    }
    public boolean isConnected() { return isConnected; }
    private void scheduleReconnect() {
        if (reconnectAttempts >= 5) {
            reconnectAttempts = 0;
            return;
        }
        reconnectAttempts++;
        long delay = Math.min(1000 * (long) Math.pow(2, reconnectAttempts - 1), 10000);
        reconnectHandler.postDelayed(() -> {
            if (token != null && !isConnected) connect(token);
        }, delay);
    }
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "logout");
            webSocket = null;
        }
    }
    public void addListener(WSListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }
    public void removeListener(WSListener l) {
        listeners.remove(l);
    }
    public void setCurrentVisibleRoom(String roomId) {
        this.currentVisibleRoom = roomId;
    }
    private class WSListenerImpl extends WebSocketListener {
        @Override
        public void onOpen(WebSocket ws, okhttp3.Response response) {
            Log.d(TAG, "WebSocket connected, sending auth");
            isConnected = true;
            isConnecting = false;
            reconnectAttempts = 0;
            try {
                JSONObject auth = new JSONObject();
                auth.put("type", "auth");
                auth.put("token", token);
                ws.send(auth.toString());
            } catch (Exception e) {
                Log.e(TAG, "Send auth error", e);
            }
            for (WSListener l : listeners) l.onConnected();
        }
        @Override
        public void onMessage(WebSocket ws, String text) {
            try {
                JSONObject msg = new JSONObject(text);
                String type = msg.optString("type", "");
                switch (type) {
                    case "hello": parseHello(msg); break;
                    case "error": Log.e(TAG, "Server error: " + msg.optString("error", "")); break;
                    case "banned": Log.e(TAG, "User banned: " + msg.optString("error", "")); break;
                    case "global": handleGlobalMessage(msg); break;
                    case "dm": handleDmMessage(msg); break;
                    case "group": handleGroupMessage(msg); break;
                    case "recall": handleRecall(msg); break;
                    case "recalled": handleRecall(msg); break;
                    case "typing": handleTyping(msg); break;
                    case "read": handleRead(msg); break;
                    case "group_announcement": handleGroupAnnouncement(msg); break;
                    case "file_upload_complete": handleFileUploadComplete(msg); break;
                    case "file_upload_error": handleFileUploadError(msg); break;
                    case "file_chunk_ack": handleFileChunkAck(msg); break;
                    case "title-update": handleTitleUpdate(msg); break;
                    case "status-update": handleStatusUpdate(msg); break;
                    case "presence": handlePresence(msg); break;
                    case "moments": parseMoments(msg); break;
                    case "moment-notify": handleMomentNotify(msg); break;
                    case "system": handleSystem(msg); break;
                    case "friend-request": handleFriendRequest(msg); break;
                    case "friend-update": handleFriendUpdate(msg); break;
                    case "request-sent": handleRequestSent(msg); break;
                    case "request-respond": handleRequestRespond(msg); break;
                    case "friend-removed": handleFriendRemoved(msg); break;
                    case "group-renamed": handleGroupRenamed(msg); break;
                    case "group-member-removed": handleGroupMemberRemoved(msg); break;
                    case "group-members-added": handleGroupMembersAdded(msg); break;
                    case "group-dissolved": handleGroupDissolved(msg); break;
                    case "group-left": handleGroupLeft(msg); break;
                    case "group-removed": handleGroupRemoved(msg); break;
                    case "group-created": handleGroupCreated(msg); break;
                    case "group-apply-sent": break;
                    case "group-apply-accepted": handleGroupApplyAccepted(msg); break;
                    case "group-apply-rejected": break;
                    case "group-apply-request": break;
                    case "group-apply-list": break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Parse error", e);
            }
        }
        @Override
        public void onClosed(WebSocket ws, int code, String reason) {
            Log.d(TAG, "WebSocket closed: " + code + " " + reason);
            isConnected = false;
            isConnecting = false;
            for (WSListener l : listeners) l.onDisconnected();
            if (token != null) scheduleReconnect();
        }
        @Override
        public void onFailure(WebSocket ws, Throwable t, okhttp3.Response response) {
            Log.e(TAG, "WS failure", t);
            isConnected = false;
            isConnecting = false;
            for (WSListener l : listeners) l.onDisconnected();
            if (token != null) scheduleReconnect();
        }
    }
    // ========== 好友消息处理 ==========
    private void handleFriendRequest(JSONObject msg) {
        try {
            JSONObject reqObj = msg.optJSONObject("request");
            if (reqObj != null) {
                FriendRequest req = FriendRequest.fromJson(reqObj);
                boolean exists = false;
                for (FriendRequest r : friendRequests) {
                    if (r.id.equals(req.id)) { exists = true; break; }
                }
                if (!exists) friendRequests.add(req);
                for (WSListener l : listeners) l.onFriendRequestReceived();
                showNotification("好友请求", req.fromName + " 请求添加你为好友", null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Handle friend request error", e);
        }
    }
    private void handleFriendUpdate(JSONObject msg) {
        try {
            friends.clear();
            JSONArray arr = msg.optJSONArray("friends");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    friends.add(User.fromJson(arr.getJSONObject(i)));
                }
            }
            for (WSListener l : listeners) l.onFriendListUpdated();
        } catch (Exception e) {
            Log.e(TAG, "Handle friend update error", e);
        }
    }
    private void handleRequestSent(JSONObject msg) {
        boolean ok = msg.optBoolean("ok", false);
        String error = msg.optString("error", "");
        for (WSListener l : listeners) l.onFriendRequestResult(ok, error);
    }
    private void handleRequestRespond(JSONObject msg) {
        Log.d(TAG, "Request respond: " + msg.toString());
    }
    private void handleFriendRemoved(JSONObject msg) {
        try {
            String fromId = msg.optString("from", "");
            User toRemove = null;
            for (User u : friends) {
                if (u.id.equals(fromId)) { toRemove = u; break; }
            }
            if (toRemove != null) friends.remove(toRemove);
            for (WSListener l : listeners) l.onFriendListUpdated();
        } catch (Exception e) {
            Log.e(TAG, "Handle friend removed error", e);
        }
    }
    // ========== 群更新处理 ==========
    private void handleGroupRenamed(JSONObject msg) {
        try {
            String gid = msg.optString("gid", "");
            JSONObject gObj = msg.optJSONObject("group");
            if (gObj != null) {
                Group g = Group.fromJson(gObj);
                for (int i = 0; i < groups.size(); i++) {
                    if (groups.get(i).id.equals(gid)) { groups.set(i, g); break; }
                }
                ChatRoom room = chatRooms.get(gid);
                if (room != null) room.name = g.name;
            }
            for (WSListener l : listeners) l.onFriendListUpdated();
        } catch (Exception e) { Log.e(TAG, "Group renamed error", e); }
    }
    private void handleGroupMemberRemoved(JSONObject msg) {
        try {
            String gid = msg.optString("gid", "");
            String userId = msg.optString("userId", "");
            JSONObject gObj = msg.optJSONObject("group");
            if (gObj != null) {
                Group g = Group.fromJson(gObj);
                for (int i = 0; i < groups.size(); i++) {
                    if (groups.get(i).id.equals(gid)) { groups.set(i, g); break; }
                }
            }
            // 如果是自己被移除
            if (currentUser != null && userId.equals(currentUser.id)) {
                groups.removeIf(g -> g.id.equals(gid));
                chatRooms.remove(gid);
            }
            for (WSListener l : listeners) l.onFriendListUpdated();
        } catch (Exception e) { Log.e(TAG, "Member removed error", e); }
    }
    private void handleGroupMembersAdded(JSONObject msg) {
        try {
            String gid = msg.optString("gid", "");
            JSONObject gObj = msg.optJSONObject("group");
            if (gObj != null) {
                Group g = Group.fromJson(gObj);
                boolean found = false;
                for (int i = 0; i < groups.size(); i++) {
                    if (groups.get(i).id.equals(gid)) { groups.set(i, g); found = true; break; }
                }
                if (!found) {
                    groups.add(g);
                    chatRooms.put(gid, new ChatRoom(gid, g.name, true));
                }
            }
            for (WSListener l : listeners) l.onFriendListUpdated();
        } catch (Exception e) { Log.e(TAG, "Members added error", e); }
    }
    private void handleGroupDissolved(JSONObject msg) {
        try {
            String gid = msg.optString("gid", "");
            groups.removeIf(g -> g.id.equals(gid));
            chatRooms.remove(gid);
            for (WSListener l : listeners) l.onFriendListUpdated();
        } catch (Exception e) { Log.e(TAG, "Group dissolved error", e); }
    }
    private void handleGroupLeft(JSONObject msg) {
        try {
            String gid = msg.optString("gid", "");
            groups.removeIf(g -> g.id.equals(gid));
            chatRooms.remove(gid);
            for (WSListener l : listeners) l.onFriendListUpdated();
        } catch (Exception e) { Log.e(TAG, "Group left error", e); }
    }
    private void handleGroupRemoved(JSONObject msg) {
        try {
            String gid = msg.optString("gid", "");
            groups.removeIf(g -> g.id.equals(gid));
            chatRooms.remove(gid);
            for (WSListener l : listeners) l.onFriendListUpdated();
        } catch (Exception e) { Log.e(TAG, "Group removed error", e); }
    }
    private void handleGroupCreated(JSONObject msg) {
        try {
            JSONObject gObj = msg.optJSONObject("group");
            if (gObj != null) {
                Group g = Group.fromJson(gObj);
                boolean found = false;
                for (Group existing : groups) {
                    if (existing.id.equals(g.id)) { found = true; break; }
                }
                if (!found) {
                    groups.add(g);
                    chatRooms.put(g.id, new ChatRoom(g.id, g.name, true));
                }
            }
            for (WSListener l : listeners) l.onFriendListUpdated();
        } catch (Exception e) { Log.e(TAG, "Group created error", e); }
    }
    private void handleGroupApplyAccepted(JSONObject msg) {
        try {
            String gid = msg.optString("gid", "");
            JSONObject gObj = msg.optJSONObject("group");
            if (gObj != null) {
                Group g = Group.fromJson(gObj);
                boolean found = false;
                for (Group existing : groups) {
                    if (existing.id.equals(gid)) { found = true; break; }
                }
                if (!found) {
                    groups.add(g);
                    chatRooms.put(gid, new ChatRoom(gid, g.name, true));
                }
            }
            for (WSListener l : listeners) l.onFriendListUpdated();
            showNotification("入群申请通过", "你已加入群聊", gid);
        } catch (Exception e) { Log.e(TAG, "Group apply accepted error", e); }
    }
    private void parseHello(JSONObject msg) {
        try {
            JSONObject selfObj = msg.optJSONObject("self");
            if (selfObj != null) {
                currentUser = User.fromJson(selfObj);
                // 保存头像到 SharedPreferences
                if (context != null && currentUser.avatar != null) {
                    context.getSharedPreferences("chatapp_prefs", 0).edit()
                        .putString("avatar", currentUser.avatar).apply();
                }
            }
            isAdmin = msg.optBoolean("isAdmin", false);
            latestAnnouncement = msg.optString("announcement", "");
            // 解析群公告
            JSONObject groupAnnObj = msg.optJSONObject("groupAnnouncements");
            if (groupAnnObj != null) {
                java.util.Iterator<String> keys = groupAnnObj.keys();
                while (keys.hasNext()) {
                    String gid = keys.next();
                    JSONObject annObj = groupAnnObj.optJSONObject(gid);
                    if (annObj != null) {
                        groupAnnouncements.put(gid, annObj.optString("text", ""));
                    } else {
                        groupAnnouncements.put(gid, groupAnnObj.optString(gid, ""));
                    }
                }
            }
            // 好友
            friends.clear();
            JSONArray friendsArr = msg.optJSONArray("friends");
            if (friendsArr != null) {
                for (int i = 0; i < friendsArr.length(); i++) {
                    friends.add(User.fromJson(friendsArr.getJSONObject(i)));
                }
            }
            // 待处理的好友请求
            friendRequests.clear();
            JSONArray pendingArr = msg.optJSONArray("pendingRequests");
            if (pendingArr != null) {
                for (int i = 0; i < pendingArr.length(); i++) {
                    friendRequests.add(FriendRequest.fromJson(pendingArr.getJSONObject(i)));
                }
            }
            // 公共大厅历史
            globalRoom.messages.clear();
            JSONArray globalMsgs = msg.optJSONArray("globalMsgs");
            if (globalMsgs != null) {
                for (int i = 0; i < globalMsgs.length(); i++) {
                    Message m = Message.fromJson(globalMsgs.getJSONObject(i));
                    globalRoom.addMessage(m);
                }
            }
            // 私聊历史 - 服务器key是roomKey(如u2_u3)，需解析出对方uid
            JSONObject dmRooms = msg.optJSONObject("dmRooms");
            if (dmRooms != null) {
                JSONArray keys = dmRooms.names();
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String roomKey = keys.getString(i);
                        // 从roomKey解析出对方uid
                        String otherUid = roomKey;
                        if (roomKey.contains("_") && currentUser != null) {
                            String[] parts = roomKey.split("_");
                            otherUid = parts[0].equals(currentUser.id) ? parts[1] : parts[0];
                        }
                        JSONArray msgs = dmRooms.getJSONArray(roomKey);
                        ChatRoom room = chatRooms.get(otherUid);
                        if (room == null) {
                            String name = getFriendName(otherUid);
                            room = new ChatRoom(otherUid, name, false);
                            chatRooms.put(otherUid, room);
                        } else {
                            room.name = getFriendName(otherUid);
                        }
                        room.messages.clear();
                        for (int j = 0; j < msgs.length(); j++) {
                            Message m = Message.fromJson(msgs.getJSONObject(j));
                            room.addMessage(m);
                        }
                    }
                }
            }
            // 群聊
            groups.clear();
            JSONArray groupsArr = msg.optJSONArray("groups");
            if (groupsArr != null) {
                for (int i = 0; i < groupsArr.length(); i++) {
                    Group g = Group.fromJson(groupsArr.getJSONObject(i));
                    groups.add(g);
                    if (!chatRooms.containsKey(g.id)) {
                        chatRooms.put(g.id, new ChatRoom(g.id, g.name, true));
                    } else {
                        chatRooms.get(g.id).name = g.name;
                    }
                }
            }
            // 群聊历史消息
            JSONObject groupMsgs = msg.optJSONObject("groupMsgs");
            if (groupMsgs != null) {
                JSONArray gkeys = groupMsgs.names();
                if (gkeys != null) {
                    for (int i = 0; i < gkeys.length(); i++) {
                        String gid = gkeys.getString(i);
                        JSONArray msgs = groupMsgs.getJSONArray(gid);
                        ChatRoom room = chatRooms.get(gid);
                        if (room == null) {
                            String name = getGroupName(gid);
                            room = new ChatRoom(gid, name, true);
                            chatRooms.put(gid, room);
                        }
                        room.messages.clear();
                        for (int j = 0; j < msgs.length(); j++) {
                            Message m = Message.fromJson(msgs.getJSONObject(j));
                            room.addMessage(m);
                        }
                    }
                }
            }
            // 朋友圈
            parseMoments(msg);
            for (WSListener l : listeners) l.onConnected();
            for (WSListener l : listeners) l.onFriendListUpdated();
            for (WSListener l : listeners) l.onFriendRequestReceived();
        } catch (Exception e) {
            Log.e(TAG, "Parse hello error", e);
        }
    }
    private void handleGlobalMessage(JSONObject msg) {
        try {
            Message m = Message.fromJson(msg);
            if (m == null) return;
            globalRoom.addMessage(m);
            if (appContext != null) MessageStore.saveMessage(appContext, "global", m);
            for (WSListener l : listeners) l.onMessage(m, "global");
        // 通知：如果当前不在公共大厅且不是自己发的
        if (currentUser != null && m.from != null && !m.from.equals(currentUser.id)
                && !"global".equals(currentVisibleRoom)) {
            String sender = m.fromName != null && !m.fromName.isEmpty() ? m.fromName : getFriendName(m.from);
            String content = m.hasImage() ? "[图片]" : m.content;
            showNotification("公共大厅 - " + sender, content, "global");
        }
        } catch (Exception e) { Log.e(TAG, "Handle global error", e); }
    }
    private void handleDmMessage(JSONObject msg) {
        try {
        Message m = Message.fromJson(msg);
        if (m == null) return;
        String from = m.from;
        String to = msg.optString("to", "");
        String other = currentUser != null && from.equals(currentUser.id) ? to : from;
        ChatRoom room = chatRooms.get(other);
        if (room == null) {
            // 优先用消息中的发送者名称，其次用好友列表中的名称
            String name = other;
            if (m.fromName != null && !m.fromName.isEmpty() && from != null && !from.equals(currentUser != null ? currentUser.id : "")) {
                name = m.fromName;
            } else {
                name = getFriendName(other);
            }
            room = new ChatRoom(other, name, false);
            chatRooms.put(other, room);
        } else if (room.name.equals(other) || room.name.startsWith("u")) {
            // 如果房间名还是uid，尝试更新为好友名称
            String name = getFriendName(other);
            if (!name.equals(other)) room.name = name;
        }
        room.addMessage(m);
        if (appContext != null) MessageStore.saveMessage(appContext, other, m);
        for (WSListener l : listeners) l.onMessage(m, other);
        // 通知：如果当前不在这个私聊房间且不是自己发的
        if (currentUser != null && !m.from.equals(currentUser.id)
                && !other.equals(currentVisibleRoom)) {
            String sender = m.fromName != null && !m.fromName.isEmpty() ? m.fromName : room.name;
            String content = m.hasImage() ? "[图片]" : m.content;
            showNotification(sender, content, other);
        }
        } catch (Exception e) { Log.e(TAG, "Handle dm error", e); }
    }
    private void handleGroupMessage(JSONObject msg) {
        try {
        Message m = Message.fromJson(msg);
        if (m == null) return;
        String gid = msg.optString("gid", "");
        ChatRoom room = chatRooms.get(gid);
        if (room == null) {
            String name = getGroupName(gid);
            room = new ChatRoom(gid, name, true);
            chatRooms.put(gid, room);
        }
        room.addMessage(m);
        if (appContext != null) MessageStore.saveMessage(appContext, gid, m);
        for (WSListener l : listeners) l.onMessage(m, gid);
        // 通知：如果当前不在这个群聊且不是自己发的
        if (currentUser != null && m.from != null && !m.from.equals(currentUser.id)
                && !gid.equals(currentVisibleRoom)) {
            String sender = m.fromName != null && !m.fromName.isEmpty() ? m.fromName : getFriendName(m.from);
            String content = m.hasImage() ? "[图片]" : m.content;
            showNotification(room.name + " - " + sender, content, gid);
        }
        } catch (Exception e) { Log.e(TAG, "Handle group error", e); }
    }
    private void handleRecall(JSONObject msg) {
        String msgId = msg.optString("msgId", "");
        if (msgId.isEmpty()) msgId = msg.optString("id", "");
        String room = msg.optString("room", "");
        String roomId;
        // 服务器广播格式：room='global'/'dm'/'group'，私聊带 from/to，群聊带 gid
        if ("global".equals(room)) {
            roomId = "global";
        } else if ("dm".equals(room)) {
            // 私聊：从 from 和 to 计算对方 uid
            String from = msg.optString("from", "");
            String to = msg.optString("to", "");
            String other = (currentUser != null && from.equals(currentUser.id)) ? to : from;
            roomId = other;
        } else if ("group".equals(room)) {
            // 群聊：gid 字段
            roomId = msg.optString("gid", "");
        } else if (room.startsWith("dm:")) {
            roomId = room.substring(3);
        } else if (room.startsWith("group:")) {
            roomId = room.substring(6);
        } else {
            roomId = room;
        }
        ChatRoom r = "global".equals(roomId) ? globalRoom : chatRooms.get(roomId);
        if (r != null) {
            for (Message m : r.messages) {
                if (m.id != null && m.id.equals(msgId)) {
                    m.recalled = true;
                    break;
                }
            }
        }
        for (WSListener l : listeners) l.onMessageRecalled(msgId, roomId);
    }
    private void handleSystem(JSONObject msg) {
        Message m = new Message();
        m.from = "system";
        m.fromName = "";
        m.content = msg.optString("content", "");
        m.time = msg.optLong("time", System.currentTimeMillis());
        if (m.content != null && m.content.startsWith("【站内公告】")) {
            latestAnnouncement = m.content;
        }
        globalRoom.addMessage(m);
        for (WSListener l : listeners) l.onMessage(m, "global");
    }
    private void handleMomentNotify(JSONObject msg) {
        String action = msg.optString("action", "comment");
        String fromName = msg.optString("fromName", "有人");
        String momentText = msg.optString("momentText", "");
        String commentText = msg.optString("commentText", "");
        for (WSListener l : listeners) l.onMomentNotify(action, fromName, momentText, commentText);
    }

    private void parseMoments(JSONObject msg) {
        moments.clear();
        JSONArray arr = msg.optJSONArray("moments");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                moments.add(Moment.fromJson(arr.optJSONObject(i)));
            }
        }
        // 按时间倒序排列，最新的在上面
        java.util.Collections.sort(moments, (a, b) -> Long.compare(b.time, a.time));
        for (WSListener l : listeners) l.onMomentsUpdated();
    }
    // ========== 发送消息 ==========
    public void sendGlobal(String content) {
        sendGlobal(content, null);
    }
    public void sendGlobal(String content, JSONObject quote) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "global");
            msg.put("content", content != null ? content : "");
            if (quote != null) msg.put("quote", quote);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Send global error", e); }
    }
    public void sendGlobalWithImages(String content, JSONArray images) {
        sendGlobalWithImages(content, images, null);
    }
    public void sendGlobalWithImages(String content, JSONArray images, JSONObject quote) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "global");
            msg.put("content", content != null ? content : "");
            if (images != null) msg.put("images", images);
            if (quote != null) msg.put("quote", quote);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Send global images error", e); }
    }
    public void sendDm(String to, String content) {
        sendDm(to, content, null);
    }
    public void sendDm(String to, String content, JSONObject quote) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "dm");
            msg.put("to", to);
            msg.put("content", content != null ? content : "");
            if (quote != null) msg.put("quote", quote);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Send dm error", e); }
    }
    public void sendDmWithImages(String to, String content, JSONArray images) {
        sendDmWithImages(to, content, images, null);
    }
    public void sendDmWithImages(String to, String content, JSONArray images, JSONObject quote) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "dm");
            msg.put("to", to);
            msg.put("content", content != null ? content : "");
            if (images != null) msg.put("images", images);
            if (quote != null) msg.put("quote", quote);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Send dm images error", e); }
    }
    // ========== 群聊 ==========
    public void sendGroup(String gid, String content) {
        sendGroup(gid, content, null);
    }
    public void sendGroup(String gid, String content, JSONObject quote) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group");
            msg.put("gid", gid);
            msg.put("content", content != null ? content : "");
            if (quote != null) msg.put("quote", quote);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Send group error", e); }
    }
    public void sendGroupWithImages(String gid, String content, JSONArray images) {
        sendGroupWithImages(gid, content, images, null);
    }
    public void sendGroupWithImages(String gid, String content, JSONArray images, JSONObject quote) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group");
            msg.put("gid", gid);
            msg.put("content", content != null ? content : "");
            if (images != null) msg.put("images", images);
            if (quote != null) msg.put("quote", quote);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Send group images error", e); }
    }
    public void createGroup(String name, List<String> memberIds) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "create-group");
            msg.put("name", name);
            JSONArray members = new JSONArray();
            for (String id : memberIds) members.put(id);
            msg.put("members", members);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Create group error", e); }
    }
    // ========== 群管理 ==========
    public void renameGroup(String gid, String name) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group-rename");
            msg.put("gid", gid);
            msg.put("name", name);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Rename group error", e); }
    }
    public void removeGroupMember(String gid, String userId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group-remove-member");
            msg.put("gid", gid);
            msg.put("userId", userId);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Remove member error", e); }
    }
    public void addGroupMembers(String gid, JSONArray members) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group-add-members");
            msg.put("gid", gid);
            msg.put("members", members);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Add members error", e); }
    }
    public void leaveGroup(String gid) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group-leave");
            msg.put("gid", gid);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Leave group error", e); }
    }
    public void dissolveGroup(String gid) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group-dissolve");
            msg.put("gid", gid);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Dissolve group error", e); }
    }

    public void setGroupAdmin(String gid, String userId, boolean isAdmin) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group-set-admin");
            msg.put("gid", gid);
            msg.put("userId", userId);
            msg.put("isAdmin", isAdmin);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Set group admin error", e); }
    }

    public void muteGroupUser(String gid, String userId, boolean mute) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group-mute");
            msg.put("gid", gid);
            msg.put("userId", userId);
            msg.put("mute", mute);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Mute user error", e); }
    }

    public void setGroupAllMuted(String gid, boolean allMuted) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group-mute");
            msg.put("gid", gid);
            msg.put("mute", allMuted);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Set all muted error", e); }
    }
    // ========== 群申请加入 ==========
    public void applyGroup(String gid) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group-apply");
            msg.put("gid", gid);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Apply group error", e); }
    }
    public void respondGroupApply(String applyId, boolean accept) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group-apply-respond");
            msg.put("applyId", applyId);
            msg.put("action", accept ? "accept" : "reject");
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Respond group apply error", e); }
    }
    // ========== 好友请求 ==========
    public void sendFriendRequest(String username) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "friend-request");
            msg.put("username", username);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Friend request error", e); }
    }
    public void acceptFriendRequest(String requestId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "request-respond");
            msg.put("requestId", requestId);
            msg.put("action", "accept");
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
            for (int i = 0; i < friendRequests.size(); i++) {
                if (friendRequests.get(i).id.equals(requestId)) {
                    friendRequests.remove(i);
                    break;
                }
            }
        } catch (Exception e) { Log.e(TAG, "Accept friend error", e); }
    }
    public void denyFriendRequest(String requestId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "request-respond");
            msg.put("requestId", requestId);
            msg.put("action", "deny");
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
            for (int i = 0; i < friendRequests.size(); i++) {
                if (friendRequests.get(i).id.equals(requestId)) {
                    friendRequests.remove(i);
                    break;
                }
            }
        } catch (Exception e) { Log.e(TAG, "Deny friend error", e); }
    }
    // ========== 管理员命令 ==========
    public void adminBanUser(String username) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "ban-user");
            msg.put("username", username);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Ban error", e); }
    }
    public void adminUnbanUser(String username) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "unban-user");
            msg.put("username", username);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Unban error", e); }
    }
    public void adminKickUser(String userId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "kick-user");
            msg.put("userId", userId);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Kick error", e); }
    }
    public void adminAnnounce(String content) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "announce");
            msg.put("content", content);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Announce error", e); }
    }
    public void adminSetMaxOnline(int value) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "set-max-online");
            msg.put("value", value);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Set max online error", e); }
    }
    public void adminRenameHall(String name) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "rename-hall");
            msg.put("name", name);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Rename hall error", e); }
    }
    public void adminClearHall() {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "clear-hall");
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Clear hall error", e); }
    }
    // ========== 撤回 ==========
    public void sendTyping(String to) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "typing");
            msg.put("to", to);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Send typing error", e); }
    }
    public void sendRead(String to, String msgId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "read");
            msg.put("to", to);
            msg.put("msgId", msgId);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Send read error", e); }
    }
    public boolean sendFileChunk(String fileId, int chunkIndex, int totalChunks, String base64Data, String filename, String fileType) {
        try {
            if (webSocket == null) {
                Log.e(TAG, "sendFileChunk: webSocket is null");
                return false;
            }
            if (!isConnected) {
                Log.e(TAG, "sendFileChunk: not connected");
                return false;
            }
            JSONObject msg = new JSONObject();
            msg.put("type", "file_chunk");
            msg.put("fileId", fileId);
            msg.put("chunkIndex", chunkIndex);
            msg.put("totalChunks", totalChunks);
            msg.put("data", base64Data);
            msg.put("filename", filename);
            msg.put("fileType", fileType);
            String jsonStr = msg.toString();
            Log.d(TAG, "sendFileChunk: fileId=" + fileId + " chunk=" + chunkIndex + " size=" + jsonStr.length());
            webSocket.send(jsonStr);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Send file chunk error", e);
            return false;
        }
    }

    public void sendFileUploadEnd(String fileId, int totalChunks, String filename, String fileType) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "file_upload_end");
            msg.put("fileId", fileId);
            msg.put("totalChunks", totalChunks);
            msg.put("filename", filename);
            msg.put("fileType", fileType);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Send file upload end error", e); }
    }
    public void sendGroupAnnouncement(String gid, String text) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "group_announcement");
            msg.put("gid", gid);
            msg.put("text", text);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
            // 立即更新本地缓存，确保发布者自己能看到横幅
            groupAnnouncements.put(gid, text);
            for (WSListener l : listeners) l.onGroupAnnouncement(gid, text, System.currentTimeMillis());
        } catch (Exception e) { Log.e(TAG, "Send group announcement error", e); }
    }
    private void handleTyping(JSONObject msg) {
        String from = msg.optString("from", "");
        for (WSListener l : listeners) l.onTyping(from);
    }
    private void handleRead(JSONObject msg) {
        String from = msg.optString("from", "");
        String msgId = msg.optString("msgId", "");
        for (WSListener l : listeners) l.onMessageRead(from, msgId);
    }
    private void handlePresence(JSONObject msg) {
        JSONArray online = msg.optJSONArray("online");
        if (online != null) {
            onlineUsers.clear();
            for (int i = 0; i < online.length(); i++) {
                JSONObject u = online.optJSONObject(i);
                if (u != null) {
                    onlineUsers.add(u.optString("id", ""));
                }
            }
        }
        for (WSListener l : listeners) l.onPresenceUpdate();
    }
    private void handleStatusUpdate(JSONObject msg) {
        String userId = msg.optString("userId", "");
        String status = msg.optString("status", "");
        for (User u : friends) {
            if (u.id.equals(userId)) { u.status = status; break; }
        }
        if (currentUser != null && currentUser.id.equals(userId)) {
            currentUser.status = status;
        }
        for (WSListener l : listeners) l.onStatusUpdate(userId, status);
    }
    private void handleTitleUpdate(JSONObject msg) {
        String userId = msg.optString("userId", "");
        String title = msg.optString("title", "");
        // 更新好友列表中的头衔
        for (User u : friends) {
            if (u.id.equals(userId)) { u.title = title; break; }
        }
        // 更新当前用户头衔
        if (currentUser != null && currentUser.id.equals(userId)) {
            currentUser.title = title;
        }
        for (WSListener l : listeners) l.onTitleUpdate(userId, title);
    }
    private void handleFileUploadComplete(JSONObject msg) {
        String fileId = msg.optString("fileId", "");
        String url = msg.optString("url", "");
        String filename = msg.optString("filename", "");
        long size = msg.optLong("size", 0);
        for (WSListener l : listeners) l.onFileUploadComplete(fileId, url, filename, size);
    }
    private void handleFileChunkAck(JSONObject msg) {
        String fileId = msg.optString("fileId", "");
        int chunkIndex = msg.optInt("chunkIndex", -1);
        for (WSListener l : listeners) l.onFileChunkAck(fileId, chunkIndex);
    }
    private void handleFileUploadError(JSONObject msg) {
        String fileId = msg.optString("fileId", "");
        String error = msg.optString("error", "上传失败");
        for (WSListener l : listeners) l.onFileUploadError(fileId, error);
    }
    private void handleGroupAnnouncement(JSONObject msg) {
        String gid = msg.optString("gid", "");
        String text = msg.optString("text", "");
        long time = msg.optLong("time", System.currentTimeMillis());
        groupAnnouncements.put(gid, text);
        for (WSListener l : listeners) l.onGroupAnnouncement(gid, text, time);
    }
    public void recallMessage(String msgId, String room) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "recall");
            msg.put("msgId", msgId);
            msg.put("room", room);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Recall error", e); }
    }
    // ========== 朋友圈 ==========
    public void likeMoment(String mid) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "moment-like");
            msg.put("mid", mid);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Like error", e); }
    }
    public void commentMoment(String mid, String text) {
        commentMoment(mid, text, null, null);
    }
    public void commentMoment(String mid, String text, String replyTo, String replyToName) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "moment-comment");
            msg.put("mid", mid);
            msg.put("text", text);
            if (replyTo != null) msg.put("replyTo", replyTo);
            if (replyToName != null) msg.put("replyToName", replyToName);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Comment error", e); }
    }
    public void deleteMoment(String mid) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "moment-delete");
            msg.put("mid", mid);
            if (webSocket != null && isConnected) webSocket.send(msg.toString());
        } catch (Exception e) { Log.e(TAG, "Delete error", e); }
    }
}
