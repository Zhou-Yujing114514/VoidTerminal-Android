package com.example.chatapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.api.ApiClient;
import com.example.chatapp.util.SharedPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AIChatActivity extends AppCompatActivity {
    private static final String TAG = "AIChatActivity";
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private EditText etInput;
    private Button btnSend;
    private List<JSONObject> history = new ArrayList<>();
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate start");
        try {
            if (getWindow() != null) {
                android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
                lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.7);
                getWindow().setAttributes(lp);
            }
            // 设置窗口为浮动模式，占屏幕85%
            if (getWindow() != null) {
                android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
                lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.7);
                getWindow().setAttributes(lp);
                getWindow().setBackgroundDrawableResource(R.drawable.bg_ai_dialog_round);
            }
            // 纯代码创建UI，不用布局文件
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(0xFFE8F5E9); // 淡绿色背景
            root.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));

            // 标题栏
            LinearLayout titleBar = new LinearLayout(this);
            titleBar.setOrientation(LinearLayout.HORIZONTAL);
            titleBar.setGravity(Gravity.CENTER_VERTICAL);
            titleBar.setPadding(32, 24, 32, 24);
            titleBar.setBackgroundColor(0xFFC8E6C9); // 淡绿色标题栏

            TextView tvTitle = new TextView(this);
            tvTitle.setText("纳西妲 AI 助手");
            tvTitle.setTextSize(18);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setTextColor(Color.BLACK);
            tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            Button btnClose = new Button(this);
            btnClose.setText("关闭");
            btnClose.setTextColor(Color.WHITE);
            btnClose.setBackgroundColor(0xFF888888);
            btnClose.setOnClickListener(v -> {
                Log.d(TAG, "Close button clicked");
                finish();
            });

            titleBar.addView(tvTitle);
            titleBar.addView(btnClose);
            root.addView(titleBar);

            // 聊天区域
            scrollView = new ScrollView(this);
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
            scrollView.setBackgroundColor(0xFFE8F5E9); // 淡绿色聊天背景
            scrollView.setPadding(24, 16, 24, 16);

            chatContainer = new LinearLayout(this);
            chatContainer.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(chatContainer);
            root.addView(scrollView);

            // 输入区域
            LinearLayout inputBar = new LinearLayout(this);
            inputBar.setOrientation(LinearLayout.HORIZONTAL);
            inputBar.setGravity(Gravity.CENTER_VERTICAL);
            inputBar.setPadding(24, 16, 24, 16);
            inputBar.setBackgroundColor(0xFFC8E6C9); // 淡绿色输入栏

            etInput = new EditText(this);
            etInput.setHint("和纳西妲说点什么...");
            etInput.setBackgroundColor(0xFFFFFFFF); // 白色输入框
            etInput.setPadding(20, 16, 20, 16);
            etInput.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            btnSend = new Button(this);
            btnSend.setText("发送");
            btnSend.setTextColor(Color.WHITE);
            btnSend.setBackgroundColor(0xFF66BB6A); // 绿色发送按钮
            btnSend.setOnClickListener(v -> sendMessage());
            LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sendParams.setMargins(16, 0, 0, 0);
            btnSend.setLayoutParams(sendParams);

            inputBar.addView(etInput);
            inputBar.addView(btnSend);
            root.addView(inputBar);

            setContentView(root);
            Log.d(TAG, "setContentView done");

            addMessage("纳西妲", "你好呀~我是纳西妲，须弥的草神。有什么想聊的吗？", false);
            Log.d(TAG, "onCreate success");
        } catch (Exception e) {
            Log.e(TAG, "onCreate error", e);
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        try {
            FloatingBallService.setBallVisible(true);
        } catch (Exception e) {
            Log.e(TAG, "onDestroy error", e);
        }
    }

    private void sendMessage() {
        try {
            String text = etInput.getText().toString().trim();
            if (text.isEmpty() || isLoading) return;

            addMessage("我", text, true);
            etInput.setText("");
            isLoading = true;
            btnSend.setEnabled(false);

            String token = SharedPrefs.getToken(this);
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", text);
            history.add(userMsg);

            JSONArray messages = new JSONArray();
            for (int i = Math.max(0, history.size() - 10); i < history.size(); i++) {
                messages.put(history.get(i));
            }

            ApiClient.aiChat(token, messages.toString(), new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        String reply = result.optString("reply", "抱歉，我现在有点累~");
                        JSONObject aiMsg = new JSONObject();
                        aiMsg.put("role", "assistant");
                        aiMsg.put("content", reply);
                        history.add(aiMsg);
                        addMessage("纳西妲", reply, false);
                    } catch (Exception e) {
                        addMessage("纳西妲", "抱歉，出了点小问题~", false);
                    }
                    isLoading = false;
                    btnSend.setEnabled(true);
                }

                @Override
                public void onError(String error) {
                    addMessage("纳西妲", "网络有点问题: " + error, false);
                    isLoading = false;
                    btnSend.setEnabled(true);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "sendMessage error", e);
            Toast.makeText(this, "发送失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            isLoading = false;
            btnSend.setEnabled(true);
        }
    }

    private void addMessage(String sender, String content, boolean isMe) {
        try {
            runOnUiThread(() -> {
                TextView tv = new TextView(this);
                tv.setText(sender + "：\n" + content);
                tv.setTextSize(14);
                tv.setTextColor(0xFF333333);
                tv.setPadding(24, 16, 24, 16);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.bottomMargin = 16;
                tv.setLayoutParams(params);
                tv.setBackgroundColor(isMe ? 0xFFA5D6A7 : 0xFFFFFFFF); // 自己淡绿，对方白色
                chatContainer.addView(tv);
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            });
        } catch (Exception e) {
            Log.e(TAG, "addMessage error", e);
        }
    }
}
