package com.androidandyuk.regularreminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by AndyCr15 on 06/07/2017.
 */

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG,"onReceive");

        new MainActivity().checkNotifications(context);



    }
}