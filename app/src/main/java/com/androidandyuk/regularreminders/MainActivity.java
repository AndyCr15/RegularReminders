package com.androidandyuk.regularreminders;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static com.androidandyuk.regularreminders.reminderItem.daysDifference;
import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private static Boolean scrolling = false;

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor ed;
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy");
    public static SimpleDateFormat dayOfWeek = new SimpleDateFormat("EEEE");

    public static FirebaseAuth mAuth;
    public static FirebaseAuth.AuthStateListener mAuthListener;
    public static FirebaseDatabase database;
    public static FirebaseUser user;
    public static DatabaseReference myRef;

    String signInOut = "Sign In";

    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient mGoogleApiClient;
    public static Boolean syncGoogle = false;

    static MyReminderAdapter myAdapter;
    public static ArrayList<String> tags;
    public static ArrayList<reminderItem> reminders;

    // used to store what item might be being edited or deleted
    public static int activeReminderPosition = -1;
    public static reminderItem activeReminder = null;
    // these should be for the individual lop selected from the activeReminder
    public static int itemLongPressedPosition = -1;

    public static boolean showDate = false;
    public static boolean storageAccepted;

    public static Date staticTodayDate;
    public static String staticTodayString;

    public static SQLiteDatabase remindersDB;

    // the time the reminders will notify
    public static int reminderHour = 10;
    public static int reminderMinute = 0;

    public static String reminderDefault = "Recurring";
    public static String advancedWarning = "One";
    public static int daysDefault = 7;

    static final int TIME_DIALOG_ID = 0;

    EditText reminderName;
    EditText reminderTag;
    EditText reminderFrequency;
    TextView recurringTV;
    TextView singleTV;
    View addReminder;
    FloatingActionButton floatingActionButton;
    public static ImageView shield;

    public static int animTime = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        setContentView(R.layout.activity_main);

        Log.i("onCreate", "Starting");

        sharedPreferences = this.getSharedPreferences("regularreminders", Context.MODE_PRIVATE);
        ed = sharedPreferences.edit();

        loadSettings();

        // until I implement landscape view, lock the orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        remindersDB = this.openOrCreateDatabase("Reminders", MODE_PRIVATE, null);

        staticTodayDate = new Date();
        staticTodayString = sdf.format(staticTodayDate);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        reminderName = (EditText) findViewById(R.id.reminderName);
        reminderTag = (EditText) findViewById(R.id.reminderTag);
        reminderFrequency = (EditText) findViewById(R.id.reminderFrequency);

        addReminder = findViewById(R.id.addReminder);
        recurringTV = (TextView) findViewById(R.id.addRecurringTV);
        singleTV = (TextView) findViewById(R.id.addSingleTV);

        floatingActionButton = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        shield = (ImageView) findViewById(R.id.shield);

        // requesting permissions to access storage and location
        String[] perms = {"android.permission.READ_EXTERNAL_STORAGE"};
        int permsRequestCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(perms, permsRequestCode);
        }

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        reminders = new ArrayList<>();

        loadReminders();

        ListView listView = (ListView) findViewById(R.id.reminderListView);
        myAdapter = new MyReminderAdapter(reminders);
        listView.setAdapter(myAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
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
                                if (user != null) {
                                    myRef.child(user.getUid()).child(reminders.get(position).reminderID).removeValue();
                                }
                                reminders.remove(position);
                                myAdapter.notifyDataSetChanged();
                                Snackbar.make(findViewById(R.id.main), "Deleted", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                                saveReminders();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

                return true;
            }

        });

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // TODO Auto-generated method stub
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub

                if (scrollState == 0) {
                    scrolling = false;
                } else {
                    scrolling = true;
                }
            }
        });

        floatingActionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showFABOptions();
                return true;
            }
        });

        File dir = getDatabasePath("reminders");
        Log.i("DBPath", "" + dir.getAbsolutePath());

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Snackbar.make(findViewById(R.id.main), "Error", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d("Login", "onAuthStateChanged:signed_in:" + user.getUid());
                    Snackbar.make(findViewById(R.id.main), "Signed in as " + user.getDisplayName(), Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                    Log.i("SignedIn", "onAuthStateChanged");
                    loadFromGoogle();
                    myAdapter.notifyDataSetChanged();
                    invalidateOptionsMenu();
                    setToolbarUser();
                    setLogInOut();
                } else {
                    // User is signed out
                    Log.d("Login", "onAuthStateChanged:signed_out");
                    invalidateOptionsMenu();
                    setToolbarUser();
                    setLogInOut();
                }
            }
        };

        mAuth = FirebaseAuth.getInstance();

        mAuth.addAuthStateListener(mAuthListener);

        setNotiMenu();

        Collections.sort(reminders);
        myAdapter.notifyDataSetChanged();

    }

    private void setLogInOut() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu menu = navigationView.getMenu();
        MenuItem logText = menu.findItem(R.id.nav_logout);

        if (user == null) {
            logText.setTitle("Log in");
        } else {
            logText.setTitle("Log out");
        }
    }

    public void setNotiMenu() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // get menu from navigationView
        Menu menu = navigationView.getMenu();

        // find MenuItem you want to change
        MenuItem nav_set_time = menu.findItem(R.id.nav_set_time);

        // set new title to the MenuItem
        String mins = "" + reminderMinute;
        if (reminderMinute < 10) {
            mins = "0" + reminderMinute;
        }
        nav_set_time.setTitle("Notification Time : " + reminderHour + ":" + mins);
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        Log.i("onRequestPermissionsRes", "Starting");
        switch (permsRequestCode) {

            case 200:

                storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                Log.i("Permissions STRG ", "" + storageAccepted);

                break;

        }

    }

    public void setRecurring(Context context) {
        Log.i(TAG, "setRecurring");

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        SharedPreferences sharedPreferences = context.getSharedPreferences("regularreminders", Context.MODE_PRIVATE);
        int reminderHour = sharedPreferences.getInt("reminderHour", 10);
        int reminderMinute = sharedPreferences.getInt("reminderMinute", 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, reminderHour);
        calendar.set(Calendar.MINUTE, reminderMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);

        Log.i(TAG, "AlarmSet " + calendar);

    }

    public void fabClicked(View view) {
        String tag = view.getTag().toString();
        switch (tag) {
            case "default":
                if (reminderDefault.equals("Choose")) {
                    showFABOptions();
                } else {
                    showAddReminder(reminderDefault);
                }
                break;
            case "Single":
                hideFABOptions();
                showAddReminder(tag);
                break;
            case "Recurring":
                hideFABOptions();
                showAddReminder(tag);
                break;
        }
    }

    public void showFABOptions() {
        Animation sFadeIn = new AlphaAnimation(0, 1);
        sFadeIn.setStartOffset(animTime / 2);
        sFadeIn.setDuration(animTime);
        AnimationSet sAnimation = new AnimationSet(true);
        sAnimation.addAnimation(sFadeIn);

        Animation rFadeIn = new AlphaAnimation(0, 1);
        rFadeIn.setDuration(animTime);
        AnimationSet rAnimation = new AnimationSet(true);
        rAnimation.addAnimation(rFadeIn);

        Animation shFadeIn = new AlphaAnimation(0, 1);
        shFadeIn.setDuration(animTime);
        AnimationSet shAnimation = new AnimationSet(true);
        shAnimation.addAnimation(shFadeIn);

        recurringTV.setVisibility(View.VISIBLE);
        singleTV.setVisibility(View.VISIBLE);
        shield.setVisibility(View.VISIBLE);

        shield.startAnimation(shAnimation);
        recurringTV.startAnimation(rAnimation);
        singleTV.startAnimation(sAnimation);

    }

    public void hideFABOptions() {
        if (recurringTV.isShown()) {
            Animation sFadeOut = new AlphaAnimation(1, 0);

            sFadeOut.setDuration(animTime);
            AnimationSet sAnimation = new AnimationSet(true);
            sAnimation.addAnimation(sFadeOut);

            Animation rFadeOut = new AlphaAnimation(1, 0);
            rFadeOut.setStartOffset(animTime / 2);
            rFadeOut.setDuration(animTime);
            AnimationSet rAnimation = new AnimationSet(true);
            rAnimation.addAnimation(rFadeOut);

            Animation shFadeOut = new AlphaAnimation(1, 0);
            shFadeOut.setStartOffset(animTime / 2);
            shFadeOut.setDuration(animTime);
            AnimationSet shAnimation = new AnimationSet(true);
            shAnimation.addAnimation(shFadeOut);

            shield.startAnimation(shAnimation);
            singleTV.startAnimation(sAnimation);
            recurringTV.startAnimation(rAnimation);
        }

        recurringTV.setVisibility(View.INVISIBLE);
        singleTV.setVisibility(View.INVISIBLE);
        shield.setVisibility(View.INVISIBLE);
    }

    public void showAddReminder(String reason) {

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setStartOffset(animTime / 2);
        fadeIn.setDuration(animTime);
        AnimationSet animation = new AnimationSet(true);
        animation.addAnimation(fadeIn);

        Animation shFadeIn = new AlphaAnimation(0, 1);
        shFadeIn.setDuration(animTime);
        AnimationSet shAnimation = new AnimationSet(true);
        shAnimation.addAnimation(shFadeIn);

        addReminder.setVisibility(View.VISIBLE);
        shield.setVisibility(View.VISIBLE);

        shield.startAnimation(shAnimation);
        addReminder.startAnimation(animation);


        Log.i("showAddReminder", "Starting");

        reminderFrequency.setText("");
        reminderTag.setFocusableInTouchMode(true);
        reminderFrequency.append(Integer.toString(daysDefault));
        reminderFrequency.setFocusableInTouchMode(true);

        reminderName.setFocusableInTouchMode(true);
        reminderName.requestFocus();

        reminderFrequency.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    String name = reminderName.getText().toString();
                    String tag = reminderTag.getText().toString();

                    int freq = 0;
                    try {
                        freq = parseInt(reminderFrequency.getText().toString());
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    if (!name.equals("") && freq >= 0) {
                        addReminder(name, tag, freq);
                        return true;
                    } else {
                        Snackbar.make(findViewById(R.id.main), "Please complete all details", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                    }
                }
                return false;
            }
        });

        if (reason.equals("Single")) {
            // this will be a one off reminder, with frequency of zero
            reminderFrequency.setText("Single Event");
            reminderFrequency.setFocusable(false);
            reminderFrequency.setFocusableInTouchMode(false);
            reminderFrequency.setClickable(false);

            reminderName.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    // If the event is a key-down event on the "enter" button
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                            (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        // Perform action on key press
                        String name = reminderName.getText().toString();
                        String tag = reminderTag.getText().toString();
                        if (!name.equals("")) {
                            addReminder(name, tag, 0);
                            return true;
                        } else {
                            Snackbar.make(findViewById(R.id.main), "Please complete all details", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                        }
                    }
                    return false;
                }
            });

            reminderTag.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    // If the event is a key-down event on the "enter" button
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                            (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        // Perform action on key press
                        String name = reminderName.getText().toString();
                        String tag = reminderTag.getText().toString();
                        if (!name.equals("")) {
                            addReminder(name, tag, 0);
                            return true;
                        } else {
                            Snackbar.make(findViewById(R.id.main), "Please complete all details", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                        }
                    }
                    return false;
                }
            });
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void addReminder(String name, String tag, int freq) {
        hideReminder();
        reminderItem newReminder = new reminderItem(name, tag, freq);
        reminders.add(newReminder);
        saveReminderToGoogle(newReminder);
        saveCompletedToGoogle(newReminder);

        reminderName.setText("");
        reminderTag.setText("");
        reminderFrequency.setText("");

        View thisView = this.getCurrentFocus();
        if (thisView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(thisView.getWindowToken(), 0);
        }

        Collections.sort(reminders);
        myAdapter.notifyDataSetChanged();
        saveReminders();
    }

    public void shieldClicked(View view) {
        hideFABOptions();
        hideReminder();

        View thisView = this.getCurrentFocus();
        if (thisView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(thisView.getWindowToken(), 0);
        }
    }

    private void hideReminder() {
        if (addReminder.isShown()) {
            Animation sFadeOut = new AlphaAnimation(1, 0);
            sFadeOut.setDuration(animTime);
            AnimationSet sAnimation = new AnimationSet(true);
            sAnimation.addAnimation(sFadeOut);

            Animation shFadeOut = new AlphaAnimation(1, 0);
            shFadeOut.setStartOffset(animTime / 2);
            shFadeOut.setDuration(animTime);
            AnimationSet shAnimation = new AnimationSet(true);
            shAnimation.addAnimation(shFadeOut);

            addReminder.startAnimation(sAnimation);
            shield.startAnimation(shAnimation);
        }

        addReminder.setVisibility(View.INVISIBLE);
        shield.setVisibility(View.INVISIBLE);
    }

    public void checkNotifications(Context context) {
        Log.i(TAG, "checkNotifications");

        ArrayList<reminderItem> theseReminders = new ArrayList<>();
        ArrayList<String> overdue = new ArrayList();

        theseReminders.clear();

        SharedPreferences sharedPreferences = context.getSharedPreferences("regularreminders", Context.MODE_PRIVATE);

        SQLiteDatabase remindersDB = context.openOrCreateDatabase("Reminders", MODE_PRIVATE, null);

        // load in the reminders from the database
        try {

            Cursor c = remindersDB.rawQuery("SELECT * FROM reminders", null);

            int idIndex = c.getColumnIndex("id");
            int nameIndex = c.getColumnIndex("name");
            int tagIndex = c.getColumnIndex("tag");
            int freqIndex = c.getColumnIndex("freq");
            int notifyIndex = c.getColumnIndex("notify");
            int completedIndex = c.getColumnIndex("completed");

            c.moveToFirst();

            do {
                ArrayList<String> completed;

                completed = (ArrayList<String>) ObjectSerializer.deserialize(c.getString(completedIndex));

                Boolean thisNotify = (c.getInt(notifyIndex) == 1) ? true : false;

                reminderItem newReminder = new reminderItem(c.getString(idIndex), c.getString(nameIndex), c.getString(tagIndex), c.getInt(freqIndex), thisNotify, "Loading");

                for (String thisCompleted : completed) {
                    newReminder.completed.add(thisCompleted);
                }
                theseReminders.add(newReminder);

            } while (c.moveToNext());


        } catch (Exception e) {

            Log.i("LoadingDB", "Caught Error");
            e.printStackTrace();

        }

        String advancedWarning = sharedPreferences.getString("advancedWarning", "One");
        Long advancedWarningMillis = 86400000L;

        switch(advancedWarning){
            case "Due":
                advancedWarningMillis = 0L;
                break;
            case "One":
                advancedWarningMillis = 86400000L;
                break;
            case "Two":
                advancedWarningMillis =172800000L;
                break;
        }

        Calendar nowCal = Calendar.getInstance();
        nowCal.set(Calendar.HOUR_OF_DAY,24);
        nowCal.set(Calendar.MINUTE,0);
        nowCal.set(Calendar.SECOND,0);
        nowCal.set(Calendar.MILLISECOND,0);
        Long now = nowCal.getTimeInMillis();

        if (theseReminders.size() >= 0) {
            // there are reminders, now find the last one to set the notification
            int lastNotify = -1;
            for (int i = 0 ; i < theseReminders.size(); i++) {
                //check if it's due/overdue
                Date date = reminderItem.nextDue(theseReminders.get(i));
                Long dueMillis = date.getTime();

                if (now + advancedWarningMillis >= dueMillis) {
                    // check if this reminders is set to notify
                    if (theseReminders.get(i).notify) {
                        lastNotify = i;
                    }
                }
            }

            //only do the rest if we have a notification to send
            if (lastNotify >= 0) {
                // load the overdue into it's own Array
                for (int i = 0; i <= lastNotify; i++) {

                    Date nextDue = reminderItem.nextDue(theseReminders.get(i));
                    String due = "";

                    int dif = daysDifference(new Date(), nextDue);

                    // first set the text
                    if (dif > 1) {
                        due = " due in " + dif + " days";
                    }
                    if (dif == 1) {
                        due = " due tomorrow";
                    }
                    if (dif < 0) {
                        if (Math.abs(dif) == 1) {
                            due = " now " + Math.abs(dif) + " day late";
                        } else {
                            due = " now " + Math.abs(dif) + " days late";
                        }
                    }
                    if (dif == 0) {
                        due = "Due today";
                    }

                    String message = theseReminders.get(i).name + due;

                    overdue.add(message);
                }


                Log.i("setNotification", "Using " + theseReminders.get(lastNotify));
                Log.i("lastNotify", "" + lastNotify);


                int notificationID = 100;

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                Intent resultIntent = new Intent(context, MainActivity.class);
                resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // The id of the channel.
                    String id = "my_channel_01";
                    // The user-visible name of the channel.
                    CharSequence name = "Channel Name";
                    // The user-visible description of the channel.
                    String description = "Channel Desc";
                    int importance = NotificationManager.IMPORTANCE_LOW;
                    NotificationChannel mChannel = new NotificationChannel(id, name, importance);
                    // Configure the notification channel.
                    mChannel.setDescription(description);
                    mChannel.enableLights(true);
                    // Sets the notification light color for notifications posted to this
                    // channel, if the device supports this feature.
                    mChannel.setLightColor(Color.BLUE);
                    mChannel.enableVibration(true);
                    mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                    notificationManager.createNotificationChannel(mChannel);


                    // Create a notification and set the notification channel.
                    Notification.Builder notification = new Notification.Builder(context)
                            .setContentIntent(pendingIntent)
                            .setContentTitle("Check your reminders!")
                            .setSmallIcon(R.drawable.rr)
                            .setChannelId(id)
                            .setAutoCancel(true);

                    Notification.InboxStyle inboxStyle =
                            new Notification.InboxStyle();

                    // Sets a title for the Inbox in expanded layout
                    inboxStyle.setBigContentTitle("You have the following overdue:");

                    // Moves events into the expanded layout
                    for (int i = 0; i < overdue.size(); i++) {
                        inboxStyle.addLine(overdue.get(i));
                    }
                    // Moves the expanded layout object into the notification object.
                    notification.setStyle(inboxStyle);

                    // Issue the notification.
                    notificationManager.notify(notificationID, notification.build());

                } else {

                    Notification.Builder notification = new Notification.Builder(context)
                            .setContentIntent(pendingIntent)
                            .setContentTitle("Check your reminders!")
                            .setSmallIcon(R.drawable.rr)
                            .setAutoCancel(true);

                    Notification.InboxStyle inboxStyle =
                            new Notification.InboxStyle();

                    // Sets a title for the Inbox in expanded layout
                    inboxStyle.setBigContentTitle("You have the following overdue:");

                    // Moves events into the expanded layout
                    for (int i = 0; i < overdue.size(); i++) {
                        inboxStyle.addLine(overdue.get(i));
                    }
                    // Moves the expanded layout object into the notification object.
                    notification.setStyle(inboxStyle);

                    // Issue the notification.
                    notificationManager.notify(notificationID, notification.build());
                }
            }
        }
    }

    public class MyReminderAdapter extends BaseAdapter {
        public ArrayList<reminderItem> reminderDataAdapter;

        private int lastPosition = -1;

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

            TextView colour = myView.findViewById(R.id.colour);
//            colour.setBackgroundColor();

            TextView reminder = myView.findViewById(R.id.reminder);
            reminder.setText(s.name);
            if (s.notify) {
                reminder.setTypeface(null, Typeface.BOLD);
            }
            if (s.frequency == 0) {
                reminder.setTextColor(getResources().getColor(R.color.colorAccent));
            }

            TextView due = myView.findViewById(R.id.due);

            due.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(findViewById(R.id.main), "Adding Today", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                    // if it's a single entry, remove it, else add completed.
                    if (s.frequency == 0) {
                        if (user != null) {
                            myRef.child(user.getUid()).child(s.reminderID).removeValue();
                        }
                        reminders.remove(s);
                        Snackbar.make(findViewById(R.id.main), "Completed", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                    } else {
                        s.completed.add(staticTodayString);
                    }
                    Collections.sort(s.completed, new StringDateComparator());
                    Collections.sort(reminders);
                    myAdapter.notifyDataSetChanged();
                    saveReminders();
                    saveCompletedToGoogle(s);
                }
            });

            Date nextDue = reminderItem.nextDue(s);

            int dif = daysDifference(new Date(), nextDue);
            // shows as a date or number of days time

            if (showDate) {
                due.setText("Due on " + sdf.format(nextDue));
            } else {

                // first set the text
                if (dif > 0) {
                    due.setText("Due in " + dif + " days");
                }
                if (dif < 0) {
                    due.setText("" + Math.abs(dif) + " days late");
                    due.setTextColor(ContextCompat.getColor(context, R.color.colorRed));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorRed));
                }
                if (dif == 0) {
                    due.setText("Due today");
                    due.setTextColor(ContextCompat.getColor(context, R.color.colorAmber));
                    colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAmber));
                }

            }

            Double schedule = ((double) dif) / s.frequency;
            if (s.frequency == 0) {
                schedule = ((double) dif) / 7;
            }

            // now set the colour, start at worse and work forwards
            due.setTextColor(ContextCompat.getColor(context, R.color.colorDarkRed));
            colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDarkRed));

            if (schedule > -2) {
                due.setTextColor(ContextCompat.getColor(context, R.color.colorRed));
                colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorRed));
            }

            if (schedule > -1) {
                due.setTextColor(ContextCompat.getColor(context, R.color.colorAmberRed));
                colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAmberRed));
            }

            if (schedule > -0.5) {
                due.setTextColor(ContextCompat.getColor(context, R.color.colorAmber));
                colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAmber));
            }

            if (schedule > -0.2) {
                due.setTextColor(ContextCompat.getColor(context, R.color.colorYellow));
                colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorYellow));
            }

            if (schedule > 0) {
                due.setTextColor(ContextCompat.getColor(context, R.color.colorGreenYellow));
                colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorGreenYellow));
            }

            if (schedule > 0.2) {
                due.setTextColor(ContextCompat.getColor(context, R.color.colorLightGreen));
                colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorLightGreen));
            }

            if (schedule > 0.6) {
                due.setTextColor(ContextCompat.getColor(context, R.color.colorGreen));
                colour.setBackgroundColor(ContextCompat.getColor(context, R.color.colorGreen));
            }

            if (scrolling) {
                Animation animation = AnimationUtils.loadAnimation(context, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
                myView.startAnimation(animation);
                lastPosition = position;
            }

            return myView;
        }

    }

    public static void saveReminderToGoogle(reminderItem saveReminder) {
        Log.i("saveReminderToGoogle", "Starting");
        if (user != null) {
            Log.i("saveReminderToGoogle", "user is not null");

            // clear anything previously saved for this reminder (not completed, that's done separately)
//            myRef.child(user.getUid()).child(saveReminder.reminderID).removeValue();

            // save back the reminder
            myRef.child(user.getUid()).child(saveReminder.reminderID).child("Name").setValue(saveReminder.name);
            myRef.child(user.getUid()).child(saveReminder.reminderID).child("Tag").setValue(saveReminder.tag);
            myRef.child(user.getUid()).child(saveReminder.reminderID).child("Freq").setValue(Integer.toString(saveReminder.frequency));
            myRef.child(user.getUid()).child(saveReminder.reminderID).child("Notify").setValue(Boolean.toString(saveReminder.notify));
        } else {
            Log.i("saveReminderToGoogle", "user IS null");
        }
    }

    public static void saveCompletedToGoogle(reminderItem saveReminder) {
        if (user != null) {
            Log.i("saveCompletedToGoogle", "user is not null");
            // clear anything previously saved for this item completed
//            myRef.child(user.getUid()).child(saveReminder.reminderID).child("Completed").removeValue();

            // save back the completed for this user
            try {
                myRef.child(user.getUid()).child(saveReminder.reminderID).child("Completed").setValue(ObjectSerializer.serialize(saveReminder.completed));
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("saveToGoogle", "Error saving completed");
            }
        }
    }

    public static void saveToGoogle() {
        Log.i("saveToGoogle", "syncGoogle :" + syncGoogle);
        if (user != null) {
            if (!syncGoogle) {
                Log.i("saveToGoogle", "Removing old data from Google");
                myRef.child(user.getUid()).removeValue();
            }
            // save back the reminders for this user
            for (reminderItem thisReminder : reminders) {
                Log.i("saveToGoogle", "" + thisReminder.reminderID);
                myRef.child(user.getUid()).child(thisReminder.reminderID).child("Name").setValue(thisReminder.name);
                myRef.child(user.getUid()).child(thisReminder.reminderID).child("Tag").setValue(thisReminder.tag);
                myRef.child(user.getUid()).child(thisReminder.reminderID).child("Freq").setValue(Integer.toString(thisReminder.frequency));
                myRef.child(user.getUid()).child(thisReminder.reminderID).child("Notify").setValue(Boolean.toString(thisReminder.notify));
                try {
                    myRef.child(user.getUid()).child(thisReminder.reminderID).child("Completed").setValue(ObjectSerializer.serialize(thisReminder.completed));
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("saveToGoogle", "Error saving completed " + thisReminder.name);
                }
            }
        } else {
            Log.i("saveToGoogle", "No user logged in");
        }
    }

    public static void loadFromGoogle() {
        Log.i("loadFromGoogle : " + user, "syncGoogle :" + syncGoogle);
        if (user != null && !syncGoogle) {

            Log.i("loadFromGoogle", "noSync");

            myRef.child(user.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.i("loadFromGoogle", "onDataChange");
                    reminders.clear();

                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        Log.i("DataSnapshot", "" + ds);
                        if (ds.getChildrenCount() == 5) {
                            GenericTypeIndicator<Map<String, String>> genericTypeIndicator = new GenericTypeIndicator<Map<String, String>>() {
                            };
                            Map<String, String> map = ds.getValue(genericTypeIndicator);

                            String name = map.get("Name");
                            String tag = map.get("Tag");
                            int freq = parseInt(map.get("Freq"));
                            Boolean notify = Boolean.parseBoolean(map.get("Notify"));
                            String completed = map.get("Completed");

                            ArrayList<String> completedArray = new ArrayList<>();
                            try {
                                completedArray = (ArrayList<String>) ObjectSerializer.deserialize(completed);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Log.i("getKey", "" + ds.getKey());
                            reminderItem newReminder = new reminderItem(ds.getKey(), name, tag, freq, notify, "Loading");

                            for (String thisCompleted : completedArray) {
                                newReminder.completed.add(thisCompleted);
                            }

                            reminders.add(newReminder);
                        }
                    }
                    Collections.sort(reminders);
                    myAdapter.notifyDataSetChanged();
                    saveReminders();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else if (user != null && syncGoogle) {
            // skip the initial load of overwrite is false, but then set back to true for future loads
            saveToGoogle();
            syncGoogle = false;
            Log.i("loadFromGoogle", "Synced");
            loadFromGoogle();
        } else {
            Log.i("loadFromGoogle", "No user");
        }
    }

    public static void saveReminders() {
        Log.i("saveReminders", "reminders.size() " + reminders.size());

        try {

            remindersDB.execSQL("CREATE TABLE IF NOT EXISTS reminders (id VARCHAR, name VARCHAR, tag VARCHAR, freq INT(4), notify INT(1), completed VARCHAR)");

            remindersDB.delete("reminders", null, null);

            for (reminderItem thisReminder : reminders) {

                int notifyInt = (thisReminder.notify) ? 1 : 0;
                Log.i("SavingDB", "thisReminder.name " + thisReminder.name);
                remindersDB.execSQL("INSERT INTO reminders (id, name, tag, freq, notify, completed) VALUES ('" + thisReminder.reminderID + "' , '" + thisReminder.name + "' , '" + thisReminder.tag + "' , '" + thisReminder.frequency + "' , '" + notifyInt + "' , '" + ObjectSerializer.serialize(thisReminder.completed) + "')");

            }

        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    public static void loadReminders() {

        reminders.clear();

        reminderHour = sharedPreferences.getInt("reminderHour", 10);
        reminderMinute = sharedPreferences.getInt("reminderMinute", 0);

        try {

            Cursor c = remindersDB.rawQuery("SELECT * FROM reminders", null);

            int idIndex = c.getColumnIndex("id");
            int nameIndex = c.getColumnIndex("name");
            int tagIndex = c.getColumnIndex("tag");
            int freqIndex = c.getColumnIndex("freq");
            int notifyIndex = c.getColumnIndex("notify");
            int completedIndex = c.getColumnIndex("completed");

            c.moveToFirst();

            do {
                ArrayList<String> completed;

                completed = (ArrayList<String>) ObjectSerializer.deserialize(c.getString(completedIndex));

                Boolean thisNotify = (c.getInt(notifyIndex) == 1) ? true : false;

                reminderItem newReminder = new reminderItem(c.getString(idIndex), c.getString(nameIndex), c.getString(tagIndex), c.getInt(freqIndex), thisNotify, "Loading");

                for (String thisCompleted : completed) {
                    newReminder.completed.add(thisCompleted);
                }
                reminders.add(newReminder);

            } while (c.moveToNext());

            c.close();
        } catch (Exception e) {

            Log.i("LoadingDB", "Caught Error");
            e.printStackTrace();

        }

    }

    public static void exportDB() {
        Log.i("exportDB", "Starting");
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source = null;
        FileChannel destination = null;

        File dir = new File(Environment.getExternalStorageDirectory() + "/RegularReminders/");
        try {
            if (dir.mkdir()) {
                System.out.println("Directory created");
            } else {
                System.out.println("Directory is not created");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Creating Dir Error", "" + e);
        }

        String currentDBPath = "/data/com.androidandyuk.regularreminders/databases/Reminders";
        String backupDBPath = "RegularReminders/Reminders.db";
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
//            Snackbar.make(findViewById(R.id.main), "DB Exported!", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        } catch (IOException e) {
            e.printStackTrace();
//            Snackbar.make(findViewById(R.id.main), "Exported Failed!", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        }
    }

    public static void importDB() {
        Log.i("ImportDB", "Started");
        try {
            String DB_PATH = "/data/data/com.androidandyuk.regularreminders/databases/Reminders";

            File sdcard = Environment.getExternalStorageDirectory();
            String yourDbFileNamePresentInSDCard = sdcard.getAbsolutePath() + File.separator + "RegularReminders/Reminders.db";

            Log.i("ImportDB", "SDCard File " + yourDbFileNamePresentInSDCard);

            File file = new File(yourDbFileNamePresentInSDCard);
            // Open your local db as the input stream
            InputStream myInput = new FileInputStream(file);

            // Path to created empty db
            String outFileName = DB_PATH;

            // Opened assets database structure
            OutputStream myOutput = new FileOutputStream(outFileName);

            // transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }

            // Close the streams
            myOutput.flush();
            myOutput.close();
            myInput.close();
        } catch (Exception e) {
            Log.i("ImportDB", "Exception Caught" + e);
        }
        loadReminders();
        Collections.sort(reminders);
        myAdapter.notifyDataSetChanged();
    }

    private void setNotificationTime() {
        TimePicker timePicker = (TimePicker) findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        // set current time into timepicker
        timePicker.setCurrentHour(reminderHour);
        timePicker.setCurrentMinute(reminderMinute);
        createdDialog(0).show();
    }

    protected Dialog createdDialog(int id) {
        switch (id) {
            case TIME_DIALOG_ID:
                return new TimePickerDialog(this, timePickerListener, reminderHour, reminderMinute, false);

        }
        return null;
    }

    private TimePickerDialog.OnTimeSetListener timePickerListener =
            new TimePickerDialog.OnTimeSetListener() {
                public void onTimeSet(TimePicker view, int hourOfDay,
                                      int selectedMinute) {
                    reminderHour = hourOfDay;
                    reminderMinute = selectedMinute;
                    TimePicker timePicker = (TimePicker) findViewById(R.id.timePicker);
                    timePicker.setVisibility(View.INVISIBLE);
                    Log.i("New Notification time ", reminderHour + " " + reminderMinute);
                    ed.putInt("reminderHour", reminderHour).apply();
                    ed.putInt("reminderMinute", reminderMinute).apply();
                    setNotiMenu();
                }
            };

    //   GOOGLE SIGN IN

    private void signIn() {
        Log.i("signIn", "Starting");

//        syncGoogle = true;

        final Context context = App.getContext();

        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Over write current info with Google data?")
                .setMessage("Select Sync if you want to keep what you have on the phone, or Google if you're happy to loose these reminders")
                .setPositiveButton("Sync", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Snackbar.make(findViewById(R.id.main), "Syncing Google Data", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                        syncGoogle = true;
                        Log.i("Setting syncGoogle", "" + syncGoogle);
                        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                        startActivityForResult(signInIntent, RC_SIGN_IN);
                    }
                })
                .setNegativeButton("Google", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Snackbar.make(findViewById(R.id.main), "Loading Google Data", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                        syncGoogle = false;
                        Log.i("Setting syncGoogle", "" + syncGoogle);
                        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                        startActivityForResult(signInIntent, RC_SIGN_IN);
                    }
                })
                .show();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("onActivityResult", "Starting");
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
                Log.i("SignedIn", "OnActivityResult");
            } else {
                // Google Sign In failed, update UI appropriately
                Log.i("onActivityResult", "Sign in failed");
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.i("GoogleSignIn", "signInWithCredential:success");
                            user = mAuth.getCurrentUser();
                            loadFromGoogle();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.i("GoogleSignIn", "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(R.id.main), "Authentication failed.", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                        }
                    }
                });
        Log.i("SignedIn", "firebaseAuthWithGoogle");
    }

    public void signOut() {
        mAuth.signOut();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        Log.i("signOut", "Complete");
        Snackbar.make(findViewById(R.id.main), "Signed Out", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        invalidateOptionsMenu();
    }

    //   GOOGLE SIGN IN END

    public void setToolbarUser() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView userName = headerView.findViewById(R.id.nav_user);
        try {
            userName.setText(user.getDisplayName());
        } catch (Exception e) {
            e.printStackTrace();
            userName.setText("Not logged in to Google");
        }

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_settings) {

            Intent intent = new Intent(getApplicationContext(), Settings.class);
            startActivity(intent);

        } else if (id == R.id.nav_set_time) {

            setNotificationTime();

        } else if (id == R.id.nav_logout) {

            if (user == null) {
                signIn();
            } else {
                signOut();
            }

        } else if (id == R.id.nav_backup) {

            exportDB();

        } else if (id == R.id.nav_restore) {

            importDB();

        } else if (id == R.id.nav_exit) {

            finish();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//
//        super.onCreateOptionsMenu(menu);
//
//        if (user == null) {
//            signInOut = "Sign In";
//        } else {
//            signInOut = "Sign Out";
//        }
//
//        String notifyText = "Reminder Time " + reminderHour;
//        if (reminderMinute < 10) {
//            notifyText += ":0" + reminderMinute;
//        } else {
//            notifyText += ":" + reminderMinute;
//        }
//
//        menu.add(0, 0, 0, signInOut).setShortcut('3', 'c');
//        menu.add(0, 1, 0, notifyText).setShortcut('3', 'c');
//        menu.add(0, 2, 0, "Export DB to SD").setShortcut('3', 'c');
//        menu.add(0, 3, 0, "Import DB from SD").setShortcut('3', 'c');
//
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int menu_choice = item.getItemId();
//        switch (menu_choice) {
//            case 0:
//                Log.i("Option", "0");
//
//                if (user == null) {
//                    signIn();
//                } else {
//                    signOut();
//                }
//
//                return true;
//            case 1:
//                Log.i("Option", "1");
//                setNotificationTime();
//                return true;
//            case 2:
//                Log.i("Option", "2");
//                exportDB();
//                return true;
//            case 3:
//                Log.i("Option", "3");
//                importDB();
//                return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    public static void loadSettings() {
        reminderDefault = sharedPreferences.getString("reminderDefault", "Recurring");
        daysDefault = sharedPreferences.getInt("daysDefault", 7);
    }

    public static void saveSettings() {
        ed.putString("reminderDefault", reminderDefault).apply();
        ed.putString("advancedWarning", advancedWarning).apply();
        ed.putInt("daysDefault", daysDefault).apply();
    }

    @Override
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (addReminder.isShown() || recurringTV.isShown() || singleTV.isShown()) {
                hideReminder();
                hideFABOptions();
            } else {
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
//        mAuth.addAuthStateListener(mAuthListener);
        user = mAuth.getCurrentUser();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
//        mAuth.removeAuthStateListener(mAuthListener);
        setRecurring(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
//        saveReminders();
//        saveToGoogle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        loadReminders();
//        loadFromGoogle();
        Collections.sort(reminders);
        myAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
//        saveToGoogle();
        mAuth.removeAuthStateListener(mAuthListener);
    }
}
