package com.example.chatapp;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class ImageViewerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        ImageView iv = findViewById(R.id.iv_image);
        String url = getIntent().getStringExtra("image_url");
        if (url != null) {
            Glide.with(this).load(url).into(iv);
        }
        iv.setOnClickListener(v -> finish());
    }
}
