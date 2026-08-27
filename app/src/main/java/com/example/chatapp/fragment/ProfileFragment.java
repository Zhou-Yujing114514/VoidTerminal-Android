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
import com.example.chatapp.websocket.WebSocketManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
public class ProfileFragment extends Fragment {
    private static final String SERVER_URL = "https://buer.kdns.fr";
    private ImageView ivAvatar;
    private TextView tvUsername, tvUserId, tvServer;
    private View btnCustomBg;
    private View btnCustomStatus;
    private View btnFloatingBall;
    private android.widget.TextView tvFloatingStatus;
    private androidx.activity.result.ActivityResultLauncher<Intent> bgPickerLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        ivAvatar = view.findViewById(R.id.iv_avatar);
        tvUsername = view.findViewById(R.id.tv_username);
        tvUserId = view.findViewById(R.id.tv_user_id);
        btnCustomBg = view.findViewById(R.id.btn_custom_bg);
        btnCustomStatus = view.findViewById(R.id.btn_custom_status);
        btnFloatingBall = view.findViewById(R.id.btn_floating_ball);
        tvFloatingStatus = view.findViewById(R.id.tv_floating_status);
        bgPickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) saveBackground(uri);
                    }
                });
        View btnLogout = view.findViewById(R.id.btn_logout);
        View btnCreateGroup = view.findViewById(R.id.btn_create_group);
        View btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        View btnNovel = view.findViewById(R.id.btn_novel);
        View btnCheckUpdate = view.findViewById(R.id.btn_check_update);
        View btnAdmin = view.findViewById(R.id.btn_admin);
        tvUsername.setText(SharedPrefs.getUsername(getContext()));
        tvUserId.setText("ID: " + SharedPrefs.getUserId(getContext()));
        String avatar = getContext().getSharedPreferences("chatapp_prefs", 0).getString("avatar", "");
        // 优先从 WebSocketManager 获取最新头像
        if (WebSocketManager.getInstance().currentUser != null && WebSocketManager.getInstance().currentUser.avatar != null) {
            avatar = WebSocketManager.getInstance().currentUser.avatar;
        }
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
        btnCustomStatus.setOnClickListener(v -> showStatusDialog());
        btnFloatingBall.setOnClickListener(v -> toggleFloatingBall());
        updateFloatingStatus();
        btnCustomBg.setOnClickListener(v -> showBgMenu());
        btnNovel.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://morax.kdns.fr/"))));
        btnCheckUpdate.setOnClickListener(v -> {
            String currentVersion = "v8.2";
            new AlertDialog.Builder(getContext())
                .setTitle("设置")
                .setItems(new String[]{"检查更新 (当前" + currentVersion + ")", "重连服务器", "注销账号"}, (d, which) -> {
                    if (which == 0) {
                        Toast.makeText(getContext(), "当前版本: " + currentVersion + "\n更新由管理员手动发布", Toast.LENGTH_LONG).show();
                    } else if (which == 1) {
                        // 重连服务器
                        if (WebSocketManager.getInstance().isConnected()) {
                            WebSocketManager.getInstance().disconnect();
                        }
                        String token = com.example.chatapp.util.SharedPrefs.getToken(getContext());
                        if (token != null) {
                            WebSocketManager.getInstance().connect(token);
                        }
                        Toast.makeText(getContext(), "正在重连服务器...", Toast.LENGTH_SHORT).show();
                    } else if (which == 2) {
                        // 注销账号
                        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                            .setTitle("确认注销")
                            .setMessage("确定注销账号吗？此操作不可恢复，所有数据将被删除！")
                            .setPositiveButton("确认注销", (d2, w2) -> {
                                String token = com.example.chatapp.util.SharedPrefs.getToken(getContext());
                                com.example.chatapp.api.ApiClient.deleteAccount(token, new com.example.chatapp.api.ApiClient.Callback() {
                                    @Override
                                    public void onSuccess(org.json.JSONObject result) {
                                        if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                            WebSocketManager.getInstance().disconnect();
                                            com.example.chatapp.util.SharedPrefs.clear(getContext());
                                            Intent intent = new Intent(getContext(), com.example.chatapp.LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            if (getActivity() != null) getActivity().finish();
                                        });
                                    }
                                    @Override
                                    public void onError(String error) {
                                        if (getActivity() != null) getActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(), "注销失败: " + error, Toast.LENGTH_SHORT).show());
                                    }
                                });
                            })
                            .setNegativeButton("取消", null)
                            .show();
                    }
                })
                .show();
        });
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
                    if (newUsername.equals(SharedPrefs.getUsername(getContext()))) {
                        Toast.makeText(getContext(), "不能将命名设为与之前相同的名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newUsername.isEmpty()) {
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
        // 检查权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 1001);
                return;
            }
        } else {
            if (androidx.core.content.ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
                return;
            }
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pickImage();
        } else {
            Toast.makeText(getContext(), "需要存储权限才能选择头像", Toast.LENGTH_SHORT).show();
        }
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
                                // 更新当前用户头像
                                if (WebSocketManager.getInstance().currentUser != null) {
                                    WebSocketManager.getInstance().currentUser.avatar = avatar;
                                }
                                // 通知所有页面头像更新
                                WebSocketManager.getInstance().notifyAvatarUpdate(
                                    SharedPrefs.getUserId(getContext()), avatar);
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


    private void saveBackground(android.net.Uri uri) {
        try {
            java.io.InputStream is = getContext().getContentResolver().openInputStream(uri);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
            is.close();
            String base64 = android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP);
            getContext().getSharedPreferences("chatapp_prefs", 0).edit()
                    .putString("chat_bg_global", base64).apply();
            Toast.makeText(getContext(), "背景设置成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showStatusDialog() {
        final android.widget.EditText etStatus = new android.widget.EditText(getContext());
        String currentStatus = "";
        if (WebSocketManager.getInstance().currentUser != null) {
            currentStatus = WebSocketManager.getInstance().currentUser.status != null ? WebSocketManager.getInstance().currentUser.status : "";
        }
        etStatus.setText(currentStatus);
        etStatus.setHint("输入自定义状态（最多30字）");
        etStatus.setMaxLines(1);
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
            .setTitle("设置自定义状态")
            .setView(etStatus)
            .setPositiveButton("确定", (d, w) -> {
                String status = etStatus.getText().toString().trim();
                String token = com.example.chatapp.util.SharedPrefs.getToken(getContext());
                com.example.chatapp.api.ApiClient.setStatus(token, status, new com.example.chatapp.api.ApiClient.Callback() {
                    @Override
                    public void onSuccess(org.json.JSONObject result) {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "状态设置成功", Toast.LENGTH_SHORT).show();
                            if (WebSocketManager.getInstance().currentUser != null) {
                                WebSocketManager.getInstance().currentUser.status = status;
                            }
                        });
                    }
                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "失败: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showBgMenu() {
        String[] items = {"选择背景图片", "恢复默认背景"};
        new AlertDialog.Builder(getContext())
                .setTitle("自定义背景")
                .setItems(items, (d, which) -> {
                    if (which == 0) pickBackground();
                    else clearBackground();
                })
                .show();
    }
    private void pickBackground() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        bgPickerLauncher.launch(intent);
    }
    private void clearBackground() {
        if (getContext() != null) {
            getContext().getSharedPreferences("chatapp_prefs", 0).edit()
                    .remove("chat_bg_global").apply();
            Toast.makeText(getContext(), "已恢复默认背景", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        updateFloatingStatus();
    }

    private void updateFloatingStatus() {
        if (getActivity() == null) return;
        android.content.SharedPreferences sp = getActivity().getSharedPreferences("chatapp_prefs", 0);
        boolean enabled = sp.getBoolean("floating_ball_enabled", false);
        if (tvFloatingStatus != null) {
            tvFloatingStatus.setText(enabled ? "已开启" : "未开启");
            tvFloatingStatus.setTextColor(enabled ? 0xFF4CAF50 : 0xFF999999);
        }
    }

    private void toggleFloatingBall() {
        android.content.SharedPreferences sp = getActivity().getSharedPreferences("chatapp_prefs", 0);
        boolean enabled = sp.getBoolean("floating_ball_enabled", false);
        if (enabled) {
            getActivity().stopService(new android.content.Intent(getActivity(), com.example.chatapp.FloatingBallService.class));
            sp.edit().putBoolean("floating_ball_enabled", false).apply();
            android.widget.Toast.makeText(getActivity(), "悬浮球已关闭", android.widget.Toast.LENGTH_SHORT).show();
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(getActivity())) {
                    android.widget.Toast.makeText(getActivity(), "请先开启悬浮窗权限", android.widget.Toast.LENGTH_SHORT).show();
                    startActivity(new android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                    return;
                }
            }
            getActivity().startService(new android.content.Intent(getActivity(), com.example.chatapp.FloatingBallService.class));
            sp.edit().putBoolean("floating_ball_enabled", true).apply();
            android.widget.Toast.makeText(getActivity(), "悬浮球已开启", android.widget.Toast.LENGTH_SHORT).show();
        }
        updateFloatingStatus();
    }

}
