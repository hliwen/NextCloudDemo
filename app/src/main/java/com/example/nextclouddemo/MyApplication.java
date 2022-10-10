package com.example.nextclouddemo;

import android.app.Application;
import android.content.Context;
import android.content.Intent;


//import androidx.multidex.MultiDex;

public class MyApplication extends Application {

    private static Context context;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//        MultiDex.install(this);
        LogcatHelper.getInstance().start();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
