package io.ristretto.decaptcha.solver.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jetbrains.annotations.Contract;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.Normalizer;
import java.util.Formatter;

import info.guardianproject.netcipher.NetCipher;
import io.ristretto.decaptcha.R;
import io.ristretto.decaptcha.data.ReCaptcha;
import io.ristretto.decaptcha.net.Downloader;
import io.ristretto.decaptcha.solver.Helper;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static io.ristretto.decaptcha.solver.Helper.PROTOCOLS_HTTP_AND_HTTPS;

/**
 * A placeholder fragment containing a simple view.
 */
public class ReCaptchaSolverFragment extends CaptchaSolverFragment<ReCaptcha> {

    private static final String TAG = "ReCaptchaSolverFragment";
    private WebView mWebView;
    private boolean mIsWebViewAvailable;

    public ReCaptchaSolverFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_solver, container, false);
        mWebView = (WebView) view.findViewById(R.id.webview);
        WebSettings websettings = mWebView.getSettings();
        websettings.setJavaScriptCanOpenWindowsAutomatically(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            websettings.setAllowFileAccessFromFileURLs(false);
        }
        websettings.setDomStorageEnabled(false);
        websettings.setGeolocationEnabled(false);
        //websettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        mIsWebViewAvailable = true;
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        mWebView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        mWebView.onPause();
        super.onPause();
    }

    @Nullable
    @Contract(pure = true)
    private WebView getWebView() {
        if(mIsWebViewAvailable) {
            return mWebView;
        } else {
            return null;
        }
    }

    @Override
    protected ReCaptcha receiverCaptcha(final @NonNull File cacheDir, final @NonNull Downloader downloader, final @NonNull Uri uri) throws IOException {
        URL url = Helper.uriToURL(uri, PROTOCOLS_HTTP_AND_HTTPS);
        Document document = null;
        Downloader.Result result = downloader.download(url);
        /*      if(result.getStatusCode() == HttpsURLConnection.HTTP_MOVED_PERM || result.getStatusCode() == HttpsURLConnection.HTTP_MOVED_TEMP) {
            String location = result.getResponseHeaders().get("location");
            Log.d(TAG, "Got redirection to " + location);
            if(location == null) {
                throw new IOException("Moved but no location: " + result);
            }
            result = downloader.download(new URL(location));
        }*/
        Log.d(TAG, "HTTP code: " + result.getStatusCode());
        document = Jsoup.parse(result.getInputStream(), result.getCharset(), uri.toString());
        System.err.println(document.html());
        Elements captchaContainers = document.getElementsByAttribute("data-stoken");
        if(!captchaContainers.isEmpty()) {
            for(Element element: captchaContainers) {
                String siteKey = element.attr("data-sitekey");
                if(!siteKey.isEmpty()) {
                    String stoken = element.attr("data-stoken");
                    return new ReCaptcha(siteKey, url.toString(), stoken);
                }
            }
        }

        captchaContainers = document.getElementsByClass("g-recaptcha");
        String siteKey = null;
        for (Element container : captchaContainers) {
            siteKey = container.attr("data-sitekey");
            if (!siteKey.isEmpty()) {
                break;
            }
        }
        if (siteKey == null || siteKey.isEmpty()) {
            return null;
        }
        return new ReCaptcha(siteKey, url.toString());
    }

    private String getMinimalHtmlContent(ReCaptcha captcha) {
        InputStream inputStream = getResources().openRawResource(R.raw.recaptcha_html);
        String content = toString(inputStream, "utf-8");
        Formatter formatter = new Formatter();
        return formatter.format(content, captcha.getSiteKey(), captcha.getSToken()).toString();
    }

    private static String toString(InputStream inputStream, String charsetName) {
        java.util.Scanner scanner = new java.util.Scanner(inputStream, charsetName).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private String getMinimalJsContent(ReCaptcha captcha) {
        InputStream inputStream = getResources().openRawResource(R.raw.recaptcha_js);
        String content = toString(inputStream, "utf-8");
        Formatter formatter = new Formatter();
        return formatter.format(content, captcha.getSiteKey()).toString();
    }

    private static class JSDataCallback {
        public static final String CALLBACK_NAME = "callbackObject";
        @JavascriptInterface
        public void dataCallback(String data) {
            System.out.println("Data: " + data);
        }
    }

    private void loadCaptcha(ReCaptcha captcha, WebView webview, boolean useJavascript) {
        String content;
        if(useJavascript && !captcha.hasSToken()) {
            webview.getSettings().setJavaScriptEnabled(true);
            content = getMinimalJsContent(captcha);
        } else {
            webview.getSettings().setJavaScriptEnabled(false);
            webview.setWebViewClient(new HtmlOnlyWebViewClient());
            content = getMinimalHtmlContent(captcha);
            System.out.println(content);
        }
        webview.loadDataWithBaseURL(captcha.getUrl(), content, "text/html",
                "utf-8", captcha.getUrl());
    }




    @Override
    protected void onCaptchaReceived(@NonNull ReCaptcha captcha) {
        super.onCaptchaReceived(captcha);
        WebView webview = getWebView();
        if(webview == null) throw new RuntimeException("Webview not avaiable");
        webview.setVisibility(View.VISIBLE);
        loadCaptcha(captcha, webview, true);
    }

    private void hideWebView() {
        WebView webView = getWebView();
        if(webView == null) return;
        webView.pauseTimers();
        webView.getSettings().setJavaScriptEnabled(false);
        //webView.destroy();
    }


    private void onCaptchaResult(String method, String url, String data) {

    }

    class ChromClient extends WebChromeClient {
    }

    class HtmlOnlyWebViewClient extends WebViewClient {

        private boolean shouldOverrideUrl(WebView view, Uri uri) {
            if (uri.getPath().startsWith("cdn-cgi/l/chk_captcha")) {
                String data = uri.getQueryParameter("g-recaptcha-response");
                onCaptchaResult("GET", uri.toString(), data);
                return true;
            }
            return false;
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return shouldOverrideUrl(view, Uri.parse(url));
        }

        @RequiresApi(LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrl(view, request.getUrl());
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            Log.e(TAG, "onLoadResource: " + url);
            super.onLoadResource(view, url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Log.e(TAG, request.getMethod() + " " + request.getUrl().toString());
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            Log.d(TAG, url);
            return super.shouldInterceptRequest(view, url);
        }
    }
}
