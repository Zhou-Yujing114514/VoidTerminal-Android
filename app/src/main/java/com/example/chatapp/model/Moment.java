package com.example.chatapp.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Moment {
    public String id;
    public String author;
    public String authorName;
    public String authorAvatar;
    public String text;
    public long time;
    public List<String> images = new ArrayList<>();
    public List<String> likes = new ArrayList<>();
    public List<Comment> comments = new ArrayList<>();

    public static class Comment {
        public String id;
        public String user;
        public String userName;
        public String text;
        public long time;
        public String replyTo;
        public String replyToName;
    }

    public static Moment fromJson(JSONObject obj) {
        Moment m = new Moment();
        m.id = obj.optString("id", "");
        m.author = obj.optString("author", "");
        m.authorName = obj.optString("authorName", "");
        m.authorAvatar = obj.optString("authorAvatar", "");
        m.text = obj.optString("text", "");
        m.time = obj.optLong("time", System.currentTimeMillis());
        JSONArray imgs = obj.optJSONArray("images");
        if (imgs != null) {
            for (int i = 0; i < imgs.length(); i++) {
                m.images.add(imgs.optString(i, ""));
            }
        }
        JSONArray likesArr = obj.optJSONArray("likes");
        if (likesArr != null) {
            for (int i = 0; i < likesArr.length(); i++) {
                m.likes.add(likesArr.optString(i, ""));
            }
        }
        JSONArray commentsArr = obj.optJSONArray("comments");
        if (commentsArr != null) {
            for (int i = 0; i < commentsArr.length(); i++) {
                JSONObject c = commentsArr.optJSONObject(i);
                if (c != null) {
                    Comment comment = new Comment();
                    comment.id = c.optString("id", "");
                    comment.user = c.optString("user", "") != null && !c.optString("user", "").isEmpty() ? c.optString("user", "") : c.optString("author", "");
                    comment.userName = c.optString("userName", "") != null && !c.optString("userName", "").isEmpty() ? c.optString("userName", "") : c.optString("authorName", "");
                    comment.text = c.optString("text", "");
                    comment.time = c.optLong("time", 0);
                    comment.replyTo = c.optString("replyTo", "");
                    comment.replyToName = c.optString("replyToName", "");
                    m.comments.add(comment);
                }
            }
        }
        return m;
    }
}
