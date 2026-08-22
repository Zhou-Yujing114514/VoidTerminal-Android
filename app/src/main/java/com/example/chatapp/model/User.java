package com.example.chatapp.model;

import org.json.JSONObject;

public class User {
    public String id;
    public String username;
    public String avatar;
    public String role;
    public boolean banned;

    public static User fromJson(JSONObject obj) {
        User u = new User();
        u.id = obj.optString("id", "");
        u.username = obj.optString("username", "");
        u.avatar = obj.optString("avatar", "");
        u.role = obj.optString("role", "user");
        u.banned = obj.optBoolean("banned", false);
        return u;
    }
}
