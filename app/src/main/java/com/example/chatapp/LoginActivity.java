package com.example.chatapp;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.chatapp.api.ApiClient;
import com.example.chatapp.util.SharedPrefs;
import com.example.chatapp.websocket.WebSocketManager;
import org.json.JSONObject;
public class LoginActivity extends AppCompatActivity {
    private static final String SERVER_URL = "https://buer.kdns.fr";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
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
                        WebSocketManager.getInstance().setServer(SERVER_URL);
                        WebSocketManager.getInstance().connect(token);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
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
            WebSocketManager.getInstance().setServer(SERVER_URL);
            WebSocketManager.getInstance().connect(token);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
