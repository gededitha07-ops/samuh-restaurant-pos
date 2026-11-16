package com.samuhrestaurant.pos;

import org.json.JSONArray;
import org.json.JSONObject;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.print.PrintManager;
import android.content.Context;
import android.print.PrintDocumentAdapter;
import android.content.Intent;
import android.net.Uri;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    
private WebView webView;

private static final int REQUEST_BACKUP = 1001;
private static final int REQUEST_RESTORE = 1002;
private static final int REQUEST_BLUETOOTH_PERMISSION = 1003;

private String pendingBackupJson;

private BluetoothAdapter bluetoothAdapter;
private BluetoothSocket bluetoothSocket;
private BluetoothDevice bluetoothDevice;

private final UUID printerUUID = UUID.fromString(
    "00001101-0000-1000-8000-00805F9B34FB"
);


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

                @JavascriptInterface
                public void printReceipt() {
                    runOnUiThread(() -> {
                        PrintManager printManager =
                            (PrintManager) getSystemService(
                                Context.PRINT_SERVICE
                            );

                        PrintDocumentAdapter adapter =
                            webView.createPrintDocumentAdapter(
                                "Samuh-Restaurant-Receipt"
                            );

                        printManager.print(
                            "Samuh-Restaurant-Receipt",
                            adapter,
                            null
                        );
                    });
                }
               @JavascriptInterface
public void backupData(String json) {
    runOnUiThread(() -> {
        pendingBackupJson = json;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(
            Intent.EXTRA_TITLE,
            "Samuh-Restaurant-Backup.json"
        );

        startActivityForResult(intent, REQUEST_BACKUP);
    });
}

@JavascriptInterface
public void restoreData() {
    runOnUiThread(() -> {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");

        startActivityForResult(intent, REQUEST_RESTORE);
    });
} 
            },
            "Android"
        );

        setContentView(webView);
        siapkanBluetooth();

        webView.loadUrl("file:///android_asset/index.html");
    }
    
private void siapkanBluetooth() {

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    if (bluetoothAdapter == null) {
        Toast.makeText(
            this,
            "Tablet tidak mendukung Bluetooth.",
            Toast.LENGTH_LONG
        ).show();
        return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

        if (checkSelfPermission(
            Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT
                },
                REQUEST_BLUETOOTH_PERMISSION
            );

            return;
        }
    }

    Toast.makeText(
        this,
        "Bluetooth siap digunakan.",
        Toast.LENGTH_SHORT
    ).show();
}

@Override
protected void onActivityResult(
    int requestCode,
    int resultCode,
    Intent data
) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode != RESULT_OK || data == null) {
        return;
    }

    Uri uri = data.getData();

    if (uri == null) {
        return;
    }

    try {

        if (requestCode == REQUEST_BACKUP) {

            OutputStream output =
                getContentResolver().openOutputStream(uri);

            if (output == null) {
                throw new IOException("Cannot open backup file.");
            }

            output.write(
                pendingBackupJson.getBytes(StandardCharsets.UTF_8)
            );

            output.close();

            webView.evaluateJavascript(
                "window.onBackupSaved && window.onBackupSaved()",
                null
            );
        }

        else if (requestCode == REQUEST_RESTORE) {

            InputStream input =
                getContentResolver().openInputStream(uri);

            if (input == null) {
                throw new IOException("Cannot read backup file.");
            }

            ByteArrayOutputStream buffer =
                new ByteArrayOutputStream();

            byte[] bytes = new byte[4096];
            int length;

            while ((length = input.read(bytes)) != -1) {
                buffer.write(bytes, 0, length);
            }

            input.close();

            String json =
                buffer.toString("UTF-8");

            String safeJson =
                JSONObject.quote(json);

            webView.evaluateJavascript(
                "window.onBackupRestored(" + safeJson + ")",
                null
            );
        }

    } catch (Exception e) {

        String errorMessage =
            JSONObject.quote(e.getMessage() == null
                ? "Unknown error"
                : e.getMessage());

        webView.evaluateJavascript(
            "window.onBackupError && window.onBackupError(" +
            errorMessage + ")",
            null
        );
    }
}
    
@Override
public void onRequestPermissionsResult(
    int requestCode,
    String[] permissions,
    int[] grantResults
) {
    super.onRequestPermissionsResult(
        requestCode,
        permissions,
        grantResults
    );

    if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {

        if (grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(
                this,
                "Izin Bluetooth berhasil diberikan.",
                Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                this,
                "Izin Bluetooth belum diberikan.",
                Toast.LENGTH_LONG
            ).show();
        }
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
}
