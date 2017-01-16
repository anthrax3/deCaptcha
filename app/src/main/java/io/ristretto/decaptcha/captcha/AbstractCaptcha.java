package io.ristretto.decaptcha.captcha;

import java.util.Observable;
import java.util.Observer;

public abstract class AbstractCaptcha<T extends CaptchaChallenge> implements Captcha<T> {

    private TaskObservable<T> mTaskObservable = new TaskObservable<T>();
    private OnSolvedListener<CaptchaResult> mSolvedListener = null;
    private CaptchaResult mCaptchaResult;


    @Override
    public T getChallenge() {
        synchronized (this) {
            if(mTaskObservable != null) {
                return mTaskObservable.mTask;
            }
        }
        return null;
    }

    @Override
    public boolean hasResult() {
        synchronized (this) {
            return mCaptchaResult != null;
        }
    }

    @Override
    public CaptchaResult getResult() {
        return mCaptchaResult;
    }

    @Override
    public void setResult(CaptchaResult captchaResult) {
        synchronized (this) {
            if (mCaptchaResult != captchaResult) {
                mCaptchaResult = captchaResult;
                if (mSolvedListener != null) {
                    mSolvedListener.onCaptchaSolved(this, captchaResult);
                }
            }
        }
    }

    @Override
    public void setSolvedListener(OnSolvedListener<CaptchaResult> solvedListener) {
        synchronized(this) {
            mSolvedListener = solvedListener;
        }
    }

    @Override
    public OnSolvedListener<CaptchaResult> getOnSolvedListener() {
        return mSolvedListener;
    }

    @Override
    public void addTaskObserver(Observer observer) {
        mTaskObservable.addObserver(observer);
    }

    @Override
    public void deleteTaskObserver(Observer observer) {
        mTaskObservable.deleteObserver(observer);
    }

    @Override
    public void setChallenge(T task) {
        mTaskObservable.setTask(task);
        mTaskObservable.notifyObservers(task);
    }


    private static class TaskObservable<T> extends Observable {
        private T mTask = null;

        @Override
        public void notifyObservers() {
            super.notifyObservers(mTask);
        }

        void setTask(T task) {
            if(mTask != task) {
                mTask = task;
                setChanged();
            }
        }
    }
}
