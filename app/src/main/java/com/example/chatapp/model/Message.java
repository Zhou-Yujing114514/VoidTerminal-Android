package com.example.chatapp.model;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
public class Message {
    public String id;
    public String from;
    public String fromName;
    public String fromAvatar;
    public String content;
    public long time;
    public List<String> images = new ArrayList<>();
    public boolean recalled;
    // 引用消息
    public String quoteMsgId;
    public String quoteContent;
    public String quoteFrom;
    public String quoteFromName;
    public boolean hasQuote() {
        return quoteMsgId != null && !quoteMsgId.isEmpty();
    }
    public boolean hasImage() {
        return images != null && !images.isEmpty();
    }
    public boolean isImageUrl() {
        if (content == null) return false;
        return content.matches(".*\\.(png|jpg|jpeg|gif|webp)(\\?.*)?$");
    }
    public static Message fromJson(JSONObject obj) {
        Message m = new Message();
        m.id = obj.optString("id", "");
        m.from = obj.optString("from", "");
        m.fromName = obj.optString("fromName", "");
        m.fromAvatar = obj.optString("fromAvatar", "");
        m.content = obj.optString("content", "");
        m.time = obj.optLong("time", System.currentTimeMillis());
        m.recalled = obj.optBoolean("recalled", false);
        JSONArray imgs = obj.optJSONArray("images");
        if (imgs != null) {
            for (int i = 0; i < imgs.length(); i++) {
                m.images.add(imgs.optString(i, ""));
            }
        }
        // 引用消息
        JSONObject quote = obj.optJSONObject("quote");
        if (quote != null) {
            m.quoteMsgId = quote.optString("msgId", "");
            m.quoteContent = quote.optString("content", "");
            m.quoteFrom = quote.optString("from", "");
            m.quoteFromName = quote.optString("fromName", "");
        }
        return m;
    }
}
