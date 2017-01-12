package io.ristretto.decaptcha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jetbrains.annotations.Contract;

import java.util.Map;

import io.ristretto.decaptcha.solver.ui.CaptchaSolverFragment;
import io.ristretto.decaptcha.solver.ui.ReCaptchaSolverFragment;

public class SolverActivity extends AppCompatActivity
        implements StartFragment.OnFragmentInteractionListener,
                    CaptchaSolverFragment.OnFragmentInteractionListener {

    private static final String TAG = "SolverActivity";
    private Uri startingPoint;
    private CaptchaLoadingProgressListener mCaptchaLoadingProgressListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solver);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mCaptchaLoadingProgressListener = new CaptchaLoadingProgressListener();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(startingPoint != null) {
                    solveUrl(startingPoint.toString());
                } else {
                    Toast.makeText(SolverActivity.this, "Nothing set",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        showStartFragment("https://4chan.org");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        switch (action) {
            case Intent.ACTION_VIEW:
                solveUrl(intent.getDataString());
                break;
            default:
                Log.e(TAG, "Got invalid intent: action=" + action);
                break;
        }
    }

    private void setFloatingButtonIsVisible(boolean visible) {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab == null) {
            Log.w(TAG, "Fab not found");
            return;
        }
        if(visible) {
            fab.show();
        } else {
            fab.hide();
        }
    }

    private void showFAB() {
        setFloatingButtonIsVisible(true);
    }

    private void hideFAB() {
        setFloatingButtonIsVisible(false);
    }


    /**
     * Get the active fragment if it is of the expected class.
     * @param expectedClass the expected class.
     * @param <T> The fragment type
     * @return the casted fragment
     */
    @Nullable
    @Contract("null -> fail")
    private <T extends Fragment> T getActiveFragment(Class<T> expectedClass) {
        if(expectedClass == null) throw new NullPointerException("expectedClass is null");
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment);
        if(fragment == null || ! expectedClass.isAssignableFrom(fragment.getClass())) {
            return null;
        } else {
            return expectedClass.cast(fragment);
        }
    }

    private void showStartFragment(String defaultUrl) {
        showStartFragment(defaultUrl, null);
    }

    private void showStartFragment(String defaultUrl, @Nullable String errorMessage) {
        Fragment fragment = getActiveFragment(StartFragment.class);
        if(fragment == null) {
            Log.d(TAG, "Creating start fragment with default url: " + defaultUrl);
            fragment = StartFragment.newInstance(defaultUrl, errorMessage);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment, fragment)
                    .commit();
        } else {
            Log.d(TAG, "Updating default url: " + defaultUrl);
            ((StartFragment) fragment).updateDefaultUrl(defaultUrl);
        }
    }

    @Contract("null -> fail")
    private void solveUrl(final String url) {
        if(url == null) throw new NullPointerException("url is null");
        if(url.isEmpty()) throw new IllegalArgumentException("url is empty");
        Fragment fragment = getActiveFragment(CaptchaSolverFragment.class);
        if(fragment == null) {
            CaptchaSolverFragment captchaSolverFragment;
            captchaSolverFragment = CaptchaSolverFragment.newInstance(ReCaptchaSolverFragment.class, url);
            captchaSolverFragment.addLoadingProgressListener(mCaptchaLoadingProgressListener);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment, captchaSolverFragment)
                    .addToBackStack(null)
                    .commit();
        }
        hideFAB();
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

    @Override
    public void onFragmentInteraction(Uri uri) {
        this.startingPoint = uri;
        showFAB();
    }

    @Override
    public void onAuthentionHeadersResolved(Map<String, String> headers) {

    }

    @Override
    public void onFailure(@NonNull String message, @Nullable Throwable throwable) {
        hideProgressBar();
        showStartFragment(startingPoint.toString(), message);
    }


    private @Nullable ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.progressBar);
    }

    private void hideProgressBar() {
        ProgressBar progressBar = getProgressBar();
        if(progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private class CaptchaLoadingProgressListener implements CaptchaSolverFragment.ProgressListener {
        @Override
        public void onIsIndeterminate(@NonNull CaptchaSolverFragment fragment) {
            ProgressBar progressBar = getProgressBar();
            if(progressBar == null) return;
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onProgress(@NonNull CaptchaSolverFragment fragment, int progress, int max) {
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
        public void onAbort(@NonNull CaptchaSolverFragment fragment) {
            hideProgressBar();
        }
    }
}
