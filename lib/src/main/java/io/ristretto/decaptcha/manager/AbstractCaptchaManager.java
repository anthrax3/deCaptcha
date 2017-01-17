package io.ristretto.decaptcha.manager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

import io.ristretto.decaptcha.captcha.Captcha;
import io.ristretto.decaptcha.captcha.CaptchaChallenge;
import io.ristretto.decaptcha.captcha.CaptchaResult;
import io.ristretto.decaptcha.net.Downloader;


public abstract class AbstractCaptchaManager<T extends CaptchaChallenge, C extends Captcha<T>>
        implements CaptchaManager<T, C> {

    private final Downloader downloader;
    private final File cacheDir;

    public AbstractCaptchaManager(Downloader downloader, File cacheDir) {
        this.downloader = downloader;
        this.cacheDir = cacheDir;
    }


    public File getCacheDir() {
        return cacheDir;
    }

    public Downloader getDownloader() {
        return downloader;
    }


    public void submitTask(C captcha, T task, Object... answers)
            throws IOException, LoaderException
    {
        T newChallenge = submitAndGetNewTask(captcha, task, answers);
        captcha.setChallenge(newChallenge);
    }

    protected void foundResult(Captcha<T> captcha, CaptchaResult result) {
        captcha.setResult(result);
    }

    @Nullable
    protected abstract T submitAndGetNewTask(C captcha, T task, Object... answers) throws IOException, LoaderException;
}
