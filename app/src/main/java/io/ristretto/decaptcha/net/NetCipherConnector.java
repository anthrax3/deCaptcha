package io.ristretto.decaptcha.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import info.guardianproject.netcipher.NetCipher;


public class NetCipherConnector implements Connector {

    private static NetCipherConnector instance = null;

    public static NetCipherConnector getInstance() {
        if(instance == null) {
            synchronized (NetCipherConnector.class) {
                if(instance == null) {
                    instance = new NetCipherConnector();
                }
            }
        }
        return instance;
    }

    @Override
    public HttpURLConnection connect(String method, URL url, HttpHeaders headers) throws IOException {
        HttpURLConnection httpURLConnection = NetCipher.getHttpURLConnection(url);
        httpURLConnection.setRequestMethod(method);
        httpURLConnection.setInstanceFollowRedirects(false);
        for(Map.Entry<String, String> entry: headers.entrySet()) {
            httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        httpURLConnection.connect();
        return httpURLConnection;
    }
}
