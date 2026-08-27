package com.example.chatapp;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

public class FloatingBallService extends Service {
    private static final String TAG = "FloatingBall";
    private static FloatingBallService instance;
    private WindowManager windowManager;
    private ImageView floatingBall;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private long lastClickTime = 0;
    private int ballSize = 60; // 固定60px，约20dp

    public static void setBallVisible(boolean visible) {
        try {
            if (instance != null && instance.floatingBall != null) {
                instance.floatingBall.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e("FloatingBall", "setBallVisible error", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "FloatingBallService onCreate");
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        showFloatingBall();
    }

    private void showFloatingBall() {
        try {
            // 计算悬浮球大小：屏幕宽度的1/120，最小40px
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            ballSize = Math.max(60, screenWidth / 100);
            Log.d(TAG, "Screen width=" + screenWidth + ", ball size=" + ballSize);

            floatingBall = new ImageView(this);
            // 用普通的 ViewGroup.LayoutParams 设置 ImageView 尺寸
            floatingBall.setLayoutParams(new ViewGroup.LayoutParams(ballSize, ballSize));
            floatingBall.setScaleType(ImageView.ScaleType.CENTER_CROP);
            floatingBall.setAdjustViewBounds(true);
            // 圆形裁剪
            Glide.with(this).load(R.drawable.nahida_float).circleCrop().into(floatingBall);

            // WindowManager.LayoutParams 也设置固定尺寸
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            params.format = PixelFormat.TRANSLUCENT;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            params.gravity = Gravity.TOP | Gravity.START;
            params.width = ballSize;  // 固定宽度
            params.height = ballSize; // 固定高度
            params.x = 100;
            params.y = 300;

            Log.d(TAG, "Adding view to WindowManager, params width=" + params.width + " height=" + params.height);

            windowManager.addView(floatingBall, params);
            Log.d(TAG, "Floating ball added successfully");

            floatingBall.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true;
                            params.x = initialX + (int) dx;
                            params.y = initialY + (int) dy;
                            try {
                                windowManager.updateViewLayout(floatingBall, params);
                            } catch (Exception e) {
                                Log.e(TAG, "Update view layout error", e);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            long now = System.currentTimeMillis();
                            if (now - lastClickTime < 500) {
                                Log.d(TAG, "Duplicate click ignored");
                                return true;
                            }
                            lastClickTime = now;
                            Log.d(TAG, "Floating ball clicked");
                            try {
                                Log.d(TAG, "Hiding floating ball");
                                floatingBall.setVisibility(View.GONE);
                                Log.d(TAG, "Creating intent");
                                Intent intent = new Intent(this, AIChatActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Log.d(TAG, "Starting activity");
                                startActivity(intent);
                                Log.d(TAG, "AIChatActivity started successfully");
                            } catch (Exception e) {
                                Log.e(TAG, "Start AIChatActivity error", e);
                                floatingBall.setVisibility(View.VISIBLE);
                                Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            try {
                                int sw = getResources().getDisplayMetrics().widthPixels;
                                if (params.x < sw / 2) {
                                    params.x = 0;
                                } else {
                                    params.x = sw - ballSize;
                                }
                                windowManager.updateViewLayout(floatingBall, params);
                            } catch (Exception e) {
                                Log.e(TAG, "Snap to edge error", e);
                            }
                        }
                        return true;
                }
                return false;
            });
        } catch (Exception e) {
            Log.e(TAG, "Show floating ball error", e);
            Toast.makeText(this, "悬浮球创建失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            if (floatingBall != null && windowManager != null) {
                windowManager.removeView(floatingBall);
            }
        } catch (Exception e) {
            Log.e(TAG, "Remove floating ball error", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
