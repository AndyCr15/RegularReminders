package com.androidandyuk.regularreminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static com.androidandyuk.regularreminders.reminderItem.daysDifference;

public class MainActivity extends AppCompatActivity {

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor ed;
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy");

    public static FirebaseAuth mAuth;
    public static FirebaseAuth.AuthStateListener mAuthListener;
    public static FirebaseDatabase database;
    public static FirebaseUser user;
    String signInOut = "Sign In";

    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient mGoogleApiClient;

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

    public static int reminderHour = 20;

    EditText reminderName;
    EditText reminderTag;
    EditText reminderFrequency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("onCreate", "Starting");

        sharedPreferences = this.getSharedPreferences("regularreminders", Context.MODE_PRIVATE);
        ed = sharedPreferences.edit();

        // until I implement landscape view, lock the orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        remindersDB = this.openOrCreateDatabase("Reminders", MODE_PRIVATE, null);

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
                saveToGoogle();
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

//        Log.i("Saving sP", "Beginning");
//        String name = "rrSharedPref";
//        int mode = MODE_PRIVATE;
//        File path = new File(Environment.getExternalStorageDirectory().toString());
//        File file = new File(path, "MySharedPreferences.xml");
//        Log.i("Saving sP", "F:" + file);
//        saveSharedPreferences(name, mode, file);

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
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(MainActivity.this, "Signed in as " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                    Log.i("SignedIn", "onAuthStateChanged");
                    loadFromGoogle();
                    myAdapter.notifyDataSetChanged();
                } else {
                    // User is signed out
                    Log.d("Login", "onAuthStateChanged:signed_out");
                }
            }
        };

        mAuth = FirebaseAuth.getInstance();

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

    public void showAddReminder(View view) {
        final View addReminder = findViewById(R.id.addReminder);
        addReminder.setVisibility(View.VISIBLE);

        Log.i("showAddReminder", "Starting");

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

                        reminderName.setText("");
                        reminderTag.setText("");
                        reminderFrequency.setText("");


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

    public void setNotifications() {
        Log.i("setNotifications", "Starting");
        if (reminders.size() >= 0) {
            Calendar calendar = Calendar.getInstance();

            Date nextDue = reminderItem.nextDue(reminders.get(0));
            int dif = daysDifference(new Date(), nextDue);
            if (dif < 0) {
                // if it's overdue, set to alarm today
                dif = 0;
            }

            if (dif == 0 && (calendar.get(Calendar.HOUR_OF_DAY) >= reminderHour)) {
                // it's due today, but it's passed alarm time
                dif = 1;
            }

            calendar.set(Calendar.HOUR_OF_DAY, reminderHour);
            calendar.add(Calendar.DAY_OF_YEAR, dif);

//            calendar.add(Calendar.SECOND, 10);

            Log.i("DaysUntilNextReminder", "" + dif);

            Intent intent = new Intent(getApplicationContext(), NotificationReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    public void loadPressed(View view) {
        loadFromGoogle();
    }

    public void savePressed(View view) {
        saveToGoogle();
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

    public static void saveToGoogle() {
        Log.i("saveToGoogle", "Starting");
        if (user != null) {
            Log.i("saveToGoogle", "user is not null");
            database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference();

            // clear anything previously saved for this user
            myRef.child(user.getUid()).removeValue();

            // save back the reminders for this user
            for (int i = 0; i < reminders.size(); i++) {
                myRef.child(user.getUid()).child("Item" + i).child("Name").setValue(reminders.get(i).name);
                myRef.child(user.getUid()).child("Item" + i).child("Tag").setValue(reminders.get(i).tag);
                myRef.child(user.getUid()).child("Item" + i).child("Freq").setValue(Integer.toString(reminders.get(i).frequency));
                try {
                    myRef.child(user.getUid()).child("Item" + i).child("Completed").setValue(ObjectSerializer.serialize(reminders.get(i).completed));
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("saveToGoogle", "Error saving completed " + i);
                }
            }
        }
    }

    public static void saveCompletedToGoogle() {
        Log.i("saveCompletedToGoogle", "Active Reminder " + activeReminderPosition);
        if (user != null) {
            Log.i("saveCompletedToGoogle", "user is not null");
            database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference();
            if (activeReminderPosition > -1) {
                Log.i("saveCompletedToGoogle", "activeReminderPosition is " + activeReminderPosition);
                // clear anything previously saved for this item completed
                myRef.child(user.getUid()).child("Item" + activeReminderPosition).child("Completed").removeValue();

                // save back the completed for this user
                try {
                    myRef.child(user.getUid()).child("Item" + activeReminderPosition).child("Completed").setValue(ObjectSerializer.serialize(reminders.get(activeReminderPosition).completed));
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("saveToGoogle", "Error saving completed");
                }
            }
        }
    }

    public static void loadFromGoogle() {
        if (user != null) {

            Log.i("loadFromGoogle", "Starting");

            database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference().child(user.getUid());

            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    Log.i("loadFromGoogle", "onDataChange");
                    reminders.clear();

                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        GenericTypeIndicator<Map<String, String>> genericTypeIndicator = new GenericTypeIndicator<Map<String, String>>() {
                        };
                        Map<String, String> map = ds.getValue(genericTypeIndicator);

                        String name = map.get("Name");
                        String tag = map.get("Tag");
                        int freq = Integer.parseInt(map.get("Freq"));
                        String completed = map.get("Completed");

                        ArrayList<String> completedArray = new ArrayList<>();
                        try {
                            completedArray = (ArrayList<String>) ObjectSerializer.deserialize(completed);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        reminderItem newReminder = new reminderItem(name, tag, freq, "Loading");

                        for (String thisCompleted : completedArray) {
                            newReminder.completed.add(thisCompleted);
                        }

                        reminders.add(newReminder);

                    }
                    Collections.sort(reminders);
                    myAdapter.notifyDataSetChanged();
                    saveReminders();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }


    public static void saveReminders() {
        Log.i("saveReminders", "reminders.size() " + reminders.size());
        try {

            remindersDB.execSQL("CREATE TABLE IF NOT EXISTS reminders (name VARCHAR, tag VARCHAR, freq INT(4), completed VARCHAR)");
            remindersDB.delete("reminders", null, null);

            for (reminderItem thisReminder : reminders) {

                remindersDB.execSQL("INSERT INTO reminders (name, tag, freq, completed) VALUES ('" + thisReminder.name + "' , '" + thisReminder.tag + "' , '" + thisReminder.frequency + "' , '" + ObjectSerializer.serialize(thisReminder.completed) + "')");

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public static void loadReminders() {

        reminders.clear();
        Log.i("LoadingDB", "reminder.size() " + reminders.size());

        try {

            Cursor c = remindersDB.rawQuery("SELECT * FROM reminders", null);

            int nameIndex = c.getColumnIndex("name");
            int tagIndex = c.getColumnIndex("tag");
            int freqIndex = c.getColumnIndex("freq");
            int completedIndex = c.getColumnIndex("completed");

            c.moveToFirst();

            do {
                Log.i("Cursor: nameIndex", "" + nameIndex);
                ArrayList<String> completed;

                completed = (ArrayList<String>) ObjectSerializer.deserialize(c.getString(completedIndex));

                reminderItem newReminder = new reminderItem(c.getString(nameIndex), c.getString(tagIndex), c.getInt(freqIndex), "Loading");

                for (String thisCompleted : completed) {
                    newReminder.completed.add(thisCompleted);
                }
                reminders.add(newReminder);

            } while (c.moveToNext());


        } catch (Exception e) {

            Log.i("LoadingDB", "Caught Error");
            e.printStackTrace();

        }

    }

    private void exportDB() {
        Log.i("exportDB", "Starting");
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source = null;
        FileChannel destination = null;
        String currentDBPath = "/data/" + "com.androidandyuk.regularreminders" + "/databases/Reminders";
        String backupDBPath = "Reminders.db";
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Toast.makeText(this, "DB Exported!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Exported Failed!", Toast.LENGTH_LONG).show();
        }
    }

    //   GOOGLE SIGN IN

    private void signIn() {
        Log.i("signIn", "Starting");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
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
//                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.i("GoogleSignIn", "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
        Log.i("SignedIn", "firebaseAuthWithGoogle");
    }

    public void signOut() {
        saveToGoogle();
        mAuth.signOut();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        Log.i("Signed Out", "Complete");
        Toast.makeText(MainActivity.this, "Signed Out", Toast.LENGTH_SHORT).show();

    }

    //   GOOGLE SIGN IN END

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        super.onCreateOptionsMenu(menu);

        if (user == null) {
            signInOut = "Sign In";
        } else {
            signInOut = "Sign Out";
        }

        menu.add(0, 0, 0, signInOut).setShortcut('3', 'c');
        menu.add(0, 1, 0, "Export DB to SD").setShortcut('3', 'c');

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menu_choice = item.getItemId();
        switch (menu_choice) {
            case 0:
                Log.i("Option", "0");

                if (user == null) {
                    signIn();
                } else {
                    signOut();
                }

                return true;
            case 1:
                Log.i("Option", "1");
                exportDB();
                return true;
        }

        return super.onOptionsItemSelected(item);
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
    protected void onStart() {
        super.onStart();
        Log.i("onStart", "Starting");
        mAuth.addAuthStateListener(mAuthListener);
        user = mAuth.getCurrentUser();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("onStop", "Starting");
        mAuth.removeAuthStateListener(mAuthListener);
        setNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MainActivity", "onPause");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("onDestroy", "Starting");
        saveToGoogle();

    }
}
