package com.example.chatapp.util;
import android.content.Context;
import com.example.chatapp.model.Message;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class MessageStore {
    private static final String FILE_NAME = "local_messages.json";
    private static Map<String, List<Message>> cache = null;
    public static synchronized Map<String, List<Message>> loadAll(Context ctx) {
        if (cache != null) return cache;
        cache = new HashMap<>();
        try {
            File f = new File(ctx.getFilesDir(), FILE_NAME);
            if (!f.exists()) return cache;
            BufferedReader br = new BufferedReader(new InputStreamReader(ctx.openFileInput(FILE_NAME)));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONArray keys = root.names();
            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    String roomId = keys.getString(i);
                    JSONArray arr = root.getJSONArray(roomId);
                    List<Message> msgs = new ArrayList<>();
                    for (int j = 0; j < arr.length(); j++) {
                        msgs.add(Message.fromJson(arr.getJSONObject(j)));
                    }
                    cache.put(roomId, msgs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cache;
    }
    public static synchronized List<Message> loadRoom(Context ctx, String roomId) {
        Map<String, List<Message>> all = loadAll(ctx);
        return all.getOrDefault(roomId, new ArrayList<>());
    }
    public static synchronized void saveMessage(Context ctx, String roomId, Message msg) {
        Map<String, List<Message>> all = loadAll(ctx);
        List<Message> msgs = all.get(roomId);
        if (msgs == null) {
            msgs = new ArrayList<>();
            all.put(roomId, msgs);
        }
        // 去重
        for (Message m : msgs) {
            if (m.id != null && m.id.equals(msg.id)) return;
        }
        msgs.add(msg);
        // 只保留最近500条，防止文件过大
        if (msgs.size() > 500) {
            msgs = msgs.subList(msgs.size() - 500, msgs.size());
            all.put(roomId, msgs);
        }
        persist(ctx);
    }
    public static synchronized void mergeMessages(Context ctx, String roomId, List<Message> serverMsgs) {
        Map<String, List<Message>> all = loadAll(ctx);
        List<Message> local = all.get(roomId);
        if (local == null || local.isEmpty()) {
            all.put(roomId, new ArrayList<>(serverMsgs));
            persist(ctx);
            return;
        }
        // 合并：以服务器消息为主，补充本地更早的消息
        List<Message> merged = new ArrayList<>();
        // 先加本地中比服务器最早消息还早的
        long serverEarliest = Long.MAX_VALUE;
        for (Message m : serverMsgs) {
            if (m.time < serverEarliest) serverEarliest = m.time;
        }
        for (Message m : local) {
            if (m.time < serverEarliest) merged.add(m);
        }
        merged.addAll(serverMsgs);
        // 去重
        Map<String, Message> dedup = new HashMap<>();
        for (Message m : merged) {
            if (m.id != null) dedup.put(m.id, m);
        }
        merged = new ArrayList<>(dedup.values());
        // 按时间排序
        merged.sort((a, b) -> Long.compare(a.time, b.time));
        all.put(roomId, merged);
        persist(ctx);
    }
    private static void persist(Context ctx) {
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, List<Message>> entry : cache.entrySet()) {
                JSONArray arr = new JSONArray();
                for (Message m : entry.getValue()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", m.id);
                    obj.put("from", m.from);
                    obj.put("fromName", m.fromName);
                    obj.put("fromAvatar", m.fromAvatar);
                    obj.put("content", m.content);
                    obj.put("time", m.time);
                    obj.put("recalled", m.recalled);
                    if (m.images != null && !m.images.isEmpty()) {
                        JSONArray imgs = new JSONArray();
                        for (String img : m.images) imgs.put(img);
                        obj.put("images", imgs);
                    }
                    if (m.hasQuote()) {
                        JSONObject q = new JSONObject();
                        q.put("msgId", m.quoteMsgId);
                        q.put("content", m.quoteContent);
                        q.put("from", m.quoteFrom);
                        q.put("fromName", m.quoteFromName);
                        obj.put("quote", q);
                    }
                    arr.put(obj);
                }
                root.put(entry.getKey(), arr);
            }
            FileOutputStream fos = ctx.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(root.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static synchronized void clearCache() {
        cache = null;
    }
}
