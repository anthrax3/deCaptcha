package io.ristretto.decaptcha.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public interface Downloader {

    interface Result {
        int getStatusCode() throws IOException;

        @NonNull
        Map<String, String> getResponseHeaders() throws IOException;

        @NonNull
        InputStream getInputStream() throws IOException;

        @Nullable
        String getCharset() throws IOException;

        @Nullable
        String getContentType();

        @NonNull
        List<HttpCookie> getCookies() throws IOException;

        void close();
    }

    Result download(URL url) throws IOException;

    Result download(URL url, HttpHeaders headers) throws IOException;

    File download(File cacheDir, URL url, HttpHeaders headers) throws IOException;

    HttpURLConnection connect(URL url, HttpHeaders headers) throws IOException;
}
