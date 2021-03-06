package io.ristretto.decaptcha.ui;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import io.ristretto.decaptcha.captcha.Captcha;
import io.ristretto.decaptcha.captcha.CaptchaChallenge;
import io.ristretto.decaptcha.captcha.CaptchaResult;
import io.ristretto.decaptcha.captcha.ParcableCaptchaResult;
import io.ristretto.decaptcha.manager.CaptchaManager;
import io.ristretto.decaptcha.net.Connector;
import io.ristretto.decaptcha.net.Downloader;
import io.ristretto.decaptcha.net.GracefulDownloader;
import io.ristretto.decaptcha.net.NetCipherConnector;
import io.ristretto.decaptcha.util.UriHelper;


public abstract class CaptchaSolverFragment<T extends CaptchaChallenge, C extends Captcha<T>> extends Fragment{

    private static final String KEY_URL = "url";
    private static final String TAG = "CaptchaSolverFragment";
    private Uri uri;
    private OnFragmentInteractionListener mListener;
    private List<WeakReference<ProgressListener>> mLoadingProgressListeners = new LinkedList<>();

    private boolean isLoading = false;
    private Handler mWorkHandler;
    private Handler mUIHandler;
    private HandlerThread mWorkHandlerThread;

    private C pendingCaptcha = null;
    private C mCaptcha;
    private Downloader mDownloader;
    private Captcha.OnSolvedListener<CaptchaResult> mCaptchaSolvedListener;
    private Observer mTaskObserver;

    public interface OnFragmentInteractionListener {
        void onResult(@NonNull CaptchaResult captchaResult);
        void onFailure(@NonNull String message, @Nullable Throwable throwable);
    }

    /**
     * Factory method to create a CaptchaSolverFragment
     * @param clazz the subclass of CaptchaSolverFragment to use
     * @param captchaUrl the url which contains the fragment
     * @param <T>
     * @return
     */
    public static <T extends CaptchaSolverFragment> T newInstance(Class<T> clazz, String captchaUrl) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_URL, captchaUrl);
        T fragment;
        try {
            fragment = clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Can't create fragment", e);
        }
        fragment.setArguments(bundle);
        return fragment;

    }

    protected Handler getHandler() {
        return mWorkHandler;
    }


    protected abstract CaptchaManager<T, C> getCaptchaManager();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            this.uri = Uri.parse(savedInstanceState.getString(KEY_URL));
        } else {
            Bundle args = getArguments();
            if(args != null) {
                this.uri = Uri.parse(getArguments().getString(KEY_URL));
            }
        }
        mUIHandler = new Handler();
        notifyLoadingIsIndeterminate();

        mWorkHandlerThread = new HandlerThread(TAG + "Thread");
        mWorkHandlerThread.start();
        mWorkHandler = new Handler(mWorkHandlerThread.getLooper());
        Connector mConnector = NetCipherConnector.getInstance();
        mDownloader = new GracefulDownloader(mConnector);
        mTaskObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if(arg == null) return;
                Log.d(TAG, "New challenge: " + arg);
                final T challenge = (T) arg;
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        C captcha = getCaptcha();
                        if(captcha == null) return;
                        onCaptchaChallengeLoaded(captcha, challenge);
                    }
                });
            }
        };
        mCaptchaSolvedListener = new Captcha.OnSolvedListener<CaptchaResult>() {
            @Override
            public void onCaptchaSolved(final Captcha captcha, final CaptchaResult result) {
                if(result == null) throw new NullPointerException("result is null");
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onResult(result);
                    }
                });
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        if(uri != null) {
            // TODO
            final URL url = UriHelper.uriToURL(uri, UriHelper.PROTOCOLS_HTTP_AND_HTTPS);
            mWorkHandler.post(new Runnable() {
                @Override
                public void run() {
                loadCaptcha(url);
                }
            });
        }
    }


    private void loadCaptcha(URL url) {
        final C captcha;
        CaptchaManager<T, C> captchaManager = getCaptchaManager();
        if(captchaManager == null) {
            throw new IllegalStateException("captchaManager is null");
        }
        try {
            captcha = captchaManager.loadCaptcha(url);
        } catch (IOException | CaptchaManager.LoaderException e) {
            notifyLoadingFailed(e);
            return;
        }
        Activity activity = getActivity();
        if(activity == null) {
            // Fragment detached
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateCaptcha(captcha);
            }
        });
        mWorkHandler.post(new Runnable() {
            @Override
            public void run() {
                loadCaptchaChallenge(captcha);
            }
        });
    }

    private void loadCaptchaChallenge(final C captcha) {
        CaptchaManager<T, C> captchaManager = getCaptchaManager();
        if(captchaManager == null) {
            throw new IllegalStateException("captchaManager is null");
        }
        final T challenge;
        try {
            challenge = captchaManager.loadTask(captcha);
        } catch (CaptchaManager.LoaderException | IOException e) {
            notifyLoadingFailed(e);
            return;
        }
        captcha.setSolvedListener(mCaptchaSolvedListener);
        captcha.addTaskObserver(mTaskObserver);
        captcha.setChallenge(challenge);
        Activity activity = getActivity();
        if(activity == null) {
            // Fragment has been detached.
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onCaptchaChallengeLoaded(captcha, challenge);
                notifyLoadingIsDone();
            }
        });
    }



    protected void onCaptchaChallengeLoaded(@NonNull C captcha, @NonNull T challenge){};

    protected void onCaptchaLoaded(@NonNull C captcha){};


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final C captcha = pendingCaptcha;
        pendingCaptcha = null;
        if(captcha != null) {
            // Do it later. No hurry
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    onCaptchaLoaded(captcha);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mWorkHandlerThread.quitSafely();
        } else {
            mWorkHandlerThread.quit();
        }
    }


    private void updateCaptcha(final C captcha) {
        mCaptcha = captcha;
        final Activity activity = getActivity();
        if(activity == null) {
            return;
        }
        if (getView() != null) {
            onCaptchaLoaded(captcha);
        } else {
            pendingCaptcha = captcha;
        }
    }

    @Nullable
    public C getCaptcha() {
        return mCaptcha;
    }

    @NonNull
    public Downloader getDownloader() {
        if(mDownloader == null) throw new IllegalStateException("downloader not ready");
        return mDownloader;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_URL, uri.toString());
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException("Activity " + context +
                    " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        notifyLoadingIsAborted(null);
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDownloader = null;
        notifyLoadingIsAborted(null);
    }


    protected void notifyFailed(@StringRes int messageId, Throwable exception) {
        String message = getString(messageId);
        Log.e(TAG, message, exception);
    }

    /**
     * Note: Calls to this method are executed with the default ui thread and should not
     * block.
     */
    public interface ProgressListener {
        /**
         * Called when its loading but don't know how long it will take
         * @param fragment the fragment
         */
        void onIsIndeterminate(@NonNull CaptchaSolverFragment fragment);

        /**
         * Called when the progress can be estimated
         * @param fragment the fragment
         * @param progress the current progress
         * @param max the maximal estimated progress
         */
        void onProgress(@NonNull CaptchaSolverFragment fragment, int progress, int max);

        /**
         * Called when loading is done
         * @param fragment the fragment
         */
        void onDone(@NonNull CaptchaSolverFragment fragment);

        /**
         * Called when loading is aborted
         * @param fragment the fragment
         * @param reason the reason for the abort
         */
        void onAbort(@NonNull CaptchaSolverFragment fragment, Throwable reason);
    }

    /**
     * Caller must keep a reference.
     */
    public void addLoadingProgressListener(ProgressListener listener) {
        WeakReference<ProgressListener> reference = new WeakReference<>(listener);
        mLoadingProgressListeners.add(reference);
    }


    protected void notifyLoadingFailed(Throwable exception) {
        Log.e(TAG, "Failed to load captcha", exception);
        notifyLoadingIsAborted(exception);
    }

    protected void notifyLoadingProgress(final int progress, final int max) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                isLoading = true;
                for (Iterator<WeakReference<ProgressListener>> iterator = mLoadingProgressListeners.iterator(); iterator.hasNext(); ) {
                    WeakReference<ProgressListener> reference = iterator.next();
                    ProgressListener listener = reference.get();
                    if(null == listener) {
                        iterator.remove();
                    } else {
                        listener.onProgress(CaptchaSolverFragment.this, progress, max);
                    }
                }
            }
        });
    }

    protected void notifyLoadingIsIndeterminate() {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                isLoading = true;
                for (Iterator<WeakReference<ProgressListener>> iterator = mLoadingProgressListeners.iterator(); iterator.hasNext(); ) {
                    WeakReference<ProgressListener> reference = iterator.next();
                    ProgressListener listener = reference.get();
                    if(null == listener) {
                        iterator.remove();
                    } else {
                        listener.onIsIndeterminate(CaptchaSolverFragment.this);
                    }
                }
            }
        });
    }

    protected void notifyLoadingIsDone() {
        if (!isLoading) return;
        isLoading = false;
        mUIHandler.post(new Runnable() {
            public void run() {
                for (Iterator<WeakReference<ProgressListener>> iterator = mLoadingProgressListeners.iterator(); iterator.hasNext(); ) {
                    WeakReference<ProgressListener> reference = iterator.next();
                    ProgressListener listener = reference.get();
                    if (null == listener) {
                        iterator.remove();
                    } else {
                        listener.onDone(CaptchaSolverFragment.this);
                    }
                }
            }
        });
    }

    protected void notifyLoadingIsAborted(@Nullable final Throwable reason) {
        if (!isLoading) return;
        isLoading = false;
        mUIHandler.post(new Runnable() {
            public void run() {
                Iterator<WeakReference<ProgressListener>> iterator;
                iterator = mLoadingProgressListeners.iterator();
                for (; iterator.hasNext(); ) {
                    WeakReference<ProgressListener> reference = iterator.next();
                    ProgressListener listener = reference.get();
                    if (null == listener) {
                        iterator.remove();
                    } else {
                        listener.onAbort(CaptchaSolverFragment.this, reason);
                    }
                }
            }
        });
    }
}
