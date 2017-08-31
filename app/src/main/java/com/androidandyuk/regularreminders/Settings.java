package com.androidandyuk.regularreminders;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends AppCompatActivity {

    Spinner defaultTypeSpinner;
    TextView daysDefault;

    public static ImageView shield;
    public static String tag;
    View getDetails;

    public static String details;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        setContentView(R.layout.activity_settings);

        daysDefault = (TextView) findViewById(R.id.daysDefault);
        daysDefault.setText(Integer.toString(MainActivity.daysDefault));

        defaultTypeSpinner = (Spinner) findViewById(R.id.defaultTypeSpinner);
        defaultTypeSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_regular, reminderType.values()));

        switch (MainActivity.reminderDefault) {
            case "Recurring":
                defaultTypeSpinner.setSelection(0);
                break;
            case "Single":
                defaultTypeSpinner.setSelection(1);
                break;
            case "Choose":
                defaultTypeSpinner.setSelection(2);
                break;
        }

        shield = (ImageView) findViewById(R.id.shield);

    }

    public void shieldClicked(View view){
        if(getDetails.isShown()){
            getDetails.setVisibility(View.INVISIBLE);
            shield.setVisibility(View.INVISIBLE);
        }
    }

    public void getDetailsClicked(View view) {
        tag = view.getTag().toString();
        getDetails(tag);
    }

    public void getDetails(String hint) {
        Log.i("Get Details", hint);
        getDetails = findViewById(R.id.getDetails);
        getDetails.setVisibility(View.VISIBLE);
        shield.setVisibility(View.VISIBLE);
        final EditText thisET = (EditText) findViewById(R.id.getDetailsText);
        thisET.setHint(hint);

        thisET.setFocusableInTouchMode(true);
        thisET.requestFocus();

        thisET.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    details = thisET.getText().toString();
                    Log.i("Details", details);
                    getDetails.setVisibility(View.INVISIBLE);
                    shield.setVisibility(View.INVISIBLE);
                    thisET.setText(null);
                    checkDetails();
                    return true;
                }
                return false;
            }
        });
    }

    public void goToAbout(View view) {
        Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
    }

    public void exportDBPressed(View view) {
        MainActivity.exportDB();
    }

    public void importDBPressed(View view) {
        MainActivity.importDB();
    }

    public void submitPressed(View view) {
        Log.i("submitPressed","Started");
        getDetails = findViewById(R.id.getDetails);
        EditText thisET = (EditText) findViewById(R.id.getDetailsText);
        details = thisET.getText().toString();
        Log.i("Details", details);
        getDetails.setVisibility(View.INVISIBLE);
        shield.setVisibility(View.INVISIBLE);
        thisET.setText(null);
        checkDetails();
    }

    public void checkDetails() {
        Log.i("Checking Details", details);
        switch (tag) {
            case "days":
                try {
                    MainActivity.daysDefault = Integer.parseInt(details);
                    daysDefault = (TextView) findViewById(R.id.daysDefault);
                    daysDefault.setText(details);
                    MainActivity.saveSettings();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Not a valid entry", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            getDetails = findViewById(R.id.getDetails);
            if (getDetails.isShown()) {
                getDetails.setVisibility(View.INVISIBLE);
                shield.setVisibility(View.INVISIBLE);
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
        defaultTypeSpinner = (Spinner) findViewById(R.id.defaultTypeSpinner);
        MainActivity.reminderDefault = defaultTypeSpinner.getSelectedItem().toString();
        MainActivity.saveSettings();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

}
