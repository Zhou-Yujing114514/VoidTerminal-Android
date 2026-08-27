package com.example.chatapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AppLockActivity extends AppCompatActivity {
    private EditText etPassword;
    private boolean isSetupMode = false;
    private String firstPassword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);

        etPassword = findViewById(R.id.et_lock_password);
        Button btnConfirm = findViewById(R.id.btn_lock_confirm);
        TextView tvTitle = findViewById(R.id.tv_lock_title);
        TextView tvHint = findViewById(R.id.tv_lock_hint);

        isSetupMode = getIntent().getBooleanExtra("setup", false);

        if (isSetupMode) {
            tvTitle.setText("设置应用锁密码");
            tvHint.setText("请输入4-6位数字密码");
        } else {
            tvTitle.setText("应用锁");
            tvHint.setText("请输入密码解锁");
        }

        btnConfirm.setOnClickListener(v -> {
            String pwd = etPassword.getText().toString().trim();
            if (pwd.length() < 4) {
                Toast.makeText(this, "密码至少4位", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isSetupMode) {
                if (firstPassword == null) {
                    firstPassword = pwd;
                    etPassword.setText("");
                    tvHint.setText("请再次输入密码确认");
                } else {
                    if (firstPassword.equals(pwd)) {
                        getSharedPreferences("app_lock", 0).edit()
                                .putString("password", pwd)
                                .putBoolean("enabled", true)
                                .apply();
                        Toast.makeText(this, "应用锁已开启", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                        firstPassword = null;
                        etPassword.setText("");
                        tvHint.setText("请输入4-6位数字密码");
                    }
                }
            } else {
                SharedPreferences prefs = getSharedPreferences("app_lock", 0);
                String savedPwd = prefs.getString("password", "");
                if (pwd.equals(savedPwd)) {
                    getSharedPreferences("app_lock", 0).edit()
                            .putLong("last_unlock", System.currentTimeMillis())
                            .apply();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
                    etPassword.setText("");
                }
            }
        });
    }
}
