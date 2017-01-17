package io.ristretto.decaptcha.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.URL;
import java.util.List;
import java.util.Map;

public interface Downloader {

    interface Result {
        int getStatusCode() throws IOException;

        Map<String, String> getResponseHeaders() throws IOException;

        InputStream getInputStream() throws IOException;

        String getCharset() throws IOException;

        String getContentType();

        List<HttpCookie> getCookies() throws IOException;

        void close();
    }

    Result download(URL url) throws IOException;

    Result download(URL url, HttpHeaders headers) throws IOException;

    Result post(URL url, HttpHeaders headers, byte[] postData) throws IOException;

    File download(File cacheDir, URL url, HttpHeaders headers) throws IOException;
}
