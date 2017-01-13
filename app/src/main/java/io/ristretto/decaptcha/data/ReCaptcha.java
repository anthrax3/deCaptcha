package io.ristretto.decaptcha.data;

import android.support.annotation.Nullable;

public class ReCaptcha extends CaptchaImpl {
    private final String siteKey;
    private final String url;
    private final String stoken;

    public ReCaptcha(String siteKey, String url) {
        super(url);
        this.siteKey = siteKey;
        this.url = url;
        this.stoken = null;
    }

    public ReCaptcha(String siteKey, String url, String sToken) {
        super(url);
        this.siteKey = siteKey;
        this.url = url;
        this.stoken = sToken;
    }

    public boolean hasSToken() {
        return stoken != null;
    }

    @Nullable
    public String getSToken() {
        return stoken;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public String getUrl() {
        return url;
    }
}
