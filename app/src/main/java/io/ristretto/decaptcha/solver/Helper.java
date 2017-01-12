package io.ristretto.decaptcha.solver;

import android.net.Uri;

import java.net.MalformedURLException;
import java.net.URL;

public class Helper {
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


    public static URL uriToURL(Uri uri, String[] allowedProtocols) {
        checkAllowedProtocol(uri.getScheme(), allowedProtocols);
        try {
            return new URL(uri.toString());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
