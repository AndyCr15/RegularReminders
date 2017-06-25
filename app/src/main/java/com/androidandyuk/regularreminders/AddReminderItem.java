package com.androidandyuk.regularreminders;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import static com.androidandyuk.regularreminders.MainActivity.itemLongPressed;
import static com.androidandyuk.regularreminders.MainActivity.itemLongPressedPosition;
import static com.androidandyuk.regularreminders.MainActivity.reminders;
import static com.androidandyuk.regularreminders.MainActivity.saveReminders;

public class AddReminderItem extends AppCompatActivity {

    static MyCompletedAdapter myAdapter;

    EditText name;
    EditText tag;
    EditText frequency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder_item);

        if(itemLongPressed!=null) {
            ListView listView = (ListView) findViewById(R.id.completedListView);
            myAdapter = new MyCompletedAdapter(reminders.get(itemLongPressedPosition).completed);
            listView.setAdapter(myAdapter);
        }

        // until I implement landscape view, lock the orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        name = (EditText) findViewById(R.id.nameET);
        tag = (EditText) findViewById(R.id.tagET);
        frequency = (EditText) findViewById(R.id.frequencyET);

        if (itemLongPressed != null) {
            Button addReminderButton = (Button) findViewById(R.id.addReminderButton);
            addReminderButton.setText("Update Reminder");
            name.setText(reminders.get(itemLongPressedPosition).name);
            tag.setText(reminders.get(itemLongPressedPosition).tag);
            frequency.setText(Integer.toString(reminders.get(itemLongPressedPosition).frequency));
        }

    }

    public void addReminder(View view) {

        if (itemLongPressed == null) {
            String freq = frequency.getText().toString();
            reminderItem thisReminder = new reminderItem(name.getText().toString(), tag.getText().toString(), Integer.parseInt(freq));
            reminders.add(thisReminder);
        } else {
            // we're editing, so just update the details
            reminders.get(itemLongPressedPosition).name = name.getText().toString();
            reminders.get(itemLongPressedPosition).tag = tag.getText().toString();
            reminders.get(itemLongPressedPosition).frequency = Integer.parseInt(frequency.getText().toString());
            resetAddItem();
        }
        saveReminders();
        myAdapter.notifyDataSetChanged();
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

            final String s = reminders.get(itemLongPressedPosition).completed.get(position);

            TextView colour = (TextView) myView.findViewById(R.id.colour);
//            colour.setBackgroundColor();

            TextView dayTV = (TextView) myView.findViewById(R.id.dayTV);
            dayTV.setText("Day");

            TextView dateTV = (TextView) myView.findViewById(R.id.dateTV);

            dateTV.setText(s);

            return myView;
        }

    }

    public void resetAddItem(){
        itemLongPressed = null;
        itemLongPressedPosition = -1;

        name = (EditText) findViewById(R.id.nameET);
        tag = (EditText) findViewById(R.id.tagET);
        frequency = (EditText) findViewById(R.id.frequencyET);

        name.setText(null);
        name.clearFocus();
        tag.setText(null);
        tag.clearFocus();
        frequency.setText(null);
        frequency.clearFocus();

        Button addReminderButton = (Button) findViewById(R.id.addReminderButton);
        addReminderButton.setText("Add Reminder");
    }

    @Override
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            resetAddItem();
            finish();
            return true;
        }
        myAdapter.notifyDataSetChanged();
        return super.onKeyDown(keyCode, event);
    }
}
