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
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
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
                JSONObject result = new JSONObject(sb.toString());
                mainHandler.post(() -> {
                    if (code >= 200 && code < 300 && result.optBoolean("ok", false)) {
                        cb.onSuccess(result);
                    } else {
                        cb.onError(result.optString("error", "HTTP " + code));
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
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                JSONObject result = new JSONObject(sb.toString());
                mainHandler.post(() -> cb.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
