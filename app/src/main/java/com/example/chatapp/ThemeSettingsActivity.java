package com.example.chatapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.util.ThemeHelper;

public class ThemeSettingsActivity extends Activity {
    private View previewBg;
    private View previewCard;
    private View previewAccent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // 用代码动态创建UI，避免布局文件兼容性问题
            ScrollView scrollView = new ScrollView(this);
            scrollView.setBackgroundColor(Color.WHITE);
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(16), dp(16), dp(16), dp(16));
            scrollView.addView(root);

            // 标题栏
            LinearLayout titleBar = new LinearLayout(this);
            titleBar.setOrientation(LinearLayout.HORIZONTAL);
            titleBar.setGravity(Gravity.CENTER_VERTICAL);
            TextView backBtn = new TextView(this);
            backBtn.setText("←");
            backBtn.setTextSize(24);
            backBtn.setTextColor(Color.BLACK);
            backBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
            backBtn.setOnClickListener(v -> finish());
            TextView title = new TextView(this);
            title.setText("自定义主题");
            title.setTextSize(20);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.BLACK);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            titleParams.setMargins(dp(8), 0, 0, 0);
            titleBar.addView(backBtn);
            titleBar.addView(title, titleParams);
            root.addView(titleBar);

            // 预览区域
            addSectionTitle(root, "预览");
            LinearLayout previewArea = new LinearLayout(this);
            previewArea.setOrientation(LinearLayout.VERTICAL);
            previewArea.setBackgroundColor(Color.parseColor("#FFF0F0F0"));
            previewArea.setPadding(dp(12), dp(12), dp(12), dp(12));
            previewBg = new View(this);
            previewBg.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams bgParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(80));
            previewArea.addView(previewBg, bgParams);
            previewCard = new View(this);
            previewCard.setBackgroundColor(Color.parseColor("#FFF5F5F5"));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(80), dp(30));
            cardParams.setMargins(0, dp(8), 0, 0);
            previewArea.addView(previewCard, cardParams);
            previewAccent = new View(this);
            previewAccent.setBackgroundColor(Color.parseColor("#FF1A73E8"));
            LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(30), dp(30));
            accentParams.setMargins(0, dp(8), 0, 0);
            previewArea.addView(previewAccent, accentParams);
            root.addView(previewArea);
            addMargin(root, dp(16));

            // 背景图片
            addSectionTitle(root, "背景图片");
            addButton(root, "选择背景图片", Color.parseColor("#FF1A73E8"), Color.WHITE, v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, 1001);
                } catch (Exception e) {
                    Toast.makeText(this, "打开相册失败", Toast.LENGTH_SHORT).show();
                }
            });
            addMargin(root, dp(16));

            // 背景颜色
            addSectionTitle(root, "背景颜色");
            LinearLayout colorRow = new LinearLayout(this);
            colorRow.setOrientation(LinearLayout.HORIZONTAL);
            addColorButton(colorRow, "白色", Color.WHITE, Color.BLACK, v -> { ThemeHelper.setBgColor(this, Color.WHITE); updatePreview(); });
            addColorButton(colorRow, "浅灰", Color.parseColor("#FFF5F5F5"), Color.BLACK, v -> { ThemeHelper.setBgColor(this, Color.parseColor("#FFF5F5F5")); updatePreview(); });
            addColorButton(colorRow, "深色", Color.parseColor("#FF1A1A1A"), Color.WHITE, v -> { ThemeHelper.setBgColor(this, Color.parseColor("#FF1A1A1A")); updatePreview(); });
            root.addView(colorRow);
            addMargin(root, dp(16));

            // 背景透明度
            addSectionTitle(root, "背景透明度");
            SeekBar sbAlpha = new SeekBar(this);
            sbAlpha.setMax(255);
            sbAlpha.setProgress(ThemeHelper.getBgAlpha(this));
            sbAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    ThemeHelper.setBgAlpha(ThemeSettingsActivity.this, progress);
                    updatePreview();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            root.addView(sbAlpha);
            addMargin(root, dp(16));

            // 主题色
            addSectionTitle(root, "主题色（按钮、强调色）");
            LinearLayout accentRow = new LinearLayout(this);
            accentRow.setOrientation(LinearLayout.HORIZONTAL);
            addColorButton(accentRow, "蓝", Color.parseColor("#FF1A73E8"), Color.WHITE, v -> { ThemeHelper.setAccentColor(this, Color.parseColor("#FF1A73E8")); updatePreview(); });
            addColorButton(accentRow, "绿", Color.parseColor("#FF4CAF50"), Color.WHITE, v -> { ThemeHelper.setAccentColor(this, Color.parseColor("#FF4CAF50")); updatePreview(); });
            addColorButton(accentRow, "紫", Color.parseColor("#FF9C27B0"), Color.WHITE, v -> { ThemeHelper.setAccentColor(this, Color.parseColor("#FF9C27B0")); updatePreview(); });
            addColorButton(accentRow, "橙", Color.parseColor("#FFFF9800"), Color.WHITE, v -> { ThemeHelper.setAccentColor(this, Color.parseColor("#FFFF9800")); updatePreview(); });
            root.addView(accentRow);
            addMargin(root, dp(16));

            // 重置主题
            addButton(root, "重置为默认主题", Color.parseColor("#FFE53935"), Color.WHITE, v -> {
                ThemeHelper.resetTheme(this);
                sbAlpha.setProgress(255);
                updatePreview();
                Toast.makeText(this, "主题已重置", Toast.LENGTH_SHORT).show();
            });

            setContentView(scrollView);
            updatePreview();
        } catch (Exception e) {
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void addSectionTitle(LinearLayout root, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.BLACK);
        tv.setPadding(0, 0, 0, dp(8));
        root.addView(tv);
    }

    private void addButton(LinearLayout root, String text, int bgColor, int textColor, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(15);
        btn.setTextColor(textColor);
        btn.setBackgroundColor(bgColor);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(16), dp(12), dp(16), dp(12));
        btn.setOnClickListener(listener);
        root.addView(btn);
    }

    private void addColorButton(LinearLayout parent, String text, int bgColor, int textColor, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(14);
        btn.setTextColor(textColor);
        btn.setBackgroundColor(bgColor);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(8), dp(10), dp(8), dp(10));
        btn.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(btn, params);
    }

    private void addMargin(LinearLayout root, int margin) {
        View v = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, margin);
        root.addView(v, params);
    }

    private void updatePreview() {
        try {
            String bgImage = ThemeHelper.getBgImage(this);
            if (bgImage != null && !bgImage.isEmpty() && previewBg instanceof ImageView) {
                try {
                    com.bumptech.glide.Glide.with(this).load(Uri.parse(bgImage)).into((ImageView) previewBg);
                } catch (Exception e) {
                    int color = ThemeHelper.getBgColor(this);
                    int alpha = ThemeHelper.getBgAlpha(this);
                    int argb = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
                    previewBg.setBackgroundColor(argb);
                }
            } else {
                int color = ThemeHelper.getBgColor(this);
                int alpha = ThemeHelper.getBgAlpha(this);
                int argb = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
                previewBg.setBackgroundColor(argb);
            }
            previewCard.setBackgroundColor(ThemeHelper.getCardColor(this));
            previewAccent.setBackgroundColor(ThemeHelper.getAccentColor(this));
        } catch (Exception e) {
            // 忽略预览错误
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception e) {}
                ThemeHelper.setBgImage(this, uri.toString());
                updatePreview();
                Toast.makeText(this, "背景图片已设置", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
