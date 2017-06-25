package com.androidandyuk.regularreminders;

/**
 * Created by AndyCr15 on 25/06/2017.
 */

import android.app.Application;
import android.content.Context;

public class App extends Application {

    private static Application sApplication;

    public static Application getApplication() {
        return sApplication;
    }

    public static Context getContext() {
        return getApplication().getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }
}