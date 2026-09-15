package com.example.chatapp;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashSet;
import java.util.Set;
public class NovelActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    private TextView errorText;
    // M7: WebView 允许加载的域名白名单
    private static final Set<String> ALLOWED_HOSTS = new HashSet<>();
    static {
        ALLOWED_HOSTS.add("morax.kdns.fr");
    }
    private boolean isAllowedHost(String host) {
        if (host == null) return false;
        for (String allowed : ALLOWED_HOSTS) {
            if (host.equalsIgnoreCase(allowed) || host.endsWith("." + allowed)) return true;
        }
        return false;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel);
        TextView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
        errorText = findViewById(R.id.error_text);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // M6: 禁止混合内容，强制 HTTPS
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        // M7: 关闭 file/content 访问
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // M7: 仅白名单域名在 WebView 内加载，其余用外部浏览器打开
                Uri url = request.getUrl();
                if (isAllowedHost(url.getHost())) {
                    view.loadUrl(url.toString());
                    return true;
                }
                try {
                    Intent ext = new Intent(Intent.ACTION_VIEW, url);
                    startActivity(ext);
                } catch (Exception e) {
                    Toast.makeText(NovelActivity.this, "无法打开链接", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                errorText.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    webView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText("加载失败，点击重试");
                    errorText.setOnClickListener(v -> {
                        errorText.setVisibility(View.GONE);
                        webView.setVisibility(View.VISIBLE);
                        webView.reload();
                    });
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });
        try {
            webView.loadUrl("https://morax.kdns.fr/");
        } catch (Exception e) {
            Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
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
