package com.example.chatapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.api.ApiClient;
import com.example.chatapp.util.SharedPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PostMomentActivity extends AppCompatActivity {
    private EditText etContent;
    private LinearLayout layoutImagePreview;
    private List<String> selectedImages = new ArrayList<>();
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_moment);

        etContent = findViewById(R.id.et_content);
        layoutImagePreview = findViewById(R.id.layout_image_preview);
        TextView btnCancel = findViewById(R.id.btn_cancel);
        TextView btnPost = findViewById(R.id.btn_post);
        TextView btnAddImage = findViewById(R.id.btn_add_image);

        btnCancel.setOnClickListener(v -> finish());
        btnPost.setOnClickListener(v -> postMoment());
        btnAddImage.setOnClickListener(v -> pickImage());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                InputStream is = getContentResolver().openInputStream(uri);
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                byte[] buffer = new byte[4096];
                                int len;
                                while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
                                is.close();
                                String base64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
                                selectedImages.add(base64);
                                addImagePreview(uri);
                            } catch (Exception e) {
                                Toast.makeText(this, "读取图片失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void addImagePreview(Uri uri) {
        ImageView iv = new ImageView(this);
        iv.setImageURI(uri);
        iv.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setPadding(4, 0, 4, 0);
        layoutImagePreview.addView(iv);
    }

    private void postMoment() {
        String text = etContent.getText().toString().trim();
        if (text.isEmpty() && selectedImages.isEmpty()) {
            Toast.makeText(this, "请输入内容或添加图片", Toast.LENGTH_SHORT).show();
            return;
        }
        String token = SharedPrefs.getToken(this);
        JSONArray images = new JSONArray();
        for (String img : selectedImages) images.put(img);
        ApiClient.postMoment(token, text, images, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> {
                    Toast.makeText(PostMomentActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PostMomentActivity.this, "发布失败: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
