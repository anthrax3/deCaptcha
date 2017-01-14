package io.ristretto.decaptcha.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by coffeemakr on 13.01.17.
 */

public interface Connector {
    HttpURLConnection connect(String method, URL url, HttpHeaders headers) throws IOException;
}
