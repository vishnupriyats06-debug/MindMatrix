package com.mindmatrix.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    // ⚡ Live Cloud Database Backend URL (Synchronized with Web Application Database)
    private static final String APP_URL = "https://mindmatrix-p2z8.onrender.com/login.html";
    private static final int PERMISSION_REQUEST_CODE = 101;

    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Request runtime Notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }

        // Enable cookies and session persistence
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        // Configure WebView settings for online live database operation
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        // Keep navigation inside WebView (prevent opening Chrome browser)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                swipeRefreshLayout.setRefreshing(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                swipeRefreshLayout.setRefreshing(false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame()) {
                    String errorHtml = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'/></head>"
                        + "<body style='background:#050814;color:#f0f4ff;font-family:sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;height:90vh;text-align:center;padding:20px;box-sizing:border-box;margin:0;'>"
                        + "<div style='font-size:36px;margin-bottom:12px;'>⚡</div>"
                        + "<h2 style='color:#8b5cf6;margin:0 0 10px;'>Connecting to MindMatrix</h2>"
                        + "<p style='color:#94a3b8;max-width:320px;font-size:14px;line-height:1.5;margin:0 0 24px;'>Unable to reach server. Please ensure internet access is available and try again.</p>"
                        + "<button onclick='location.reload()' style='padding:12px 32px;background:linear-gradient(135deg,#8b5cf6,#06b6d4);border:none;border-radius:12px;color:white;font-weight:bold;font-size:15px;cursor:pointer;'>Retry</button>"
                        + "</body></html>";
                    view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        // Add Javascript Interface for Web-to-Native communication
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

        // Schedule background daily alarm at 7:00 PM
        StreakAlarmScheduler.scheduleDailyReminder(this);

        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        // Load MindMatrix App from local assets
        if (savedInstanceState == null) {
            webView.loadUrl(APP_URL);
        }
    }

    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void setStreakReminderEnabled(boolean enabled) {
            runOnUiThread(() -> {
                if (enabled) {
                    StreakAlarmScheduler.scheduleDailyReminder(MainActivity.this);
                    StreakReminderReceiver.showNotification(
                            MainActivity.this,
                            "🔥 MindMatrix Streak Alert Active!",
                            "Daily reminders set for 7:00 PM. We will alert you before your streak expires!"
                    );
                } else {
                    StreakAlarmScheduler.cancelDailyReminder(MainActivity.this);
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void showNotification(String title, String message) {
            runOnUiThread(() -> {
                StreakReminderReceiver.showNotification(MainActivity.this, title, message);
            });
        }

        @android.webkit.JavascriptInterface
        public void requestNotificationPermission() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
                    }
                }
            });
        }
    }

    // Handle Phone Back Button Navigation
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
