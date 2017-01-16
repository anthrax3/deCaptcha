package io.ristretto.decaptcha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import io.ristretto.decaptcha.captcha.CaptchaResult;

public class StartActivity extends AppCompatActivity
    implements StartFragment.OnFragmentInteractionListener{

    private static final String TAG = "StartActivity";
    private static final int REQUEST_CODE_SOLVE = 1;
    public static final String KEY_RESULT = "RESULT";
    private static final String KEY_URL = "URL";

    private Uri startingPoint;
    private CaptchaResult captchaResult;

    @Override
    public void onFragmentInteraction(Uri uri) {
        this.startingPoint = uri;
        showFAB();
    }


    private void showStartFragment(String defaultUrl) {
        showStartFragment(defaultUrl, null);
    }

    private void showStartFragment(String defaultUrl, @Nullable String errorMessage) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (fragment instanceof StartFragment) {
            Log.d(TAG, "Updating default url: " + defaultUrl);
            ((StartFragment) fragment).updateDefaultUrl(defaultUrl);
        } else {
            Log.d(TAG, "Creating start fragment with default url: " + defaultUrl);
            fragment = StartFragment.newInstance(defaultUrl, errorMessage);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment, fragment)
                    .commit();
        }
    }

    private void showResultFragment(CaptchaResult captchaResult) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if(fragment instanceof ResultFragment){
            ((ResultFragment) fragment).updateResult(captchaResult);
        } else {
            fragment = ResultFragment.newInstance(captchaResult);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment, fragment)
                    .addToBackStack("result")
                    .commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            this.captchaResult = savedInstanceState.getParcelable(KEY_RESULT);
            this.startingPoint = savedInstanceState.getParcelable(KEY_URL);
        }

        setContentView(R.layout.activity_start);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(startingPoint != null) {
                    hideFAB();
                    solveUrl(startingPoint);
                } else {
                    Toast.makeText(StartActivity.this, "Nothing set",
                            Toast.LENGTH_SHORT).show();
                }
            }


        });
        showStartFragment("https://www.cloudflare.com/ddos/");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_RESULT, captchaResult);
        outState.putParcelable(KEY_URL, startingPoint);
    }

    private void solveUrl(Uri startingPoint) {
        Intent intent = new Intent(this, SolverActivity.class);
        intent.setAction(Intent.ACTION_RUN);
        intent.setData(startingPoint);
        startActivityForResult(intent, REQUEST_CODE_SOLVE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Got activity result: " + requestCode);
        switch (requestCode) {
            case REQUEST_CODE_SOLVE:
                handleSolveResult(resultCode, data);
                break;
        }
    }

    private void handleSolveResult(int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            CaptchaResult result = data.getParcelableExtra(SolverActivity.EXTRA_RESULT);
            Log.d(TAG, "Got result! :) " + result);
            showResultFragment(result);
        } else if (resultCode == RESULT_CANCELED) {
            Log.d(TAG, "Result canceled");
        } else {
            Log.wtf(TAG, "Got invalid result code " + resultCode);
        }
    }

    private void showFAB() {
        setFloatingButtonIsVisible(true);
    }

    private void hideFAB() {
        setFloatingButtonIsVisible(false);
    }


}
