package io.ristretto.decaptcha.solver.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.ristretto.decaptcha.data.Captcha;
import io.ristretto.decaptcha.net.Downloader;
import io.ristretto.decaptcha.net.NetCipherDownloader;


public abstract class CaptchaSolverFragment<I extends Captcha> extends Fragment{

    private static final String KEY_URL = "url";
    private static final String TAG = "CaptchaSolverFragment";
    private Uri uri;
    private OnFragmentInteractionListener mListener;
    private List<WeakReference<ProgressListener>> mLoadingProgressListeners = new LinkedList<>();

    private boolean isLoading = false;
    private Handler mHandler;
    private Handler mUIHandler;
    private HandlerThread mHeandlerThread;

    private boolean isReadyForCaptcha = false;
    private I pendingCaptcha = null;

    public interface OnFragmentInteractionListener {
        void onAuthentionHeadersResolved(Map<String, String> headers);
        void onFailure(@NonNull String message, @Nullable Throwable throwable);
    }

    protected static class CaptchaNotFoundException extends Exception {
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
        ProgressBar progressBar;
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
        return mHandler;
    }


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
        
        mHeandlerThread = new HandlerThread(TAG + "Thread");
        mHeandlerThread.start();
        mHandler = new Handler(mHeandlerThread.getLooper());
        
        if(uri != null) {
            // TODO
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    I captcha = null;
                    try {
                        captcha = receiverCaptcha(new NetCipherDownloader(), uri);
                        onCaptchaReceivedAsync(captcha);
                    } catch (IOException e) {
                        Log.e(TAG, "Error while receiving ", e);
                        notifyLoadingFailed(e);
                    }
                }
            });

        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isReadyForCaptcha = true;
        final I captcha = pendingCaptcha;
        if(captcha != null) {
            // Do it later. No hurry
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    onCaptchaReceived(captcha);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isReadyForCaptcha = false;
    }

    protected abstract I receiverCaptcha(@NonNull Downloader downloader, @NonNull Uri uri) throws IOException;

    private void onCaptchaReceivedAsync(final I captcha) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if(captcha == null) {
                    notifyLoadingIsAborted();
                    Log.e(TAG, "captcha not found");
                } else {
                    if(isReadyForCaptcha) {
                        onCaptchaReceived(captcha);
                    }
                }
            }
        });

    }

    @SuppressWarnings("unused")
    protected void onCaptchaReceived(@NonNull I captcha) {
        notifyLoadingIsDone();
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
        notifyLoadingIsAborted();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notifyLoadingIsAborted();
    }

    protected void notifyAuthenticationHeaders(Map<String, String> headers) {
        if(mListener != null) {
            mListener.onAuthentionHeadersResolved(headers);
        }
    }

    /**
     * Note: Calls to this method are executed with the default ui thread and should not
     * block.
     */
    public interface ProgressListener {
        /**
         * Called when its loading but don't know how long it will take
         * @param fragment
         */
        void onIsIndeterminate(@NonNull CaptchaSolverFragment fragment);

        /**
         * Called when the progress can be estimated
         * @param fragment
         * @param progress
         * @param max
         */
        void onProgress(@NonNull CaptchaSolverFragment fragment, int progress, int max);

        /**
         * Called when loading is done
         * @param fragment
         */
        void onDone(@NonNull CaptchaSolverFragment fragment);

        /**
         * Called when loading is aborted
         * @param fragment
         */
        void onAbort(@NonNull CaptchaSolverFragment fragment);
    }

    /**
     * Caller must keep a reference.
     */
    public void addLoadingProgressListener(ProgressListener listener) {
        WeakReference<ProgressListener> reference = new WeakReference<>(listener);
        mLoadingProgressListeners.add(reference);
    }


    protected void notifyLoadingFailed(Throwable exception) {
        notifyLoadingIsAborted();
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

    protected void notifyLoadingIsAborted() {
        mUIHandler.post(new Runnable() {
            public void run() {
                if (!isLoading) return;
                isLoading = false;
                Iterator<WeakReference<ProgressListener>> iterator;
                iterator = mLoadingProgressListeners.iterator();
                for (; iterator.hasNext(); ) {
                    WeakReference<ProgressListener> reference = iterator.next();
                    ProgressListener listener = reference.get();
                    if (null == listener) {
                        iterator.remove();
                    } else {
                        listener.onAbort(CaptchaSolverFragment.this);
                    }
                }
            }
        });
    }
}
