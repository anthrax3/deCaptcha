package io.ristretto.decaptcha.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.guardianproject.netcipher.NetCipher;


public class NetCipherDownloader implements Downloader {
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

        @Override
        public void close() {
            httpUrlConnection.disconnect();
        }
    }

    @Override
    public Result download(URL url) throws IOException {
        HttpURLConnection httpURLConnection = connect(url);
        httpURLConnection.setInstanceFollowRedirects(true);
        return new Result(httpURLConnection);
    }

    @Override
    public HttpURLConnection connect(URL url) throws IOException {
        return NetCipher.getHttpURLConnection(url);
    }
}
