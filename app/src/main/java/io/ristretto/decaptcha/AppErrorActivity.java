package io.ristretto.decaptcha;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PersistableBundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class AppErrorActivity extends AppCompatActivity {

    private static final String EXTRA_PREFIX = "io.ristretto.decaptcha.AppErrorActivity.";
    private static final String EXTRA_STACKTRACE = EXTRA_PREFIX + "EXTRA_STACKTRACE";
    private static final String EXTRA_THREAD_NAME = EXTRA_PREFIX + "EXTRA_THREAD_NAME";
    private static final String TAG = "AppErrorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "OnCreate");
        setContentView(R.layout.activity_app_error);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Intent intent = getIntent();
        Throwable throwable = (Throwable) intent.getSerializableExtra(EXTRA_STACKTRACE);
        if(throwable != null) {
            updateException(throwable);
        }
    }

    private void updateException(Throwable throwable) {
        String stacktrace = Log.getStackTraceString(throwable);
        TextView textView = (TextView) findViewById(R.id.stacktrace);
        textView.setText(stacktrace);
    }

    public static Thread.UncaughtExceptionHandler getExceptionHandler(Context context) {
        return new ExceptionHandler(context.getApplicationContext());
    }

    public static void startWithError(Context context, Thread thread, Throwable exception) {
        Log.e(TAG, "An error occurred", exception);
        Intent intent = new Intent(context, AppErrorActivity.class);
        intent.setAction(Intent.ACTION_APP_ERROR);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_STACKTRACE, exception);
        intent.putExtra(EXTRA_THREAD_NAME, thread.getName());
        context.startActivity(intent);
    }

    private static class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        private final Context mContext;

        public ExceptionHandler(Context context) {
            mContext = context;
        }

        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
            Log.e(TAG, "Uncaught exception", e);
        }
    }
}
