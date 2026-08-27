package com.example.chatapp;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.chatapp.fragment.ChatListFragment;
import com.example.chatapp.fragment.ContactsFragment;
import com.example.chatapp.fragment.MomentsFragment;
import com.example.chatapp.fragment.ProfileFragment;
import com.example.chatapp.websocket.WebSocketManager;
import com.example.chatapp.util.ThemeHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebSocketManager.getInstance().setContext(this);
        // 应用锁检查
        android.content.SharedPreferences lockPrefs = getSharedPreferences("app_lock", 0);
        if (lockPrefs.getBoolean("enabled", false)) {
            long lastUnlock = lockPrefs.getLong("last_unlock", 0);
            // 超过5分钟需要重新解锁
            if (System.currentTimeMillis() - lastUnlock > 5 * 60 * 1000) {
                Intent intent = new Intent(this, AppLockActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        }
        setContentView(R.layout.activity_main);
        // 应用自定义主题
        
        // applyTheme(); // 已禁用，避免覆盖自定义主题
        WebSocketManager.setAppContext(this);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_chat) fragment = new ChatListFragment();
            else if (id == R.id.nav_contacts) fragment = new ContactsFragment();
            else if (id == R.id.nav_moments) fragment = new MomentsFragment();
            else if (id == R.id.nav_profile) fragment = new ProfileFragment();
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
                return true;
            }
            return false;
        });
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ChatListFragment())
                    .commit();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    private void applyTheme() {
        int bg = 0xFF1A1A2E;
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.setBackgroundColor(bg);
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                vg.getChildAt(i).setBackgroundColor(bg);
            }
        }
    }

}
