package com.example.chatapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;

public class ThemeHelper {
    private static final String PREFS = "theme_settings";
    
    // ========== 主题设置方法 ==========
    
    public static void setBgColor(Context context, int color) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, 0).edit();
        editor.putInt("theme_color", color);
        editor.apply();
    }
    
    public static int getBgColor(Context context) {
        return context.getSharedPreferences(PREFS, 0).getInt("theme_color", Color.WHITE);
    }
    
    public static void setBgAlpha(Context context, int alpha) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, 0).edit();
        editor.putInt("bg_alpha", alpha);
        editor.apply();
    }
    
    public static int getBgAlpha(Context context) {
        return context.getSharedPreferences(PREFS, 0).getInt("bg_alpha", 100);
    }
    
    public static void setAccentColor(Context context, int color) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, 0).edit();
        editor.putInt("accent_color", color);
        editor.apply();
    }
    
    public static int getAccentColor(Context context) {
        return context.getSharedPreferences(PREFS, 0).getInt("accent_color", Color.parseColor("#FF1A73E8"));
    }
    
    public static void setBgImage(Context context, String uri) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, 0).edit();
        editor.putString("theme_bg_uri", uri);
        editor.apply();
    }
    
    public static String getBgImage(Context context) {
        return context.getSharedPreferences(PREFS, 0).getString("theme_bg_uri", null);
    }
    
    public static int getCardColor(Context context) {
        int bgColor = getBgColor(context);
        int alpha = getBgAlpha(context);
        return Color.argb(alpha, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor));
    }
    
    public static void resetTheme(Context context) {
        context.getSharedPreferences(PREFS, 0).edit().clear().apply();
    }
    
    // ========== 应用主题到视图 ==========
    
    public static void applyTheme(View rootView, Context context) {
        if (rootView == null || context == null) return;
        try {
            String themeBgUri = getBgImage(context);
            int themeColor = getBgColor(context);
            
            boolean hasTheme = (themeBgUri != null && !themeBgUri.isEmpty()) || themeColor != Color.WHITE;
            
            // 设置根视图背景
            if (themeBgUri != null && !themeBgUri.isEmpty()) {
                try {
                    Uri uri = Uri.parse(themeBgUri);
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
                    rootView.setBackground(drawable);
                } catch (Exception e) {
                    rootView.setBackgroundColor(themeColor);
                }
            } else {
                rootView.setBackgroundColor(themeColor);
            }
            
            if (hasTheme) {
                setContainersTransparent(rootView);
            }
        } catch (Exception e) {}
    }
    
    private static void setContainersTransparent(View view) {
        if (view == null) return;
        // 跳过内容视图
        if (view instanceof ImageView || view instanceof Button || 
            view instanceof EditText || view instanceof ImageButton ||
            view instanceof CheckBox || view instanceof RadioButton) {
            return;
        }
        // RecyclerView 的 item 设置半透明
        if (view.getParent() instanceof RecyclerView) {
            view.setBackgroundColor(0xB3FFFFFF);
            return;
        }
        // ViewGroup 容器设置透明
        if (view instanceof ViewGroup) {
            view.setBackgroundColor(Color.TRANSPARENT);
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                setContainersTransparent(vg.getChildAt(i));
            }
        }
        // RecyclerView 特殊处理
        if (view instanceof RecyclerView) {
            final RecyclerView rv = (RecyclerView) view;
            rv.setBackgroundColor(Color.TRANSPARENT);
            for (int i = 0; i < rv.getChildCount(); i++) {
                rv.getChildAt(i).setBackgroundColor(0xB3FFFFFF);
            }
            rv.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(View child) {
                    child.setBackgroundColor(0xB3FFFFFF);
                }
                @Override
                public void onChildViewDetachedFromWindow(View child) {}
            });
        }
        // 底部导航栏半透明
        if (view.getId() == R.id.bottom_nav) {
            view.setBackgroundColor(0xCCFFFFFF);
        }
    }
}
