package io.ristretto.decaptcha.solver.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebView;

import java.lang.reflect.Field;
import java.util.Arrays;


public class MyWebView extends WebView {
    private static final String TAG = "MyWebView";

    private void init() {
        try {
            Field f = getClass().getSuperclass().getDeclaredField("mProvider");
            f.setAccessible(true);
            Log.d(TAG, "Got mProvider: " + f.get(this));
        } catch (Exception e) {
            Log.e(TAG, "Reflection failed ", e);
        }
    }

    public MyWebView(Context context) {
        super(context);
        init();
    }

    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Deprecated
    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
        init();
    }

    @Override
    public void postUrl(String url, byte[] postData) {
        Log.d(TAG, "postUrl: " + url + " -- post data: " + Arrays.toString(postData));
        super.postUrl(url, postData);
    }
}
