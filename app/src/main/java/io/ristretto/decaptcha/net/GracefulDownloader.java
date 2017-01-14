package io.ristretto.decaptcha.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import io.ristretto.decaptcha.util.IOHelper;


/**
 * Downloader which handles the most HTTP(S) default headers.
 *
 * The following headers are set:
 * <pre>
 *     * Accept
 *     * User-Agent
 *     * Accept-Language
 *     * Accept-Encoding
 * </pre>
 *
 * It also handles GZIP-ed responses.
 */
public class GracefulDownloader implements Downloader {

    private static final String TAG = "GracefulDownloader";


    public static final String HEADER_REFERER = "Referer";

    public static final String DEFAULT_ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; rv:45.0) Gecko/20100101 Firefox/45.0";
    public static final String DEFAULT_LANGUAGE = "en-US,en;q=0.5";
    public static final String DEFAULT_ACCEPT_ENCODING = "gzip, deflate";

    private final Connector mConnector;
    private final String mUserAgent;
    private final String mAcceptHeader;

    public GracefulDownloader(Connector connector) {
        this(connector, null, null);
    }

    public GracefulDownloader(Connector connector, @Nullable String userAgent, @Nullable String acceptHeader) {
        if(connector == null) throw new NullPointerException("connector is null");
        this.mConnector = connector;
        if(userAgent == null) {
            this.mUserAgent = DEFAULT_USER_AGENT;
        } else {
            this.mUserAgent = userAgent;
        }
        if(acceptHeader == null) {
            this.mAcceptHeader = DEFAULT_ACCEPT_HEADER;
        } else {
            this.mAcceptHeader = acceptHeader;
        }
    }

    @NonNull
    private static HttpHeaders copyOrCreateHeaders(final @Nullable HttpHeaders providedHeaders) {
        HttpHeaders result;
        if(providedHeaders == null) {
            result = new HttpHeaders();
        } else {
            result = new HttpHeaders(providedHeaders);
        }
        return result;
    }

    private static void putHeaderIfNotExists(HttpHeaders headers, String name, String value) {
        if(!headers.containsKey(name)) {
            headers.put(name, value);
        }
    }

    private void setDefaultHeaders(HttpHeaders headers) {
        putHeaderIfNotExists(headers, HttpHeaders.HEADER_USER_AGENT, mUserAgent);
        putHeaderIfNotExists(headers, HttpHeaders.ACCEPT, mAcceptHeader);
        // TODO: make configurable
        putHeaderIfNotExists(headers, HttpHeaders.ACCEPT_LANGUAGE, DEFAULT_LANGUAGE);
        putHeaderIfNotExists(headers, HttpHeaders.ACCEPT_ENCODING, DEFAULT_ACCEPT_ENCODING);
    }

    private void setDefaultPostHeaders(HttpHeaders headers, byte[] postData) {
        putHeaderIfNotExists(headers, HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
        putHeaderIfNotExists(headers, HttpHeaders.CONTENT_LENGTH, Integer.toString(postData.length));
    }

    @Override
    public Downloader.Result download(URL url) throws IOException {
        return download(url, null);
    }

    @Override
    public Downloader.Result download(URL url, final @Nullable HttpHeaders providedHeaders) throws IOException {
        HttpHeaders httpHeaders = copyOrCreateHeaders(providedHeaders);
        setDefaultHeaders(httpHeaders);
        HttpURLConnection httpURLConnection = mConnector.connect("GET", url, httpHeaders);
        return new Result(httpURLConnection);
    }

    @Override
    public Downloader.Result post(URL url, HttpHeaders headers, byte[] postData) throws IOException {
        setDefaultHeaders(headers);
        setDefaultPostHeaders(headers, postData);
        Log.d(TAG, "Post data: " + new String(postData));
        HttpURLConnection httpURLConnection = mConnector.connect("POST", url, headers);
        OutputStream outputStream = httpURLConnection.getOutputStream();
        outputStream.write(postData);
        return new Result(httpURLConnection);
    }

    @Override
    public File download(File cacheDir, URL url, HttpHeaders headers) throws IOException {
        File output = File.createTempFile(TAG, null, cacheDir);
        setDefaultHeaders(headers);
        HttpURLConnection httpURLConnection = mConnector.connect("GET", url, headers);
        InputStream inputStream = httpURLConnection.getInputStream();
        OutputStream outputStream = new FileOutputStream(output);
        IOHelper.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        return output;
    }

    private static class Result implements Downloader.Result {
        private final HttpURLConnection httpUrlConnection;
        private final HttpHeaders httpHeaders;

        private Result(HttpURLConnection connect) {
            this.httpUrlConnection = connect;
            this.httpHeaders  = HttpHeaders.fromMultiHeaders(httpUrlConnection.getHeaderFields());
        }

        @Override
        public int getStatusCode() throws IOException {
            return httpUrlConnection.getResponseCode();
        }

        @NonNull
        @Override
        public Map<String, String> getResponseHeaders() {
            return new HashMap<>(httpHeaders);
        }

        @NonNull
        @Override
        public InputStream getInputStream() throws IOException {
            InputStream inputStream;
            if(getStatusCode() >= 400) {
                inputStream = httpUrlConnection.getErrorStream();
            } else {
                inputStream = httpUrlConnection.getInputStream();
            }
            if("gzip".equals(httpHeaders.get(HttpHeaders.CONTENT_ENCODING))) {
                inputStream = addGzipDecodeLayer(inputStream);
            }
            return inputStream;
        }

        private InputStream addGzipDecodeLayer(InputStream inputStream) throws IOException {
            return new GZIPInputStream(inputStream);
        }

        @Nullable
        @Override
        public String getCharset() {
            final String contentType = getContentType();
            if(contentType == null) return null;
            String[] values = contentType.split(";"); // values.length should be 2
            String charset = null;

            for (String value : values) {
                value = value.trim();

                if (value.toLowerCase().startsWith("charset=")) {
                    charset = value.substring("charset=".length());
                    charset = charset.trim();
                    if(charset.isEmpty()) {
                        charset = null;
                    } else {
                        break;
                    }
                }
            }
            return charset;
        }

        @Nullable
        @Override
        public String getContentType() {
            return httpUrlConnection.getContentType();
        }

        @NonNull
        @Override
        public List<HttpCookie> getCookies() throws IOException {
            CookieManager cookieManager = new CookieManager();
            try {
                cookieManager.put(httpUrlConnection.getURL().toURI(), httpUrlConnection.getHeaderFields());
            } catch (URISyntaxException e) {
                throw new RuntimeException("Couldn't load cookies", e);
            }
            CookieStore cookieStore = cookieManager.getCookieStore();
            return cookieStore.getCookies();
        }

        @Override
        public void close() {
            httpUrlConnection.disconnect();
        }
    }

}
