package io.ristretto.decaptcha.solver.ui;

import android.support.annotation.Nullable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.jetbrains.annotations.Contract;

import io.ristretto.decaptcha.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class ReCaptchaSolverFragment extends CaptchaSolverFragment {

    private WebView mWebView;
    private boolean mIsWebViewAvailable;

    public ReCaptchaSolverFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_solver, container, false);
        mWebView = (WebView) view.findViewById(R.id.webview);
        mIsWebViewAvailable = true;
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        mWebView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        mWebView.onPause();
        super.onPause();
    }

    @Nullable
    @Contract(pure = true)
    private WebView getWebView() {
        if(mIsWebViewAvailable) {
            return mWebView;
        } else {
            return null;
        }
    }
}
