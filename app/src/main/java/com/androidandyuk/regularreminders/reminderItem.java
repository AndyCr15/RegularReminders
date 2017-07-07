package com.androidandyuk.regularreminders;

import android.support.annotation.NonNull;
import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.androidandyuk.regularreminders.MainActivity.sdf;

/**
 * Created by AndyCr15 on 24/06/2017.
 */

public class reminderItem implements Comparable<reminderItem> {
    String name;
    String tag;
    int frequency;
    Boolean notify;
    ArrayList<String> completed = new ArrayList<>();

    public reminderItem(String name, String tag, int frequency) {
        this.name = name;
        this.tag = tag;
        this.frequency = frequency;
        this.notify = true;
        String today = sdf.format(new Date());
        Log.i("reminderItem", "Creating new item " + name);
        this.completed.add(today);
    }

    public reminderItem(String name, String tag, int frequency, Boolean notify, String status) {
        this.name = name;
        this.tag = tag;
        this.frequency = frequency;
        this.notify = notify;
        this.completed = new ArrayList<>();
    }

//    public Long nextDue() {
//        Date now = new Date();
//        Date last = null;
//        try {
//            last = sdf.parse(this.completed.get(0));
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        return Math.abs(last.getTime()) + Math.abs(frequency) - Math.abs(now.getTime());
//    }

//    @Override
//    public String toString() {
//        return name + " last completed " + this.completed.get(0);
//    }

    public static int daysDifference(Date asDate1, Date asDate2) {
        int dayDifference = 0;
        try {
            //Comparing dates
            long difference = asDate2.getTime() - asDate1.getTime();
            long differenceDates = difference / (24 * 60 * 60 * 1000);

            //Convert long to String
            dayDifference = (int) differenceDates;

        } catch (Exception exception) {
            Log.i("DIDN'T WORK", "exception " + exception);
        }
        return dayDifference;
    }

    public static Date nextDue(reminderItem o) {
        Calendar c = Calendar.getInstance();

        try {
            c.setTime(sdf.parse(o.completed.get(0)));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        c.add(Calendar.DATE, o.frequency + 1);

        Date resultDate = new Date(c.getTimeInMillis());

        return resultDate;
    }

    @Override
    public int compareTo(@NonNull reminderItem o) {

        Date resultDate1 = nextDue(o);
        Date resultDate2 = nextDue(this);

        return daysDifference(resultDate1, resultDate2);
    }

    @Override
    public String toString() {
        return "Item: " + name + " Freq: " + frequency;
    }
}
