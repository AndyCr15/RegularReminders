package com.androidandyuk.regularreminders;

import android.support.annotation.NonNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import static com.androidandyuk.regularreminders.MainActivity.sdf;

/**
 * Created by AndyCr15 on 24/06/2017.
 */

public class reminderItem implements Comparable<reminderItem> {
    String name;
    String tag;
    int frequency;
    ArrayList<String> completed;

    public reminderItem(String name, String tag, int frequency) {
        this.name = name;
        this.tag = tag;
        this.frequency = frequency;
        this.completed = new ArrayList<>();
        this.completed.add(sdf.format(new Date()));
    }

    public Long nextDue() {
        Date now = new Date();
        Date last = null;
        try {
            last = sdf.parse(this.completed.get(0));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return Math.abs(last.getTime()) + Math.abs(frequency) - Math.abs(now.getTime());
    }

//    @Override
//    public String toString() {
//        return name + " last completed " + this.completed.get(0);
//    }


    @Override
    public int compareTo(@NonNull reminderItem o) {
        try {
            return (int) (Math.abs(sdf.parse(this.completed.get(0)).getTime()) - Math.abs(sdf.parse(o.completed.get(0)).getTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
