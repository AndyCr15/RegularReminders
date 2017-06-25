package com.androidandyuk.regularreminders;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by AndyCr15 on 24/06/2017.
 */

public class reminderItem implements Comparable<reminderItem> {
    String name;
    String tag;
    int frequency;
    List<Calendar> completed;

    public reminderItem(String name, String tag, int frequency) {
        this.name = name;
        this.tag = tag;
        this.frequency = frequency;
        this.completed = new ArrayList<>();
        this.completed.add(Calendar.getInstance());
//        Calendar testDate = new GregorianCalendar(2017, Calendar.JUNE, 18);
//        this.completed.add(testDate);
//        Log.i("TestDate","" + testDate);
    }

    public int nextDue(){
        Calendar now = Calendar.getInstance();
        Calendar last = this.completed.get(0);
        last.add(Calendar.HOUR_OF_DAY, this.frequency);

        int lastDay = last.DAY_OF_YEAR;
        int nowDay = now.DAY_OF_YEAR;

        return lastDay - nowDay;
    }

    @Override
    public String toString() {
        return name + " last completed " + this.completed.get(0);
    }


    @Override
    public int compareTo(@NonNull reminderItem o) {
        if(this.completed.get(0).getTime().before(o.completed.get(0).getTime())){
            return -1;
        } else {
            return 1;
        }
    }
}
