package io.ristretto.decaptcha.captcha;


import java.util.Observer;

public interface Captcha<T extends CaptchaChallenge>{

    T getChallenge();

    void setChallenge(T task);

    boolean hasResult();

    CaptchaResult getResult();

    void setResult(CaptchaResult captchaResult);

    void setSolvedListener(OnSolvedListener<CaptchaResult> solvedListener);

    OnSolvedListener<CaptchaResult> getOnSolvedListener();

    interface OnSolvedListener<R> {
        void onCaptchaSolved(Captcha captcha, R result);
    }

    void addTaskObserver(Observer observer);
    void deleteTaskObserver(Observer observer);

    interface TaskErrorCallback {
        void onFailure(Exception exception);
    }
}
