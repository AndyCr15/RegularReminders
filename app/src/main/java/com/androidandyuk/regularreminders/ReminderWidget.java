package com.androidandyuk.regularreminders;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import static com.androidandyuk.regularreminders.MainActivity.reminders;

/**
 * Implementation of App Widget functionality.
 */
public class ReminderWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.reminder_widget);

            String text = reminders.get(0).name;
            views.setTextViewText(R.id.textView4, text);

            Intent openApp = new Intent(context, MainActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(context, 0 , openApp, 0);

            views.setOnClickPendingIntent(R.id.openApp, pIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);

        }
    }

//    @Override
//    public void onEnabled(Context context) {
//        // Enter relevant functionality for when the first widget is created
//    }
//
//    @Override
//    public void onDisabled(Context context) {
//        // Enter relevant functionality for when the last widget is disabled
//    }
}

