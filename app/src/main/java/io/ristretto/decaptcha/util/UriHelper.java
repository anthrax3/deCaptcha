package io.ristretto.decaptcha.util;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.net.MalformedURLException;
import java.net.URL;

public class UriHelper {
    public static final String[] PROTOCOLS_HTTP_AND_HTTPS = new String[]{"http", "https"};

    private static boolean isAllowedProtocol(String protocol, String[] protocols) {
        for(String allowed: protocols) {
            if(allowed.equals(protocol)) return true;
        }
        return false;
    }

    private static void checkAllowedProtocol(String protocol, String[] protocols) {
        if(!isAllowedProtocol(protocol, protocols)) {
            throw new IllegalArgumentException("Protocol '" +protocol+ "' not allowed");
        }
    }


    @NonNull
    public static URL uriToURL(Uri uri, String[] allowedProtocols) throws MalformedURLException {
        checkAllowedProtocol(uri.getScheme(), allowedProtocols);
        return new URL(uri.toString());
    }
}
