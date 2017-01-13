package io.ristretto.decaptcha.data;


public class CaptchaImpl implements Captcha {

    private final String baseURL;

    public CaptchaImpl(String baseURL) {
        this.baseURL = baseURL;
    }

    @Override
    public String getBaseURL() {
        return baseURL;
    }
}
