package com.androidandyuk.regularreminders;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
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
    public static int activeReminderPosition = -1;
    public static reminderItem activeReminder = null;
    // these should be for the individual lop selected from the activeReminder
    public static int itemLongPressedPosition = -1;
    public static reminderItem itemLongPressed = null;

    public static boolean showDate = false;
    public static boolean storageAccepted;

    public static Date staticTodayDate;
    public static String staticTodayString;


    EditText reminderName;
    EditText reminderTag;
    EditText reminderFrequency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = this.getSharedPreferences("regularreminders", Context.MODE_PRIVATE);
        ed = sharedPreferences.edit();

        // until I implement landscape view, lock the orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        staticTodayDate = new Date();
        staticTodayString = sdf.format(staticTodayDate);

        reminderName = (EditText) findViewById(R.id.reminderName);
        reminderTag = (EditText) findViewById(R.id.reminderTag);
        reminderFrequency = (EditText) findViewById(R.id.reminderFrequency);

// requesting permissions to access storage and location
        String[] perms = {"android.permission.READ_EXTERNAL_STORAGE"};
        int permsRequestCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(perms, permsRequestCode);
        }


        reminders = new ArrayList<>();

        loadReminders();

        ListView listView = (ListView) findViewById(R.id.reminderListView);
        myAdapter = new MyReminderAdapter(reminders);
        listView.setAdapter(myAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // this is for editing a fueling, it stores the info in itemLongPressed
                activeReminderPosition = position;
                activeReminder = reminders.get(position);
                itemLongPressedPosition = 0;

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

        Log.i("Saving sP", "Beginning");
        String name = "rrSharedPref";
        int mode = MODE_PRIVATE;
        File path = new File(Environment.getExternalStorageDirectory().toString());
        File file = new File(path, "MySharedPreferences.xml");
        Log.i("Saving sP", "F:" + file);
        saveSharedPreferences(name, mode, file);

    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {

        switch (permsRequestCode) {

            case 200:

                storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                Log.i("Permissions STRG ", "" + storageAccepted);

                break;

        }

    }

    private void saveSharedPreferences(String name, int mode, File file) {
        SharedPreferences prefs = getSharedPreferences(name, mode);
        try {
            FileWriter fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw);
            Map<String, ?> prefsMap = prefs.getAll();
            for (Map.Entry<String, ?> entry : prefsMap.entrySet()) {
                pw.println(entry.getKey() + ": " + entry.getValue().toString());
            }
            pw.close();
            fw.close();
        } catch (Exception e) {
            Log.wtf(getClass().getName(), e.toString());
        }
    }

    public void goToAddReminderItem(View view) {
        Intent intent = new Intent(getApplicationContext(), AddReminderItem.class);
        startActivity(intent);
    }

    public void showAddReminder(View view) {
        final View addReminder = findViewById(R.id.addReminder);
        addReminder.setVisibility(View.VISIBLE);

        reminderTag.setFocusableInTouchMode(true);
//        reminderTag.requestFocus();

        reminderFrequency.setFocusableInTouchMode(true);
//        reminderFrequency.requestFocus();

        reminderName.setFocusableInTouchMode(true);
        reminderName.requestFocus();

//        reminderName.setOnKeyListener(new View.OnKeyListener() {
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                // If the event is a key-down event on the "enter" button
//                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
//                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
//                    // Perform action on key press
//                    String name = reminderName.getText().toString();
//                    String tag = reminderTag.getText().toString();
//                    int freq = Integer.parseInt(reminderFrequency.getText().toString());
//                    if(!name.equals("") && freq > 0) {
//                        addReminder.setVisibility(View.INVISIBLE);
//                        reminders.add(new reminderItem(name, tag, freq));
//                        Collections.sort(reminders);
//                        myAdapter.notifyDataSetChanged();
//                        return true;
//                    } else {
//                        Toast.makeText(MainActivity.this, "Please complete all details", Toast.LENGTH_LONG).show();
//                    }
//                }
//                return false;
//            }
//        });

        reminderFrequency.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    String name = reminderName.getText().toString();
                    String tag = reminderTag.getText().toString();
                    int freq = Integer.parseInt(reminderFrequency.getText().toString());
                    if (!name.equals("") && freq > 0) {
                        addReminder.setVisibility(View.INVISIBLE);
                        reminders.add(new reminderItem(name, tag, freq));
                        Collections.sort(reminders);
                        myAdapter.notifyDataSetChanged();
                        return true;
                    } else {
                        Toast.makeText(MainActivity.this, "Please complete all details", Toast.LENGTH_LONG).show();
                    }
                }
                return false;
            }
        });


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

            Context context = App.getContext();

            ImageView instantAdd = (ImageView) myView.findViewById(R.id.instantAdd);

            instantAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(), "Adding Today", Toast.LENGTH_SHORT).show();
                    s.completed.add(staticTodayString);
                    Collections.sort(s.completed, new StringDateComparator());
                    Collections.sort(reminders);
                    myAdapter.notifyDataSetChanged();
                    saveReminders();
                }
            });


            TextView colour = (TextView) myView.findViewById(R.id.colour);
//            colour.setBackgroundColor();

            TextView reminder = (TextView) myView.findViewById(R.id.reminder);
            reminder.setText(s.name);

            TextView due = (TextView) myView.findViewById(R.id.due);
            Date nextDue = reminderItem.nextDue(s);
            // shows as a date or number of days time
            if (showDate) {
                due.setText("Due on " + sdf.format(nextDue));
            } else {
                int dif = daysDifference(new Date(), nextDue);
                if (dif > 0) {
                    due.setText("Due in " + dif + " days");
                    due.setTextColor(ContextCompat.getColor(context, R.color.colorGreen));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorGreen));
                } else if (dif < 0) {
                    due.setText("" + Math.abs(dif) + " days late");
                    due.setTextColor(ContextCompat.getColor(context, R.color.colorRed));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorRed));
                } else {
                    due.setText("Due today");
                    due.setTextColor(ContextCompat.getColor(context, R.color.colorAmber));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAmber));
                }

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
                ed.putString("completed" + thisReminder.name, ObjectSerializer.serialize(saveCompleted)).apply();
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
                    saveCompleted = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString("completed" + thisReminder.name, ObjectSerializer.serialize(new ArrayList<String>())));
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
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            View addRedminder = findViewById(R.id.addReminder);
            if (addRedminder.getVisibility() == addRedminder.VISIBLE) {
                addRedminder.setVisibility(View.INVISIBLE);
            } else {
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
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
        Log.i("MainActivity", "onResume");
        loadReminders();
        Collections.sort(reminders);
        myAdapter.notifyDataSetChanged();
    }
}
