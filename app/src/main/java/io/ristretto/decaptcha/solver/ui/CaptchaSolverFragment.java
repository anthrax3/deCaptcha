package io.ristretto.decaptcha.solver.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.RecoverySystem;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public abstract class CaptchaSolverFragment extends Fragment{

    private static final String KEY_URL = "url";
    private Uri uri;
    private OnFragmentInteractionListener mListener;
    private List<WeakReference<ProgressListener>> mLoadingProgressListeners = new LinkedList<>();

    private boolean isLoading = false;

    public interface OnFragmentInteractionListener {
        void onAuthentionHeadersResolved(Map<String, String> headers);
        void onException(Throwable throwable);
        void onFailure();
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

    @Override
    @Deprecated
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
        notifyLoadingIsIndeterminate();
        onCreate(uri);
    }

    public void onCreate(@NonNull Uri uri) {

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


    public interface ProgressListener {
        void onIsIndeterminate(CaptchaSolverFragment fragment);
        void onProgress(CaptchaSolverFragment fragment, int progress, int max);
        void onDone(CaptchaSolverFragment fragment);
        void onAbort(CaptchaSolverFragment fragment);
    }

    /**
     * Caller must keep a reference.
     */
    public void addLoadingProgressListener(ProgressListener listener) {
        WeakReference<ProgressListener> reference = new WeakReference<>(listener);
        mLoadingProgressListeners.add(reference);
    }

    protected void notifyLoadingProgress(int progress, int max) {
        isLoading = true;
        for (Iterator<WeakReference<ProgressListener>> iterator = mLoadingProgressListeners.iterator(); iterator.hasNext(); ) {
            WeakReference<ProgressListener> reference = iterator.next();
            ProgressListener listener = reference.get();
            if(null == listener) {
                iterator.remove();
            } else {
                listener.onProgress(this, progress, max);
            }
        }
    }

    protected void notifyLoadingIsIndeterminate() {
        isLoading = true;
        for (Iterator<WeakReference<ProgressListener>> iterator = mLoadingProgressListeners.iterator(); iterator.hasNext(); ) {
            WeakReference<ProgressListener> reference = iterator.next();
            ProgressListener listener = reference.get();
            if(null == listener) {
                iterator.remove();
            } else {
                listener.onIsIndeterminate(this);
            }
        }
    }

    protected void notifyLoadingIsDone() {
        if(!isLoading) return;
        isLoading = false;
        for (Iterator<WeakReference<ProgressListener>> iterator = mLoadingProgressListeners.iterator(); iterator.hasNext(); ) {
            WeakReference<ProgressListener> reference = iterator.next();
            ProgressListener listener = reference.get();
            if(null == listener) {
                iterator.remove();
            } else {
                listener.onDone(this);
            }
        }
    }

    protected void notifyLoadingIsAborted() {
        if(!isLoading) return;
        isLoading = false;
        for (Iterator<WeakReference<ProgressListener>> iterator = mLoadingProgressListeners.iterator(); iterator.hasNext(); ) {
            WeakReference<ProgressListener> reference = iterator.next();
            ProgressListener listener = reference.get();
            if(null == listener) {
                iterator.remove();
            } else {
                listener.onAbort(this);
            }
        }
    }
}
