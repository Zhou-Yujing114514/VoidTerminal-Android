package com.example.chatapp.fragment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.chatapp.AdminActivity;
import com.example.chatapp.CreateGroupActivity;
import com.example.chatapp.LoginActivity;
import com.example.chatapp.R;
import com.example.chatapp.api.ApiClient;
import com.example.chatapp.util.SharedPrefs;
import com.example.chatapp.util.ThemeManager;
import com.example.chatapp.websocket.WebSocketManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
public class ProfileFragment extends Fragment {
    private static final String SERVER_URL = "https://buer.kdns.fr";
    private ImageView ivAvatar;
    private TextView tvUsername, tvUserId, tvServer;
    private Button btnTheme;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        ivAvatar = view.findViewById(R.id.iv_avatar);
        tvUsername = view.findViewById(R.id.tv_username);
        tvUserId = view.findViewById(R.id.tv_user_id);
        tvServer = view.findViewById(R.id.tv_server);
        btnTheme = view.findViewById(R.id.btn_theme);
        Button btnLogout = view.findViewById(R.id.btn_logout);
        Button btnCreateGroup = view.findViewById(R.id.btn_create_group);
        Button btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        Button btnNovel = view.findViewById(R.id.btn_novel);
        Button btnAdmin = view.findViewById(R.id.btn_admin);
        tvUsername.setText(SharedPrefs.getUsername(getContext()));
        tvUserId.setText("ID: " + SharedPrefs.getUserId(getContext()));
        tvServer.setText(SERVER_URL);
        updateThemeButton();
        String avatar = getContext().getSharedPreferences("chatapp_prefs", 0).getString("avatar", "");
        if (avatar != null && !avatar.isEmpty()) {
            String url = avatar.startsWith("/") ? SERVER_URL + avatar : avatar;
            long version = SharedPrefs.getAvatarVersion(getContext());
            if (version > 0) url += "?t=" + version;
            Glide.with(this).load(url).circleCrop().into(ivAvatar);
        }
        if (WebSocketManager.getInstance().isAdmin) {
            btnAdmin.setVisibility(View.VISIBLE);
        }
        btnAdmin.setOnClickListener(v -> startActivity(new Intent(getContext(), AdminActivity.class)));
        btnCreateGroup.setOnClickListener(v -> startActivity(new Intent(getContext(), CreateGroupActivity.class)));
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnTheme.setOnClickListener(v -> toggleTheme());
        btnNovel.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://morax.kdns.fr/"))));
        ivAvatar.setOnClickListener(v -> pickImage());
        btnLogout.setOnClickListener(v -> {
            WebSocketManager.getInstance().disconnect();
            SharedPrefs.clear(getContext());
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) uploadAvatar(uri);
                    }
                });
        return view;
    }
    private void updateThemeButton() {
        boolean dark = ThemeManager.isDarkMode(getActivity());
        btnTheme.setText(dark ? "切换日间模式" : "切换夜间模式");
    }
    private void toggleTheme() {
        boolean dark = ThemeManager.isDarkMode(getActivity());
        ThemeManager.setDarkMode(getActivity(), !dark);
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }
    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText etNewUsername = dialogView.findViewById(R.id.et_new_username);
        EditText etOldPassword = dialogView.findViewById(R.id.et_old_password);
        EditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        etNewUsername.setText(SharedPrefs.getUsername(getContext()));
        new AlertDialog.Builder(getContext())
                .setTitle("修改资料")
                .setView(dialogView)
                .setPositiveButton("保存", (d, w) -> {
                    String newUsername = etNewUsername.getText().toString().trim();
                    String oldPwd = etOldPassword.getText().toString();
                    String newPwd = etNewPassword.getText().toString();
                    String token = SharedPrefs.getToken(getContext());
                    // 修改用户名
                    if (!newUsername.equals(SharedPrefs.getUsername(getContext())) && !newUsername.isEmpty()) {
                        ApiClient.changeUsername(token, newUsername, new ApiClient.Callback() {
                            @Override
                            public void onSuccess(JSONObject result) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        SharedPrefs.setUsername(getContext(), newUsername);
                                        tvUsername.setText(newUsername);
                                        Toast.makeText(getContext(), "用户名已修改", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                            @Override
                            public void onError(String error) {
                                if (getActivity() != null)
                                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "用户名修改失败: " + error, Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                    // 修改密码
                    if (!oldPwd.isEmpty() && !newPwd.isEmpty()) {
                        ApiClient.changePassword(token, oldPwd, newPwd, new ApiClient.Callback() {
                            @Override
                            public void onSuccess(JSONObject result) {
                                if (getActivity() != null)
                                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "密码已修改", Toast.LENGTH_SHORT).show());
                            }
                            @Override
                            public void onError(String error) {
                                if (getActivity() != null)
                                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "密码修改失败: " + error, Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
    private void uploadAvatar(Uri uri) {
        try {
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
            is.close();
            String base64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            String token = SharedPrefs.getToken(getContext());
            ApiClient.uploadAvatar(token, base64, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        String avatar = result.getString("avatar");
                        getContext().getSharedPreferences("chatapp_prefs", 0).edit()
                                .putString("avatar", avatar).apply();
                        SharedPrefs.setAvatarVersion(getContext(), System.currentTimeMillis());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                String url = avatar.startsWith("/") ? SERVER_URL + avatar : avatar;
                                Glide.with(ProfileFragment.this).load(url + "?t=" + System.currentTimeMillis())
                                        .circleCrop().into(ivAvatar);
                                Toast.makeText(getContext(), "头像更换成功", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        if (getActivity() != null)
                            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
                @Override
                public void onError(String error) {
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "上传失败: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), "读取图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
