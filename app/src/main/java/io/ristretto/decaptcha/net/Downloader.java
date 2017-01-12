package io.ristretto.decaptcha.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

        void close();
    }

    Result download(URL url) throws IOException;

    HttpURLConnection connect(URL url) throws IOException;
}
