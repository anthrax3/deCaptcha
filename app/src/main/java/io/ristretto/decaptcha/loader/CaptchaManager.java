package io.ristretto.decaptcha.loader;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.URL;

import io.ristretto.decaptcha.captcha.Captcha;
import io.ristretto.decaptcha.captcha.CaptchaChallenge;


public interface CaptchaManager<T extends CaptchaChallenge, C extends Captcha<T>> {

    class LoaderException extends Exception {
        private final String debugContext;

        public LoaderException(String message, Throwable cause) {
            super(message, cause);
            debugContext = null;
        }

        public LoaderException(String message, String debugContext) {
            super(message + " -- " + debugContext);
            this.debugContext = debugContext;
        }

        public String getDebugContext() {
            return debugContext;
        }
    }

    @NonNull
    C loadCaptcha(URL url) throws IOException, LoaderException;

    @NonNull
    T loadTask(C captcha) throws IOException, LoaderException;

}
