package com.example.chatapp.api;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class ApiClient {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    public interface Callback {
        void onSuccess(JSONObject result);
        void onError(String error);
    }
    public interface ProgressCallback {
        void onProgress(int percent);
        void onSuccess(JSONObject result);
        void onError(String error);
    }
    private static String baseUrl = "https://buer.kdns.fr";
    public static void setBaseUrl(String url) {
        baseUrl = url;
    }
    public static void login(String username, String password, Callback cb) {
        post("/api/login", "{\"username\":\"" + escape(username) + "\",\"password\":\"" + escape(password) + "\"}", cb);
    }
    public static void register(String username, String password, Callback cb) {
        post("/api/register", "{\"username\":\"" + escape(username) + "\",\"password\":\"" + escape(password) + "\"}", cb);
    }
    public static void uploadImage(String token, String base64, Callback cb) {
        post("/api/upload-msg-image", "{\"token\":\"" + token + "\",\"image\":\"" + base64 + "\"}", cb);
    }
    public static void uploadAvatar(String token, String base64, Callback cb) {
        post("/api/avatar", "{\"token\":\"" + token + "\",\"image\":\"" + base64 + "\"}", cb);
    }
    public static void uploadGroupAvatar(String token, String gid, String base64, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("gid", gid);
            body.put("data", base64);
            post("/api/group-avatar", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void setTitle(String token, String userId, String title, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("userId", userId);
            body.put("title", title);
            post("/api/set-title", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }

    public static void setGroupTitle(String token, String groupId, String userId, String title, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("groupId", groupId);
            body.put("userId", userId);
            body.put("title", title);
            post("/api/set-group-title", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void setStatus(String token, String status, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("status", status);
            post("/api/set-status", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void getAdminUsers(String token, Callback cb) {
        get("/api/admin/users?token=" + token, cb);
    }
    public static void banUser(String token, String userId, boolean banned, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("userId", userId);
            body.put("banned", banned);
            post("/api/admin/ban", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void deleteUser(String token, String userId, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("userId", userId);
            post("/api/admin/delete-user", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void adminBroadcast(String token, String text, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("text", text);
            post("/api/admin/broadcast", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void adminDeleteUser(String token, String userId, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("userId", userId);
            post("/api/admin/delete-user", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void deleteAccount(String token, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            post("/api/delete-account", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void uploadAudio(String token, String base64, String filename, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("data", base64);
            body.put("filename", filename);
            post("/api/upload-audio", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void uploadFile(String token, String base64, String filename, Callback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("data", base64);
            body.put("filename", filename);
            post("/api/upload-file", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void uploadAudioWithProgress(String token, String base64, String filename, final ProgressCallback cb) {
        uploadWithProgress("/api/upload-audio", token, base64, filename, cb);
    }
    public static void uploadImageWithProgress(String token, String base64, final ProgressCallback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("data", base64);
            uploadWithProgressBody("/api/upload-image", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    private static void uploadWithProgress(String path, String token, String base64, String filename, final ProgressCallback cb) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("token", token);
            body.put("data", base64);
            body.put("filename", filename);
            uploadWithProgressBody(path, body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    private static void uploadWithProgressBody(String path, String jsonBody, final ProgressCallback cb) {
        executor.execute(() -> {
            try {
                final byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
                URL url = new URL(baseUrl + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(3600000);
                // 使用默认缓冲模式，避免 Connection reset by peer
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    int chunkSize = 4096;
                    int sent = 0;
                    while (sent < bodyBytes.length) {
                        int len = Math.min(chunkSize, bodyBytes.length - sent);
                        os.write(bodyBytes, sent, len);
                        sent += len;
                        final int percent = (int) ((sent * 100L) / bodyBytes.length);
                        mainHandler.post(() -> cb.onProgress(percent));
                    }
                    os.flush();
                }
                int code = conn.getResponseCode();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(code >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                final String responseStr = sb.toString();
                final int finalCode = code;
                mainHandler.post(() -> {
                    try {
                        JSONObject result = new JSONObject(responseStr);
                        if (finalCode >= 200 && finalCode < 300 && result.optBoolean("ok", false)) {
                            cb.onSuccess(result);
                        } else {
                            cb.onError(result.optString("error", "HTTP " + finalCode));
                        }
                    } catch (Exception e) {
                        cb.onError("HTTP " + finalCode + ": " + responseStr.substring(0, Math.min(100, responseStr.length())));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }
    public static void uploadFileWithProgress(String token, String base64, String filename, final ProgressCallback cb) {
        executor.execute(() -> {
            try {
                org.json.JSONObject body = new org.json.JSONObject();
                body.put("token", token);
                body.put("data", base64);
                body.put("filename", filename);
                final byte[] bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
                URL url = new URL(baseUrl + "/api/upload-file");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(3600000);
                // 使用默认缓冲模式，避免 Connection reset by peer
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    int chunkSize = 4096;
                    int sent = 0;
                    while (sent < bodyBytes.length) {
                        int len = Math.min(chunkSize, bodyBytes.length - sent);
                        os.write(bodyBytes, sent, len);
                        sent += len;
                        final int percent = (int) ((sent * 100L) / bodyBytes.length);
                        mainHandler.post(() -> cb.onProgress(percent));
                    }
                    os.flush();
                }
                int code = conn.getResponseCode();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(code >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                final String responseStr = sb.toString();
                final int finalCode = code;
                mainHandler.post(() -> {
                    try {
                        JSONObject result = new JSONObject(responseStr);
                        if (finalCode >= 200 && finalCode < 300 && result.optBoolean("ok", false)) {
                            cb.onSuccess(result);
                        } else {
                            cb.onError(result.optString("error", "HTTP " + finalCode));
                        }
                    } catch (Exception e) {
                        cb.onError("HTTP " + finalCode + ": " + responseStr.substring(0, Math.min(100, responseStr.length())));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }
    public static void postMoment(String token, String text, JSONArray images, Callback cb) {
        try {
            JSONObject body = new JSONObject();
            body.put("token", token);
            body.put("text", text);
            body.put("images", images);
            post("/api/moment-post", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void searchGroups(String keyword, Callback cb) {
        get("/api/search-groups?keyword=" + escape(keyword), cb);
    }
    public static void changePassword(String token, String oldPassword, String newPassword, Callback cb) {
        try {
            JSONObject body = new JSONObject();
            body.put("token", token);
            body.put("oldPassword", oldPassword);
            body.put("newPassword", newPassword);
            body.put("newPassword2", newPassword);
            post("/api/change-password", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    public static void changeUsername(String token, String newUsername, Callback cb) {
        try {
            JSONObject body = new JSONObject();
            body.put("token", token);
            body.put("newUsername", newUsername);
            post("/api/change-username", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
    private static void post(String path, String jsonBody, Callback cb) {
        executor.execute(() -> {
            try {
                URL url = new URL(baseUrl + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(3600000);
                conn.setReadTimeout(3600000);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                final String responseStr = sb.toString();
                final int finalCode = code;
                mainHandler.post(() -> {
                    try {
                        JSONObject result = new JSONObject(responseStr);
                        if (finalCode >= 200 && finalCode < 300 && result.optBoolean("ok", false)) {
                            cb.onSuccess(result);
                        } else {
                            cb.onError(result.optString("error", "HTTP " + finalCode));
                        }
                    } catch (Exception e) {
                        cb.onError("HTTP " + finalCode + " (" + responseStr.length() + "字节): " + responseStr.substring(0, Math.min(80, responseStr.length())));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }
    private static void get(String path, Callback cb) {
        executor.execute(() -> {
            try {
                URL url = new URL(baseUrl + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                final String responseStr = sb.toString();
                final int finalCode = code;
                mainHandler.post(() -> {
                    try {
                        JSONObject result = new JSONObject(responseStr);
                        if (finalCode >= 200 && finalCode < 300 && result.optBoolean("ok", false)) {
                            cb.onSuccess(result);
                        } else {
                            cb.onError(result.optString("error", "HTTP " + finalCode));
                        }
                    } catch (Exception e) {
                        // JSON解析失败，返回HTTP状态码和响应内容预览
                        String preview = responseStr.length() > 50 ? responseStr.substring(0, 50) + "..." : responseStr;
                        cb.onError("HTTP " + finalCode + " 响应非JSON: " + preview);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError("网络错误: " + e.getMessage()));
            }
        });
    }
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static void aiChat(String token, String messagesJson, Callback cb) {
        try {
            JSONObject body = new JSONObject();
            body.put("token", token);
            body.put("messages", new JSONArray(messagesJson));
            post("/api/ai-chat", body.toString(), cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }

}
