package com.androidandyuk.regularreminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by AndyCr15 on 24/08/2017.
 */

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG ,"onReceive");

        new MainActivity().setRecurring(context);

    }
}
