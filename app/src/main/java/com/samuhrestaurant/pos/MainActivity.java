package com.samuhrestaurant.pos;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.print.PrintManager;
import android.content.Context;
import android.print.PrintDocumentAdapter;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(
            new Object() {
                @JavascriptInterface
                public void printReport() {
                    runOnUiThread(() -> {
                        PrintManager printManager =
                            (PrintManager) getSystemService(
                                Context.PRINT_SERVICE
                            );

                        PrintDocumentAdapter adapter =
                            webView.createPrintDocumentAdapter(
                                "Samuh-Restaurant-Report"
                            );

                        printManager.print(
                            "Samuh-Restaurant-Report",
                            adapter,
                            null
                        );
                    });
                }
            },
            "Android"
        );

        setContentView(webView);

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
