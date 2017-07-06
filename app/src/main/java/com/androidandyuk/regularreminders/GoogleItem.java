package com.androidandyuk.regularreminders;

/**
 * Created by AndyCr15 on 04/07/2017.
 */

public class GoogleItem {
    String Name;
    String Tag;
    int Freq;
    String Completed;

    public GoogleItem() {
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getTag() {
        return Tag;
    }

    public void setTag(String tag) {
        Tag = tag;
    }

    public int getFreq() {
        return Freq;
    }

    public void setFreq(int freq) {
        Freq = freq;
    }

    public String getCompleted() {
        return Completed;
    }

    public void setCompleted(String completed) {
        Completed = completed;
    }

    @Override
    public String toString() {
        return this.getName() + " Tag : " + this.getTag();
    }
}
