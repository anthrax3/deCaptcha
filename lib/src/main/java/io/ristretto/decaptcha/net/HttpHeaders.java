package io.ristretto.decaptcha.net;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class HttpHeaders extends TreeMap<String, String> {
    private static final String TAG = "HttpHeaders";

    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String HEADER_COOKIE = "Cookie";
    public static final String ACCEPT_IMAGES = "image/png,image/*;q=0.8,*/*;q=0.5";


    public HttpHeaders() {
        super();
    }

    public HttpHeaders(Map<String, String> responseHeaders) {
        super(String.CASE_INSENSITIVE_ORDER);
        putAll(responseHeaders);
    }

    public static HttpHeaders fromMultiHeaders(Map<String, List<String>> headerFields) {
        HttpHeaders headers = new HttpHeaders();
        for(Map.Entry<String, List<String>> entry: headerFields.entrySet()) {
            List<String> values = entry.getValue();
            if(values.size() > 1) {
                // TODO: throw?
                //Log.w(TAG, "Ignoring additional values for header " + entry.getKey()  + ": " + Arrays.toString(values.toArray()));
            }
            headers.put(entry.getKey(), entry.getValue().get(0));
        }
        return headers;
    }

    private static Map<String, List<String>> singleToMultiHeaders(Map<String, String> single) {
        HashMap<String, List<String>> multi = new HashMap<>(single.size());
        for(Map.Entry<String, String> entry: single.entrySet()) {
            List<String> values = new ArrayList<>(1);
            values.add(entry.getValue());
            multi.put(entry.getKey(), values);
        }
        return multi;
    }

    public void putCookies(URI uri, CookieManager cookieManager) throws IOException {
        cookieManager.put(uri, singleToMultiHeaders(this));
    }

    public CookieStore getCookieStore(String uri) throws IOException {
        CookieManager cookieManager = new CookieManager();
        try {
            putCookies(new URI(uri), cookieManager);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        return cookieManager.getCookieStore();
    }

    public void setReferer(String referer) {
        put(HEADER_REFERER, referer);
    }

    public void setReferer(URL referer) {
        setReferer(referer.toString());
    }

    public void setCookies(Iterable<HttpCookie> cookies) {
        StringBuilder builder = new StringBuilder();
        for(HttpCookie cookie: cookies) {
            if(builder.length() > 0) {
                builder.append(';');
            }
            builder.append(cookie.getName())
                    .append('=')
                    .append(cookie.getValue());
        }
        setCookies(builder.toString());
    }

    private void setCookies(String cookies) {
        put(HEADER_COOKIE, cookies);
    }
}
