package io.ristretto.decaptcha;

import android.app.Application;

import java.util.logging.Level;

import info.guardianproject.netcipher.NetCipher;
import io.ristretto.decaptcha.util.AndroidLoggingHandler;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NetCipher.useTor();
        //Thread.setDefaultUncaughtExceptionHandler(AppErrorActivity.getExceptionHandler(this));
        AndroidLoggingHandler handler = new AndroidLoggingHandler();
        handler.setLevel(Level.FINEST);
        AndroidLoggingHandler.reset(handler);
    }
}
