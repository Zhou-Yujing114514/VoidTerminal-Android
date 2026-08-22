package com.example.chatapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefs {
    private static final String PREFS = "chatapp_prefs";

    public static String getToken(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getString("token", "");
    }

    public static void setToken(Context ctx, String token) {
        ctx.getSharedPreferences(PREFS, 0).edit().putString("token", token).apply();
    }

    public static String getUsername(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getString("username", "");
    }

    public static void setUsername(Context ctx, String username) {
        ctx.getSharedPreferences(PREFS, 0).edit().putString("username", username).apply();
    }

    public static String getUserId(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getString("user_id", "");
    }

    public static void setUserId(Context ctx, String id) {
        ctx.getSharedPreferences(PREFS, 0).edit().putString("user_id", id).apply();
    }

    public static String getServer(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getString("server", "https://buer.kdns.fr");
    }

    public static void setServer(Context ctx, String server) {
        ctx.getSharedPreferences(PREFS, 0).edit().putString("server", server).apply();
    }

    public static long getAvatarVersion(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getLong("avatar_version", 0);
    }

    public static void setAvatarVersion(Context ctx, long version) {
        ctx.getSharedPreferences(PREFS, 0).edit().putLong("avatar_version", version).apply();
    }

    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREFS, 0).edit().clear().apply();
    }
}
