package com.androidandyuk.regularreminders;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

/**
 * Created by AndyCr15 on 06/07/2017.
 */

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        String notificationText = intent.getStringExtra("Message");
        //if we want ring on notification then uncomment below line
//        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 100, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);


//        Intent snoozeIntent = new Intent();
//        snoozeIntent.setAction("Snooze");
//        PendingIntent pendingSnoozeIntent = PendingIntent.getActivity(context, 100, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

//        Calendar cal = Calendar.getInstance();
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
//        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + 3600000, pendingIntent);





        NotificationCompat.Builder builder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.rr)
                .setContentTitle("Check your reminders!")
                .setContentText(notificationText)
                .setAutoCancel(true);
//                .addAction(R.drawable.icon, "Snooze", pendingSnoozeIntent);

        notificationManager.notify(100, builder.build());

    }
}