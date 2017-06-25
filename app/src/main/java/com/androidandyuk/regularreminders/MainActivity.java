package com.androidandyuk.regularreminders;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor ed;

    static MyLocationAdapter myAdapter;
    public static ArrayList<String> tags;
    public static ArrayList<reminderItem> reminders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = this.getSharedPreferences("regularreminders", Context.MODE_PRIVATE);
        ed = sharedPreferences.edit();

        reminders = new ArrayList<>();

        loadReminders();

        ListView listView = (ListView) findViewById(R.id.reminderListView);
        myAdapter = new MyLocationAdapter(reminders);
        listView.setAdapter(myAdapter);

//        tags.add("Work");
//        tags.add("Family");


    }

    public class MyLocationAdapter extends BaseAdapter {
        public ArrayList<reminderItem> reminderDataAdapter;

        public MyLocationAdapter(ArrayList<reminderItem> reminderDataAdapter) {
            this.reminderDataAdapter = reminderDataAdapter;
        }

        @Override
        public int getCount() {
            return reminderDataAdapter.size();
        }

        @Override
        public String getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater mInflater = getLayoutInflater();
            View myView = mInflater.inflate(R.layout.reminder_listview, null);

            final reminderItem s = reminderDataAdapter.get(position);

            TextView colour = (TextView) myView.findViewById(R.id.colour);
//            colour.setBackgroundColor();

            TextView reminder = (TextView) myView.findViewById(R.id.reminder);
            reminder.setText(s.name);

            TextView due = (TextView) myView.findViewById(R.id.due);
            int nextDue = s.nextDue();
            due.setText("due in " + nextDue + " days");

            return myView;
        }

    }

    public void saveReminders() {
        ed.putInt("reminderCount", reminders.size()).apply();

        ArrayList<String> name = new ArrayList<>();
        ArrayList<String> tag = new ArrayList<>();
        ArrayList<String> frequency = new ArrayList<>();

        for (reminderItem thisReminder : reminders) {

            Log.i("SavingReminders", "" + thisReminder);

            name.add(thisReminder.name);
            tag.add(thisReminder.tag);
            frequency.add(Integer.toString(thisReminder.frequency));

        }

        try {
            ed.putString("name", ObjectSerializer.serialize(name)).apply();
            ed.putString("tag", ObjectSerializer.serialize(tag)).apply();
            ed.putString("frequency", ObjectSerializer.serialize(frequency)).apply();
        } catch (IOException e) {
            Log.i("Adding details", "Failed attempt");
            e.printStackTrace();
        }



    }

    public void loadReminders() {
        Log.i("Main Activity", "loadingReminders");
        int reminderCount = sharedPreferences.getInt("reminderCount", 0);

        // for testing, if reminderCourt=0, add a test
        if (reminderCount == 0) {
            reminders.add(new reminderItem("Test", "Tag", 120));
            Log.i("reminderCount","" + reminders.size());
            saveReminders();
        } else {

            Log.i("ReminderSize", "" + reminderCount);
            reminders.clear();

            ArrayList<String> name = new ArrayList<>();
            ArrayList<String> tag = new ArrayList<>();
            ArrayList<String> frequency = new ArrayList<>();

            try {

                name = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString("name", ObjectSerializer.serialize(new ArrayList<String>())));
                tag = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString("tag", ObjectSerializer.serialize(new ArrayList<String>())));
                frequency = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString("frequency", ObjectSerializer.serialize(new ArrayList<String>())));

                Log.i("Reminders Restored ", "Count :" + name.size());
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Loading Reminders", "Failed attempt");
            }

            if (name.size() > 0 && tag.size() > 0 && frequency.size() > 0) {
                // we've checked there is some info
                if (name.size() == tag.size() && tag.size() == frequency.size()) {
                    // we've checked each item has the same amount of info, nothing is missing
                    for (int x = 0; x < name.size(); x++) {
                        int thisFrequency = Integer.parseInt(frequency.get(x));
                        reminderItem newReminder = new reminderItem(name.get(x), tag.get(x), thisFrequency);
                        Log.i("Adding", "" + x + "" + newReminder);
                        reminders.add(newReminder);
                    }
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MainActivty", "onPause");
        saveReminders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MainActivty", "onResume");
        loadReminders();
    }
}
