package com.example.chatapp;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.URLEncoder;

public class FilePreviewActivity extends AppCompatActivity {
    private String fileUrl;
    private String fileName;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_preview);

        fileUrl = getIntent().getStringExtra("file_url");
        fileName = getIntent().getStringExtra("file_name");

        TextView tvTitle = findViewById(R.id.tv_preview_title);
        tvTitle.setText(fileName != null ? fileName : "文件预览");

        findViewById(R.id.btn_preview_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_preview_download).setOnClickListener(v -> downloadAndOpen());

        webView = findViewById(R.id.webview_preview);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    if (newProgress >= 100) progressBar.setVisibility(View.GONE);
                    else progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (tvStatus != null) tvStatus.setVisibility(View.GONE);
            }
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (tvStatus != null) {
                    tvStatus.setText("在线预览加载失败，请点击右上角下载后打开");
                    tvStatus.setVisibility(View.VISIBLE);
                }
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });

        if (fileUrl != null) {
            try {
                // 判断文件类型
                String lower = fileName != null ? fileName.toLowerCase() : "";
                if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") ||
                    lower.endsWith(".ppt") || lower.endsWith(".pptx") || lower.endsWith(".xls") ||
                    lower.endsWith(".xlsx") || lower.endsWith(".txt")) {
                    // 用微软 Office Online 预览
                    String encodedUrl = URLEncoder.encode(fileUrl, "UTF-8");
                    String viewerUrl = "https://view.officeapps.live.com/op/view.aspx?src=" + encodedUrl;
                    if (tvStatus != null) {
                        tvStatus.setText("正在加载预览...");
                        tvStatus.setVisibility(View.VISIBLE);
                    }
                    webView.loadUrl(viewerUrl);
                } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                           lower.endsWith(".gif") || lower.endsWith(".webp")) {
                    // 图片直接显示
                    String html = "<html><body style='margin:0;padding:0;background:#000;display:flex;align-items:center;justify-content:center;min-height:100vh;'><img src='" + fileUrl + "' style='max-width:100%;max-height:100vh;object-fit:contain;'/></body></html>";
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                } else {
                    // 其他文件提示下载
                    if (tvStatus != null) {
                        tvStatus.setText("此文件类型不支持在线预览，请点击右上角下载后打开");
                        tvStatus.setVisibility(View.VISIBLE);
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "预览失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void downloadAndOpen() {
        if (fileUrl == null) return;
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle(fileName != null ? fileName : "文件下载");
            request.setDescription("正在下载...");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName != null ? fileName : "download");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "开始下载，完成后点击通知打开", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
