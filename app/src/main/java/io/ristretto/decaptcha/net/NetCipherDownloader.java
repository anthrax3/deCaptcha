package io.ristretto.decaptcha.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

import info.guardianproject.netcipher.NetCipher;
import io.ristretto.decaptcha.util.IOHelper;


public class NetCipherDownloader implements Downloader {

    private static final String TAG = "NetCipherDownloader";

    private static class Result implements Downloader.Result {
        private final HttpURLConnection httpUrlConnection;

        private Result(HttpURLConnection connect) {
            this.httpUrlConnection = connect;
        }

        @Override
        public int getStatusCode() throws IOException {
            return httpUrlConnection.getResponseCode();
        }

        @NonNull
        @Override
        public Map<String, String> getResponseHeaders() {
            HashMap<String, String> headers = new HashMap<>();
            for(Map.Entry<String, List<String>> entry: httpUrlConnection.getHeaderFields().entrySet()) {
                headers.put(entry.getKey(), entry.getValue().get(0));
            }
            return headers;
        }

        @NonNull
        @Override
        public InputStream getInputStream() throws IOException {
            if(getStatusCode() > 400) {
                return httpUrlConnection.getErrorStream();
            }
            return httpUrlConnection.getInputStream();
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
                throw new RuntimeException("Couldn't load coookies", e);
            }
            CookieStore cookieStore = cookieManager.getCookieStore();
            return cookieStore.getCookies();
        }

        @Override
        public void close() {
            httpUrlConnection.disconnect();
        }
    }

    @Override
    public Downloader.Result download(URL url) throws IOException {
        return download(url, new HttpHeaders());
    }

    @Override
    public Downloader.Result download(URL url, HttpHeaders headers) throws IOException {
        HttpURLConnection httpURLConnection = connect(url, headers);
        return new Result(httpURLConnection);
    }

    @Override
    public File download(File cacheDir, URL url, HttpHeaders headers) throws IOException {
        File output = File.createTempFile(TAG, null, cacheDir);
        HttpURLConnection httpURLConnection = connect(url, headers);
        InputStream inputStream = httpURLConnection.getInputStream();
        OutputStream outputStream = new FileOutputStream(output);
        IOHelper.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        return output;
    }

    @Override
    public HttpURLConnection connect(URL url, HttpHeaders headers) throws IOException {
        HttpURLConnection httpURLConnection = NetCipher.getHttpURLConnection(url);
        httpURLConnection.setInstanceFollowRedirects(true);
        headers.putHeaders(httpURLConnection);
        httpURLConnection.connect();
        return httpURLConnection;
    }
}
