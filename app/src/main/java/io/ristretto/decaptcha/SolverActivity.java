package io.ristretto.decaptcha;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import org.jetbrains.annotations.Contract;

import io.ristretto.decaptcha.captcha.CaptchaResult;
import io.ristretto.decaptcha.ui.CaptchaSolverFragment;
import io.ristretto.decaptcha.ui.CloudFlareSolverFragment;

public class SolverActivity extends AppCompatActivity
        implements CaptchaSolverFragment.OnFragmentInteractionListener {

    private static final String TAG = "SolverActivity";
    public static final String EXTRA_RESULT = "io.ristretto.decaptcha.SolverActivity.extra.EXTRA_RESULT";
    public static final String EXTRA_ERROR_MESSAGE = "io.ristretto.decaptcha.SolverActivity.extra.EXTRA_ERROR_MESSAGE";
    public static final String EXTRA_USER_CANCELED = "io.ristretto.decaptcha.SolverActivity.extra.EXTRA_USER_CANCELED";
    private static final String EXTRA_CAUSE = "io.ristretto.decaptcha.SolverActivity.extra.EXTRA_ERROR_CAUSE";
    private CaptchaLoadingProgressListener mCaptchaLoadingProgressListener;
    private int mShortAnimTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solver);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mCaptchaLoadingProgressListener = new CaptchaLoadingProgressListener();
        mShortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        String action = intent.getAction();
        switch (action) {
            case Intent.ACTION_RUN:
                solveUrl(intent.getDataString());
                break;
            default:
                Log.e(TAG, "Got invalid intent: action=" + action);
                break;
        }
    }

    @Contract("null -> fail")
    private void solveUrl(final String url) {
        if(url == null) throw new NullPointerException("url is null");
        if(url.isEmpty()) throw new IllegalArgumentException("url is empty");
        CaptchaSolverFragment captchaSolverFragment = CaptchaSolverFragment.newInstance(CloudFlareSolverFragment.class, url);
        captchaSolverFragment.addLoadingProgressListener(mCaptchaLoadingProgressListener);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment, captchaSolverFragment)
                .commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_solver, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private Intent getResultIntent(CaptchaResult result) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT, result);
        return intent;
    }

    private Intent getErrorIntent(String message, Throwable throwable) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_USER_CANCELED, false);
        intent.putExtra(EXTRA_ERROR_MESSAGE, message);
        intent.putExtra(EXTRA_CAUSE, throwable);
        return intent;
    }

    @Override
    public void onResult(@NonNull CaptchaResult captchaResult) {
        hideProgressBar();
        Log.d(TAG, "Got result: " + captchaResult);
        setResult(RESULT_OK, getResultIntent(captchaResult));
        finish();
    }

    @Override
    public void onFailure(@NonNull String message, @Nullable Throwable throwable) {
        hideProgressBar();
        Log.e(TAG, "Got failure: " + message, throwable);
        setResult(RESULT_CANCELED, getErrorIntent(message, throwable));
        finish();
    }

    private @Nullable ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.progressBar);
    }

    private void hideProgressBar() {
        final ProgressBar progressBar = getProgressBar();
        if(progressBar == null) return;
        if(progressBar.getVisibility() == View.GONE) return;
        progressBar.animate()
                .setDuration(mShortAnimTime)
                .alpha(0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void showProgressBar() {
        ProgressBar progressBar = getProgressBar();
        if(progressBar == null || progressBar.getVisibility() == View.VISIBLE) return;
        progressBar.setAlpha(0f);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.animate()
                .setDuration(mShortAnimTime)
                .alpha(1f)
                .start();
    }


    private class CaptchaLoadingProgressListener implements CaptchaSolverFragment.ProgressListener {
        @Override
        public void onIsIndeterminate(@NonNull CaptchaSolverFragment fragment) {
            showProgressBar();
            ProgressBar progressBar = getProgressBar();
            if(progressBar == null) return;
            progressBar.setIndeterminate(true);

        }

        @Override
        public void onProgress(@NonNull CaptchaSolverFragment fragment, int progress, int max) {
            showProgressBar();
            ProgressBar progressBar = getProgressBar();
            if(progressBar == null) return;
            progressBar.setMax(max);
            progressBar.setIndeterminate(false);
            progressBar.setProgress(progress);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onDone(@NonNull CaptchaSolverFragment fragment) {
            hideProgressBar();
        }

        @Override
        public void onAbort(@NonNull CaptchaSolverFragment fragment, Throwable reason) {
            hideProgressBar();
        }
    }
}
