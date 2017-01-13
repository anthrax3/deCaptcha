package io.ristretto.decaptcha.net;



import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpHeaders extends HashMap<String, String> {
    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_USER_AGENT = "User-Agent";

    public static final String ACCEPT_IMAGES = "image/png,image/*;q=0.8,*/*;q=0.5";
    public static final String DEFAULT_ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; rv:45.0) Gecko/20100101 Firefox/45.0";

    public HttpHeaders() {
        super();
    }

    public HttpHeaders(Map<String, String> responseHeaders) {
        super(responseHeaders.size());
        this.putAll(responseHeaders);
    }

    private static Map<String, List<String>> singleToMultiHeaders(Map<String, String> single) {
        HashMap<String, List<String>> multi = new HashMap<>(single.size());
        for(Entry<String, String> entry: single.entrySet()) {
            List<String> values = new ArrayList<>(1);
            values.add(entry.getValue());
            multi.put(entry.getKey(), values);
        }
        return multi;
    }

    public void putCookies(URI uri, CookieManager cookieManager) throws IOException {
        cookieManager.put(uri, singleToMultiHeaders(this));
    }

    public CookieStore getCookieStore(URI uri) throws IOException {
        CookieManager cookieManager = new CookieManager();
        putCookies(uri, cookieManager);
        return cookieManager.getCookieStore();
    }

    public void setReferer(String referer) {
        put(HEADER_REFERER, referer);
    }

    public void setUserAgent(String userAgent) {
        put(HEADER_USER_AGENT, userAgent);
    }

    public void putHeaders(HttpURLConnection httpURLConnection) {
        for(Entry<String, String> entry: entrySet()) {
            httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue());
        }
    }
}
