package com.example.chatapp;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.chatapp.api.ApiClient;
import com.example.chatapp.util.SharedPrefs;
import com.example.chatapp.websocket.WebSocketManager;
import org.json.JSONObject;
public class LoginActivity extends AppCompatActivity {
    private static final String SERVER_URL = "https://buer.kdns.fr";
    private LinearLayout loginForm;
    private LinearLayout loadingView;
    private TextView loadingText;
    private ProgressBar loadingProgress;
    private boolean isWaitingForConnection = false;
    private Handler timeoutHandler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginForm = findViewById(R.id.login_form);
        loadingView = findViewById(R.id.loading_view);
        loadingText = findViewById(R.id.loading_text);
        loadingProgress = findViewById(R.id.loading_progress);
        EditText etUsername = findViewById(R.id.et_username);
        EditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnRegister = findViewById(R.id.btn_register);
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }
            ApiClient.setBaseUrl(SERVER_URL);
            ApiClient.login(username, password, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        String token = result.getString("token");
                        JSONObject user = result.getJSONObject("user");
                        SharedPrefs.setToken(LoginActivity.this, token);
                        SharedPrefs.setUsername(LoginActivity.this, user.getString("username"));
                        SharedPrefs.setUserId(LoginActivity.this, user.getString("id"));
                        SharedPrefs.setAvatar(LoginActivity.this, user.optString("avatar", ""));
                        connectAndEnter(token);
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "登录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(LoginActivity.this, "登录失败: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }
            ApiClient.setBaseUrl(SERVER_URL);
            ApiClient.register(username, password, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    Toast.makeText(LoginActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(LoginActivity.this, "注册失败: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
        // 自动登录
        String token = SharedPrefs.getToken(this);
        if (!token.isEmpty()) {
            ApiClient.setBaseUrl(SERVER_URL);
            connectAndEnter(token);
        }
    }
    private void connectAndEnter(String token) {
        // 显示加载页
        loginForm.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
        loadingText.setText("正在连接服务器...");
        loadingProgress.setVisibility(View.VISIBLE);
        isWaitingForConnection = true;
        WebSocketManager.getInstance().setServer(SERVER_URL);
        // 注册临时监听器，等待连接成功
        WebSocketManager.WSListener tempListener = new WebSocketManager.WSListener() {
            @Override
            public void onConnected() {
                if (isWaitingForConnection) {
                    isWaitingForConnection = false;
                    timeoutHandler.removeCallbacksAndMessages(null);
                    runOnUiThread(() -> enterMain());
                }
            }
            @Override public void onDisconnected() {}
            @Override public void onMessage(com.example.chatapp.model.Message msg, String roomId) {}
            @Override public void onMessageRecalled(String msgId, String roomId) {}
            @Override public void onAvatarUpdate(String userId, String avatar) {}
    @Override public void onTyping(String fromUid) {}
    @Override public void onMessageRead(String fromUid, String msgId) {}
    @Override public void onGroupAnnouncement(String gid, String text, long time) {}
    @Override public void onFileUploadComplete(String fileId, String url, String filename, long size) {}
    @Override public void onFileUploadError(String fileId, String error) {}
    @Override public void onFileChunkAck(String fileId, int chunkIndex) {}
            @Override public void onMomentsUpdated() {}
            @Override public void onMomentNotify(String action, String fromName, String momentText, String commentText) {}
            @Override public void onFriendListUpdated() {}
            @Override public void onFriendRequestReceived() {}
            @Override public void onFriendRequestResult(boolean ok, String error) {}
    @Override
    public void onTitleUpdate(String userId, String title) {}
    @Override
    public void onStatusUpdate(String userId, String status) {}
    @Override
    public void onPresenceUpdate() {}

        };
        WebSocketManager.getInstance().addListener(tempListener);
        WebSocketManager.getInstance().connect(token);
        // 超时处理：10秒后即使没连接成功也进入
        timeoutHandler.postDelayed(() -> {
            if (isWaitingForConnection) {
                isWaitingForConnection = false;
                WebSocketManager.getInstance().removeListener(tempListener);
                runOnUiThread(() -> {
                    loadingText.setText("连接较慢，正在进入...");
                    new Handler().postDelayed(this::enterMain, 1000);
                });
            }
        }, 10000);
    }
    private void enterMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
