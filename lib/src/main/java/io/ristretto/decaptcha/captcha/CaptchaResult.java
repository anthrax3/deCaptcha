package io.ristretto.decaptcha.captcha;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CaptchaResult {
    private final ArrayList<String> cookies;

    public CaptchaResult() {
        cookies = new ArrayList<>();
    }

    public CaptchaResult(List<String> cookies) {
        this.cookies = new ArrayList<>(cookies);
    }

    public void setCookiesFromHttpCookies(Collection<HttpCookie> cookies) {
        this.cookies.clear();
        this.cookies.ensureCapacity(cookies.size());
        for(HttpCookie cookie: cookies) {
            this.cookies.add(cookie.toString());
        }
    }

    @Override
    public String toString() {
        return "CaptchaResult{" +
                "cookies=" + cookies +
                '}';
    }

    public ArrayList<String> getCookies() {
        return new ArrayList<>(cookies);
    }
}
