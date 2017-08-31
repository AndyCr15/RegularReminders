package com.androidandyuk.regularreminders;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static com.androidandyuk.regularreminders.MainActivity.activeReminder;
import static com.androidandyuk.regularreminders.MainActivity.activeReminderPosition;
import static com.androidandyuk.regularreminders.MainActivity.dayOfWeek;
import static com.androidandyuk.regularreminders.MainActivity.itemLongPressedPosition;
import static com.androidandyuk.regularreminders.MainActivity.reminders;
import static com.androidandyuk.regularreminders.MainActivity.saveCompletedToGoogle;
import static com.androidandyuk.regularreminders.MainActivity.saveReminderToGoogle;
import static com.androidandyuk.regularreminders.MainActivity.saveReminders;
import static com.androidandyuk.regularreminders.MainActivity.sdf;
import static com.androidandyuk.regularreminders.MainActivity.staticTodayDate;
import static com.androidandyuk.regularreminders.MainActivity.staticTodayString;

public class AddReminderItem extends AppCompatActivity {

    static MyCompletedAdapter myAdapter;

    EditText name;
    EditText tag;
    EditText frequency;
    TextView countTV;
    TextView countHeader;
    ToggleButton notifyToggle;

    private DatePickerDialog.OnDateSetListener logDateSetListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        setContentView(R.layout.activity_add_reminder_item);

        if (activeReminder != null) {
            ListView listView = (ListView) findViewById(R.id.completedListView);
            myAdapter = new MyCompletedAdapter(reminders.get(activeReminderPosition).completed);
            listView.setAdapter(myAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    // what to do if a date in the list is tapped

                    itemLongPressedPosition = position;
                    updateReminderDate();

                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

//                  final int thisPosition = position;
                    final Context context = App.getContext();

                    if (activeReminder.completed.size() > 0) {
                        new AlertDialog.Builder(AddReminderItem.this)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("Are you sure?")
                                .setMessage("You're about to delete this log forever...")
                                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Log.i("Removing", "Reminder " + position);
                                        activeReminder.completed.remove(position);
                                        Collections.sort(activeReminder.completed, new StringDateComparator());
                                        myAdapter.notifyDataSetChanged();
                                        Snackbar.make(findViewById(R.id.main), "Deleted!", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                                        saveReminders();
                                        saveCompletedToGoogle(activeReminder);
                                    }
                                })
                                .setNegativeButton("No", null)
                                .show();
                        return true;
                    } else {
                        Snackbar.make(findViewById(R.id.main), "You must keep a created date", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                        return true;
                    }
                }

            });
        }

        // until I implement landscape view, lock the orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        name = (EditText) findViewById(R.id.nameET);
        tag = (EditText) findViewById(R.id.tagET);
        frequency = (EditText) findViewById(R.id.frequencyET);
        countTV = (TextView) findViewById(R.id.countTV);
        countHeader = (TextView) findViewById(R.id.countHeader);
        notifyToggle = (ToggleButton) findViewById(R.id.notifyToggle);

        countTV.setText(Integer.toString(reminders.get(activeReminderPosition).completed.size()));
        name.setText(reminders.get(activeReminderPosition).name);
        tag.setText(reminders.get(activeReminderPosition).tag);
        frequency.setText(Integer.toString(reminders.get(activeReminderPosition).frequency));
        if(reminders.get(activeReminderPosition).frequency==0){
            frequency.setText("Single Event");
            countTV.setText("Single Event");
        }
        notifyToggle.setChecked(reminders.get(activeReminderPosition).notify);

        if (reminders.get(activeReminderPosition).notify) {
            notifyToggle.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corners));

        }

        logDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                Calendar date = Calendar.getInstance();
                date.set(year, month, day);
                //check it's not passed today, otherwise make it today
                if (reminderItem.daysDifference(date.getTime(), staticTodayDate) < 0) {
                    date = Calendar.getInstance();
                    Snackbar.make(findViewById(R.id.main), "Can't add a future date. Adding today instead.", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                }
                String sdfDate = sdf.format(date.getTime());
                activeReminder.completed.add(sdfDate);
                Collections.sort(activeReminder.completed, new StringDateComparator());
                saveReminders();
                saveCompletedToGoogle(activeReminder);
                myAdapter.notifyDataSetChanged();
            }
        };

        notifyToggle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (notifyToggle.isChecked()) {
//                    notifyToggle.setTextOff("ON");
                    notifyToggle.setChecked(true);
                    reminders.get(activeReminderPosition).notify = true;
                    notifyToggle.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corners));
                    Log.i("SetNotify", "" + reminders.get(activeReminderPosition).notify);

                } else {
//                    notifyToggle.setTextOn("OFF");
                    notifyToggle.setChecked(false);
                    reminders.get(activeReminderPosition).notify = false;
                    notifyToggle.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corners_back));
                    Log.i("SetNotify", "" + reminders.get(activeReminderPosition).notify);

                }
            }
        });

        frequency.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                try {
                    reminders.get(activeReminderPosition).frequency = Integer.parseInt(frequency.getText().toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                Collections.sort(activeReminder.completed, new StringDateComparator());
                myAdapter.notifyDataSetChanged();
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

            }
        });
    }

    public void setReminderDate(View view) {
        Log.i("setReminderDate", "Started");
        // if it's on a single event, remove it
        if(activeReminder.frequency==0){
            reminders.remove(activeReminder);
            Snackbar.make(findViewById(R.id.main), "Completed", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
            saveReminders();
            finish();
        } else {
            itemLongPressedPosition = -1;
            updateReminderDate();
        }
    }

    public void updateReminderDate() {
        Log.i("updateReminderDate", "Started");
        String thisDateString = "";
        // read the current date as we're editing and put it in thisDateString instead of staticTodayString
        if (itemLongPressedPosition >= 0) {
            thisDateString = reminders.get(activeReminderPosition).completed.get(itemLongPressedPosition);
        } else {
            thisDateString = staticTodayString;
        }


        Date thisDate = new Date();
        try {
            thisDate = sdf.parse(thisDateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // for some reason I can't getYear from thisDate, so will just use the current year
        Calendar cal = Calendar.getInstance();
        cal.setTime(thisDate);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                AddReminderItem.this,
                R.style.datepicker,
                logDateSetListener,
                year, month, day);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
        if (itemLongPressedPosition >= 0) {
            activeReminder.completed.remove(itemLongPressedPosition);
        }
    }

    public void updateReminder() {
        Log.i("updateReminder", "Started");
        // we're editing, so just update the details
        reminders.get(activeReminderPosition).name = name.getText().toString();
        reminders.get(activeReminderPosition).tag = tag.getText().toString();
        try {
            reminders.get(activeReminderPosition).frequency = Integer.parseInt(frequency.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        reminders.get(activeReminderPosition).notify = notifyToggle.isChecked();

        saveReminders();
        saveReminderToGoogle(activeReminder);
        saveCompletedToGoogle(activeReminder);

        finish();
    }

    public class MyCompletedAdapter extends BaseAdapter {
        public ArrayList<String> completedDataAdapter;

        public MyCompletedAdapter(ArrayList<String> completedDataAdapter) {
            this.completedDataAdapter = completedDataAdapter;
        }

        @Override
        public int getCount() {
            return completedDataAdapter.size();
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
            View myView = mInflater.inflate(R.layout.completed_listview, null);

            Context context = App.getContext();

            final String s = activeReminder.completed.get(position);

            TextView colour = (TextView) myView.findViewById(R.id.colour);
//            colour.setBackgroundColor();

            TextView dayTV = (TextView) myView.findViewById(R.id.dayTV);

            String thisDay = null;
            try {
                thisDay = (dayOfWeek.format(sdf.parse(s))).substring(0,3);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            dayTV.setText(thisDay);

            TextView dateTV = (TextView) myView.findViewById(R.id.dateTV);
            dateTV.setText(s);

            TextView onTime = (TextView) myView.findViewById(R.id.onTime);

            String schedule = "";
            int dif = 0;

            if (position == reminders.get(activeReminderPosition).completed.size() - 1) {
                //this is the last item in the list
                schedule = "Created";
                onTime.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorGrey));
            }

            if (position < reminders.get(activeReminderPosition).completed.size() - 1) {
                // we're in the list somewhere

                try {
                    dif = (reminderItem.daysDifference(sdf.parse(s), sdf.parse(activeReminder.completed.get(position + 1)))) + activeReminder.frequency;
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (dif < -2) {
                    schedule = Math.abs(dif) + " days late";
                }

                if (dif > 0) {
                    schedule = Math.abs(dif) + " days early";
                }

                if (dif < 0 && dif > -3) {
                    schedule = Math.abs(dif) + " days late";
                }

                if (dif == 0) {
                    schedule = "On schedule";
                }

                Double thisSchedule = ((double) dif) / reminders.get(activeReminderPosition).frequency;

                // now set the colour, start at worse and work forwards
                onTime.setTextColor(ContextCompat.getColor(context, R.color.colorDarkRed));
                colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDarkRed));

                if (thisSchedule > -2) {
                    onTime.setTextColor(ContextCompat.getColor(context, R.color.colorRed));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorRed));
                }

                if (thisSchedule > -1) {
                    onTime.setTextColor(ContextCompat.getColor(context, R.color.colorAmberRed));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAmberRed));
                }

                if (thisSchedule > -0.5) {
                    onTime.setTextColor(ContextCompat.getColor(context, R.color.colorAmber));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAmber));
                }

                if (thisSchedule > -0.2) {
                    onTime.setTextColor(ContextCompat.getColor(context, R.color.colorYellow));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorYellow));
                }

                if (thisSchedule > 0) {
                    onTime.setTextColor(ContextCompat.getColor(context, R.color.colorGreenYellow));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorGreenYellow));
                }

                if (thisSchedule > 0.2) {
                    onTime.setTextColor(ContextCompat.getColor(context, R.color.colorLightGreen));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorLightGreen));
                }

                if (thisSchedule > 0.6) {
                    onTime.setTextColor(ContextCompat.getColor(context, R.color.colorGreen));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorGreen));
                }


            }
            onTime.setText(schedule);
            return myView;
        }

    }

    public void resetAddItem() {
        activeReminder = null;
        activeReminderPosition = -1;

        name = (EditText) findViewById(R.id.nameET);
        tag = (EditText) findViewById(R.id.tagET);
        frequency = (EditText) findViewById(R.id.frequencyET);

//        name.setText(null);
//        name.clearFocus();
//        tag.setText(null);
//        tag.clearFocus();
//        frequency.setText(null);
//        frequency.clearFocus();
    }

    @Override
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            updateReminder();
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }
}
