package io.ristretto.decaptcha.loader;

import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;

import io.ristretto.decaptcha.captcha.Captcha;
import io.ristretto.decaptcha.captcha.CaptchaChallenge;
import io.ristretto.decaptcha.net.Downloader;


public abstract class AbstractCaptchaManager<T extends CaptchaChallenge, C extends Captcha<T>> implements CaptchaManager<T, C> {

    private final Downloader downloader;
    private final File chacheDir;

    public AbstractCaptchaManager(Downloader downloader, File cacheDir) {
        this.downloader = downloader;
        this.chacheDir = cacheDir;
    }


    public File getCacheDir() {
        return chacheDir;
    }

    public Downloader getDownloader() {
        return downloader;
    }

    @Nullable
    public abstract T submitTask(C captcha, T task, Object... answers) throws IOException, LoaderException;
}
