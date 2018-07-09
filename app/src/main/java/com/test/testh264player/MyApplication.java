package com.test.testh264player;

import android.app.Application;
import android.content.Context;

import com.yuri.xlog.Settings;
import com.yuri.xlog.XLog;

public class MyApplication extends Application {

    public static MyApplication mInstance;


    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        XLog.initialize(Settings.getInstance().setAppTag("Yuri"));

    }

    private Context getApp() {
        return mInstance.getApplicationContext();
    }
}
