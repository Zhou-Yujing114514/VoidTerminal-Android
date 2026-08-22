package com.example.chatapp.util;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.chatapp.R;
public class ThemeManager {
    private static final String PREFS = "chatapp_prefs";
    private static final String KEY_DARK = "dark_mode";
    public static boolean isDarkMode(Activity ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, 0);
        return sp.getBoolean(KEY_DARK, true);
    }
    public static void setDarkMode(Activity ctx, boolean dark) {
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_DARK, dark).apply();
    }
    public static void apply(Activity ctx) {
        boolean dark = isDarkMode(ctx);
        int bgColor = dark ? 0xFF1A1A2E : 0xFFF5F5F5;
        int cardColor = dark ? 0xFF16213E : 0xFFFFFFFF;
        int textPrimary = dark ? 0xFFEAEAEA : 0xFF212121;
        int textSecondary = dark ? 0xFF8892B0 : 0xFF757575;
        ctx.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        View root = ctx.findViewById(android.R.id.content);
        if (root != null) {
            root.setBackgroundColor(bgColor);
            applyToView(root, textPrimary, textSecondary);
        }
    }
    private static void applyToView(View v, int textPrimary, int textSecondary) {
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            int current = tv.getCurrentTextColor();
            if (current == 0xFFEAEAEA || current == 0xFF8892B0 || current == 0xFF212121 || current == 0xFF757575) {
                tv.setTextColor(textPrimary);
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyToView(vg.getChildAt(i), textPrimary, textSecondary);
            }
        }
    }
}
