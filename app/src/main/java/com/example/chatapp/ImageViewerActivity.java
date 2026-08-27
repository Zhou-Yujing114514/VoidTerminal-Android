package com.example.chatapp;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;

public class ImageViewerActivity extends AppCompatActivity {
    private ImageView iv;
    private Matrix matrix = new Matrix();
    private float scale = 1f;
    private float baseScale = 1f;
    private ScaleGestureDetector scaleDetector;
    private PointF lastPoint = new PointF();
    private boolean isDragging = false;
    private long lastClickTime = 0;
    private boolean isImageLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        iv = findViewById(R.id.iv_image);
        // 必须用 MATRIX 模式才能用 ImageMatrix 缩放
        iv.setScaleType(ImageView.ScaleType.MATRIX);

        String url = getIntent().getStringExtra("image_url");
        if (url != null) {
            Glide.with(this)
                .load(url)
                .into(new CustomViewTarget<ImageView, Drawable>(iv) {
                    @Override
                    public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                        iv.setImageDrawable(resource);
                        initMatrix();
                        isImageLoaded = true;
                    }
                    @Override
                    protected void onResourceCleared(Drawable placeholder) {
                        iv.setImageDrawable(placeholder);
                    }
                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        iv.setImageDrawable(errorDrawable);
                    }
                });
        }

        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (!isImageLoaded) return true;
                float factor = detector.getScaleFactor();
                float newScale = scale * factor;
                newScale = Math.max(baseScale * 0.5f, Math.min(newScale, baseScale * 5f));
                matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                scale = newScale;
                iv.setImageMatrix(matrix);
                return true;
            }
        });

        iv.setOnTouchListener((v, event) -> {
            if (!isImageLoaded) return true;
            scaleDetector.onTouchEvent(event);
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    lastPoint.set(event.getX(), event.getY());
                    isDragging = true;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    isDragging = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isDragging && scale > baseScale * 1.1f) {
                        float dx = event.getX() - lastPoint.x;
                        float dy = event.getY() - lastPoint.y;
                        matrix.postTranslate(dx, dy);
                        iv.setImageMatrix(matrix);
                        lastPoint.set(event.getX(), event.getY());
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    isDragging = false;
                    // 双击检测
                    long now = System.currentTimeMillis();
                    if (now - lastClickTime < 300) {
                        if (scale > baseScale * 1.1f) {
                            resetMatrix();
                        } else {
                            // 双击放大到2倍
                            float cx = iv.getWidth() / 2f;
                            float cy = iv.getHeight() / 2f;
                            matrix.postScale(2f, 2f, cx, cy);
                            scale *= 2f;
                            iv.setImageMatrix(matrix);
                        }
                        lastClickTime = 0;
                    } else {
                        lastClickTime = now;
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    isDragging = false;
                    break;
            }
            return true;
        });
    }

    private void initMatrix() {
        Drawable d = iv.getDrawable();
        if (d == null) return;
        int imgW = d.getIntrinsicWidth();
        int imgH = d.getIntrinsicHeight();
        int viewW = iv.getWidth();
        int viewH = iv.getHeight();
        if (imgW <= 0 || imgH <= 0 || viewW <= 0 || viewH <= 0) return;

        // 计算基础缩放：使图片适应屏幕
        float scaleX = (float) viewW / imgW;
        float scaleY = (float) viewH / imgH;
        baseScale = Math.min(scaleX, scaleY);
        scale = baseScale;

        // 居中显示
        float dx = (viewW - imgW * baseScale) / 2f;
        float dy = (viewH - imgH * baseScale) / 2f;

        matrix.reset();
        matrix.postScale(baseScale, baseScale);
        matrix.postTranslate(dx, dy);
        iv.setImageMatrix(matrix);
    }

    private void resetMatrix() {
        scale = baseScale;
        initMatrix();
    }
}
