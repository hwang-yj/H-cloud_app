package com.amitshekhar.tflite;

import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * Created by Administrator on 2017-08-03.
 */

public class AndroidBridge {
    private final Handler handler = new Handler();
    private WebView mWebView;
    private DBHelper dbHelper;
    private  boolean newtwork;

    public AndroidBridge(WebView mWebView, DBHelper dbHelper, boolean newtwork) {
        this.mWebView = mWebView;
        this.dbHelper = dbHelper;
        this.newtwork = newtwork;
    }

    @JavascriptInterface
    public void requestNetwork() { // must be final
        handler.post(new Runnable() {
            public void run() {
                Log.d("HybridApp", "네트워크 상태 요청");
                mWebView.loadUrl("javascript:getNetwork("+newtwork+")");
            }
        });
    }



    @JavascriptInterface
    public void setMessage(final String arg) { // must be final
        handler.post(new Runnable() {
            public void run() {
                Log.d("HybridApp", "setMessage("+arg+")");
                mWebView.loadUrl("javascript:getAndroidMessage('ANDROID -> JAVASCRIPT CALL!!')");
            }
        });
    }


}
