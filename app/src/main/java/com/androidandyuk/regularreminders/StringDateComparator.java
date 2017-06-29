package com.androidandyuk.regularreminders;

import java.text.ParseException;
import java.util.Comparator;

import static com.androidandyuk.regularreminders.MainActivity.sdf;

/**
 * Created by AndyCr15 on 28/06/2017.
 */

public class StringDateComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        try {
            return sdf.parse(o2).compareTo(sdf.parse(o1));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    return 0;
    }
}
