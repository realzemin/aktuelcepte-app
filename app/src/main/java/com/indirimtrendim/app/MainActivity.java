package com.indirimtrendim.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.onesignal.OneSignal;
import com.onesignal.Continue;

public class MainActivity extends AppCompatActivity {

    private static final String DEFAULT_URL = "https://indirimtrendim.com";
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);

        // Bildirim İzni İsteme (Android 13+)
        OneSignal.getNotifications().requestPermission(true, Continue.with(r -> {}));

        setupWebView();

        // Geri tuşu yönetimi (Sayfada geri gitme)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    finish();
                }
            }
        });

        // Swipe-to-refresh
        swipeRefresh.setOnRefreshListener(() -> {
            if (webView != null) webView.reload();
        });
        swipeRefresh.setColorSchemeColors(0xFFEE4D2D, 0xFF232F3E);
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> webView != null && webView.canScrollVertically(-1));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                swipeRefresh.setEnabled(!webView.canScrollVertically(-1));
            });
        }

        // Açılış URL'i
        String openUrl = DEFAULT_URL;
        if (getIntent() != null && getIntent().getData() != null) {
            openUrl = getIntent().getData().toString();
        }
        webView.loadUrl(openUrl);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " IndirimTrendimApp/1.0");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Sitenin kendi linkleri uygulama içinde kalsın
                if (url.startsWith("https://indirimtrendim.com") || url.startsWith("http://indirimtrendim.com")) {
                    return false;
                }
                // Harici linkler (WhatsApp, Telegram, Mağaza linkleri) dış uygulamada açılsın
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getData() != null && webView != null) {
            String targetUrl = intent.getData().toString();
            webView.loadUrl(targetUrl);
        }
    }
}
