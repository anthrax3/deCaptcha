package io.ristretto.decaptcha;

import android.app.Application;

import info.guardianproject.netcipher.NetCipher;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NetCipher.useTor();
        //Thread.setDefaultUncaughtExceptionHandler(AppErrorActivity.getExceptionHandler(this));
    }
}
