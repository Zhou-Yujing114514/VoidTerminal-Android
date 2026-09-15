package com.example.chatapp.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * 统一的偏好存储入口。
 *
 * 安全说明（v2.1.0）：
 * 此前 token / user_id 等敏感信息以明文形式存于普通 SharedPreferences，root 设备或备份
 * 即可读取。现已切换为 EncryptedSharedPreferences（基于 Android Keystore 的 AES256-GCM
 * 加密），token 等敏感字段落盘前加密。
 *
 * 兼容性：旧版本写入的明文 "chatapp_prefs" 不再被读取，升级后用户需重新登录一次。
 */
public class SharedPrefs {
    private static final String PREFS = "chatapp_secure_prefs";
    private static final String DEFAULT_SERVER = "https://buer.kdns.fr";

    private static volatile SharedPreferences instance;

    private static SharedPreferences prefs(Context ctx) {
        if (instance == null) {
            synchronized (SharedPrefs.class) {
                if (instance == null) {
                    try {
                        Context app = ctx.getApplicationContext();
                        MasterKey masterKey = new MasterKey.Builder(app)
                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                .build();
                        instance = EncryptedSharedPreferences.create(
                                app,
                                PREFS,
                                masterKey,
                                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        );
                    } catch (Exception e) {
                        // M10: 不再静默回退到明文 SharedPreferences（会把 token/PIN 明文落盘）。
                        // Keystore 异常通常意味着加密 prefs 文件损坏，删除损坏文件并重置单例，
                        // 由调用方/用户重新登录；绝不在降级路径创建明文 SP。
                        try {
                            ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                    .edit().clear().apply();
                        } catch (Exception ignored) {}
                        instance = null;
                        throw new RuntimeException("安全存储初始化失败，请重新登录: " + e.getMessage(), e);
                    }
                }
            }
        }
        return instance;
    }

    // ===== M9: 应用锁 PIN 存入加密 SharedPreferences（不再用普通 "app_lock" SP 明文）=====
    public static String getAppLockPin(Context ctx) {
        return prefs(ctx).getString("app_lock_pin", "");
    }
    public static void setAppLockPin(Context ctx, String pin) {
        prefs(ctx).edit().putString("app_lock_pin", pin).apply();
    }
    public static boolean isAppLockEnabled(Context ctx) {
        return prefs(ctx).getBoolean("app_lock_enabled", false);
    }
    public static void setAppLockEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean("app_lock_enabled", enabled).apply();
    }
    public static long getLastUnlock(Context ctx) {
        return prefs(ctx).getLong("app_lock_last_unlock", 0);
    }
    public static void setLastUnlock(Context ctx, long time) {
        prefs(ctx).edit().putLong("app_lock_last_unlock", time).apply();
    }

    public static String getToken(Context ctx) {
        return prefs(ctx).getString("token", "");
    }

    public static void setToken(Context ctx, String token) {
        prefs(ctx).edit().putString("token", token).apply();
    }

    public static String getUsername(Context ctx) {
        return prefs(ctx).getString("username", "");
    }

    public static void setUsername(Context ctx, String username) {
        prefs(ctx).edit().putString("username", username).apply();
    }

    public static String getUserId(Context ctx) {
        return prefs(ctx).getString("user_id", "");
    }

    public static void setUserId(Context ctx, String id) {
        prefs(ctx).edit().putString("user_id", id).apply();
    }

    public static String getServer(Context ctx) {
        return prefs(ctx).getString("server", DEFAULT_SERVER);
    }

    public static void setServer(Context ctx, String server) {
        prefs(ctx).edit().putString("server", server).apply();
    }

    public static long getAvatarVersion(Context ctx) {
        return prefs(ctx).getLong("avatar_version", 0);
    }

    public static void setAvatarVersion(Context ctx, long version) {
        prefs(ctx).edit().putLong("avatar_version", version).apply();
    }
    public static String getAvatar(Context ctx) {
        return prefs(ctx).getString("avatar", "");
    }
    public static void setAvatar(Context ctx, String avatar) {
        prefs(ctx).edit().putString("avatar", avatar).apply();
    }

    public static void clear(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }
}
