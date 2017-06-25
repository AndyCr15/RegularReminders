package com.androidandyuk.regularreminders;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static com.androidandyuk.regularreminders.reminderItem.daysDifference;

public class MainActivity extends AppCompatActivity {

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor ed;
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy");

    static MyReminderAdapter myAdapter;
    public static ArrayList<String> tags;
    public static ArrayList<reminderItem> reminders;

    // used to store what item might be being edited or deleted
    public static int itemLongPressedPosition = -1;
    public static reminderItem itemLongPressed = null;

    public static boolean showDate = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = this.getSharedPreferences("regularreminders", Context.MODE_PRIVATE);
        ed = sharedPreferences.edit();

        // until I implement landscape view, lock the orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        reminders = new ArrayList<>();

        loadReminders();

        ListView listView = (ListView) findViewById(R.id.reminderListView);
        myAdapter = new MyReminderAdapter(reminders);
        listView.setAdapter(myAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // this is for editing a fueling, it stores the info in itemLongPressed
                itemLongPressedPosition = position;
                itemLongPressed = reminders.get(position);

                Intent intent = new Intent(getApplicationContext(), AddReminderItem.class);
                startActivity(intent);

            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

//                final int thisPosition = position;
                final Context context = App.getContext();

                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Are you sure?")
                        .setMessage("You're about to delete this reminder forever...")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i("Removing", "Reminder " + position);
                                reminders.remove(position);
                                myAdapter.notifyDataSetChanged();
                                Toast.makeText(context, "Deleted!", Toast.LENGTH_SHORT).show();
                                saveReminders();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

                return true;
            }


        });

        Log.i("Saving sP","Beginning");
        String name = "rrSharedPref";
        int mode = MODE_PRIVATE;
        File path = new File(Environment.getExternalStorageDirectory().toString());
        File file = new File(path, "MySharedPreferences.xml");
        Log.i("Saving sP","F:" + file);
        saveSharedPreferences (name, mode, file);

    }


    private void saveSharedPreferences(String name, int mode, File file)
    {
        SharedPreferences prefs = getSharedPreferences(name, mode);
        try
        {
            FileWriter fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw);
            Map<String,?> prefsMap = prefs.getAll();
            for(Map.Entry<String,?> entry : prefsMap.entrySet())
            {
                pw.println(entry.getKey() + ": " + entry.getValue().toString());
            }
            pw.close();
            fw.close();
        }
        catch (Exception e)
        {
            Log.wtf(getClass().getName(), e.toString());
        }
    }

    public void goToAddReminderItem(View view) {
        Intent intent = new Intent(getApplicationContext(), AddReminderItem.class);
        startActivity(intent);
    }

    public class MyReminderAdapter extends BaseAdapter {
        public ArrayList<reminderItem> reminderDataAdapter;

        public MyReminderAdapter(ArrayList<reminderItem> reminderDataAdapter) {
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
            Date nextDue = reminderItem.nextDue(s);
            // shows as a date or number of days time
            if(showDate) {
                due.setText("Due on " + sdf.format(nextDue));
            } else {
                int dif = daysDifference(new Date(), nextDue);
                if(dif>0){
                    due.setText("Due in " + dif + " days");
                } else if (dif<0){
                    due.setText("" + Math.abs(dif) + " days late");
                } else due.setText("Due today");

            }

            return myView;
        }

    }

    public static void saveReminders() {
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


        for (reminderItem thisReminder : reminders) {
            Log.i("Saving Completed", "" + thisReminder);
            try {
                ArrayList<String> saveCompleted = new ArrayList<>();
                for (String thisCompleted : thisReminder.completed) {
                    saveCompleted.add(thisCompleted);
                }
                ed.putString("completed"+thisReminder.name, ObjectSerializer.serialize(saveCompleted)).apply();
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Adding Completed", "Failed attempt");
            }
        }


    }

    public static void loadReminders() {
        Log.i("Main Activity", "loadingReminders");
        int reminderCount = sharedPreferences.getInt("reminderCount", 0);

        // for testing, if reminderCourt=0, add a test
        if (reminderCount == 0) {
            reminders.add(new reminderItem("Test", "Tag", 120));
            Log.i("reminderCount", "" + reminders.size());
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
                        reminderItem newReminder = new reminderItem(name.get(x), tag.get(x), thisFrequency, "Loading");
                        Log.i("Adding", "" + x + "" + newReminder);
                        reminders.add(newReminder);
                    }
                }
            }


            for (reminderItem thisReminder : reminders) {
                thisReminder.completed.clear();
                Log.i("Loading Reminders", "" + thisReminder);
                ArrayList<String> saveCompleted = new ArrayList<>();
                try {
                    saveCompleted = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString("completed"+thisReminder.name, ObjectSerializer.serialize(new ArrayList<String>())));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("Loading Completed", "Failed attempt");
                }

                if (saveCompleted.size() > 0) {
                    // we've checked there is some info
                    for (int x = 0; x < saveCompleted.size(); x++) {
                        thisReminder.completed.add(saveCompleted.get(x));
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
        Collections.sort(reminders);
    }
}
