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
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    public static MainActivity instance;
    public static String TARGET_URL = null;

    private static final String DEFAULT_URL = "https://indirimtrendim.com";
    public WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        // Uçtan uca (Full Bleed) tam şeffaf durum çubuğu
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);

        // Android 15/16 Edge-to-Edge Güvenli Alanı:
        // Üst = 0: Web sitesinin kendi krem/amber rengi saatin arkasına kadar pürüzsüz uzanır (Full Bleed)
        // Alt = insets.bottom: Alt menü butonları telefonun Home/Back tuşlarının üstünde tertemiz durur
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.swipeRefresh), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars() |
                androidx.core.view.WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(insets.left, 0, insets.right, insets.bottom);
            return windowInsets;
        });

        // Durum çubuğu ikonlarını koyu (siyah) yapıyoruz (açık zemin üzerinde net okunsun)
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
            .setAppearanceLightStatusBars(true);

        // Bildirim İzni İsteme (Android 13+)
        OneSignal.getNotifications().requestPermission(true, Continue.with(r -> {}));

        setupWebView();

        // Geri tuşu yönetimi
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

        // Swipe-to-refresh: Çekip bırakıldığında önbelleği de temizleyip en güncel web içeriğini çeker
        swipeRefresh.setOnRefreshListener(() -> {
            if (webView != null) {
                webView.clearCache(true);
                webView.reload();
            }
        });
        swipeRefresh.setColorSchemeColors(0xFFEE4D2D, 0xFF232F3E);
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> webView != null && webView.canScrollVertically(-1));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                swipeRefresh.setEnabled(!webView.canScrollVertically(-1));
            });
        }

        // Açılış URL'i çözümleme (Bildirim tıklaması, deep link veya ana sayfa)
        String openUrl = resolveTargetUrl(getIntent());
        webView.loadUrl(openUrl);
    }

    public void loadTargetUrl(String url) {
        if (url != null && !url.trim().isEmpty() && webView != null) {
            webView.loadUrl(url);
        }
    }

    private String resolveTargetUrl(Intent intent) {
        if (TARGET_URL != null && !TARGET_URL.trim().isEmpty()) {
            String url = TARGET_URL;
            TARGET_URL = null;
            return url;
        }
        if (intent != null) {
            if (intent.getData() != null) {
                return intent.getData().toString();
            }
            if (intent.hasExtra("target_url")) {
                String u = intent.getStringExtra("target_url");
                if (u != null && !u.trim().isEmpty()) return u;
            }
            if (intent.hasExtra("onesignal_data")) {
                try {
                    JSONObject obj = new JSONObject(intent.getStringExtra("onesignal_data"));
                    if (obj.has("custom")) {
                        JSONObject custom = new JSONObject(obj.getString("custom"));
                        if (custom.has("a")) {
                            JSONObject a = custom.getJSONObject("a");
                            if (a.has("url")) return a.getString("url");
                        }
                        if (custom.has("u")) return custom.getString("u");
                    }
                } catch (Exception ignored) {}
            }
        }
        return DEFAULT_URL;
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

        // Dokunma ve Tıklama Hızı Optimizasyonu (300ms gecikmeyi sıfırlar, anında tepki verir)
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setNeedInitialFocus(false);

        // Tıklamaların ve dokunmaların SwipeRefresh tarafından yutulmasını önler:
        // Sadece sayfa en tepedeyken aşağı çekme yenilemesi çalışır, sayfa içindeyken tıklamalar anında tepki verir.
        webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            swipeRefresh.setEnabled(scrollY == 0);
        });

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
        String target = resolveTargetUrl(intent);
        loadTargetUrl(target);
    }

    @Override
    protected void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }
}
