package com.androidandyuk.regularreminders;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.Date;

import static com.androidandyuk.regularreminders.MainActivity.nextNotification;
import static com.androidandyuk.regularreminders.MainActivity.reminders;
import static com.androidandyuk.regularreminders.MainActivity.remindersOverdue;
import static com.androidandyuk.regularreminders.reminderItem.daysDifference;

/**
 * Created by AndyCr15 on 06/07/2017.
 */

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent thisIntent = new Intent(context, MainActivity.class);
        thisIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //if we want ring on notifcation then uncomment below line//
//        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 100, thisIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // check how many reminders are overdue
        remindersOverdue = 0;
        for (reminderItem thisReminder : reminders) {
            Date nextDue = reminderItem.nextDue(thisReminder);
            int dif = daysDifference(new Date(), nextDue);
            if (dif < 1) {
                remindersOverdue++;
                Log.i("remindersOverdue", thisReminder.name + " is due");
            }
        }

        String notificationText = "Don't forget to " + nextNotification;
        if (remindersOverdue == 2) {
            notificationText += " and one other reminder are due";
        }
        if (remindersOverdue > 2) {
            notificationText += " and " + (remindersOverdue - 1) + " other reminders are due";
        }

        NotificationCompat.Builder builder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.rr)
                .setContentTitle("Check your reminders!")
                .setContentText(notificationText)
                .setAutoCancel(true);

        notificationManager.notify(100, builder.build());

    }
}