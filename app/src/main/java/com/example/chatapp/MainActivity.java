package com.example.chatapp;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.chatapp.fragment.ChatListFragment;
import com.example.chatapp.fragment.ContactsFragment;
import com.example.chatapp.fragment.MomentsFragment;
import com.example.chatapp.fragment.ProfileFragment;
import com.example.chatapp.util.ThemeManager;
import com.example.chatapp.websocket.WebSocketManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyTheme();
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
    private void applyTheme() {
        boolean dark = ThemeManager.isDarkMode(this);
        int bg = dark ? 0xFF1A1A2E : 0xFFF5F5F5;
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
