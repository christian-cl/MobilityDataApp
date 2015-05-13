/**
 * Christian Cintrano on 24/04/15
 *
 * Default Main Activity
 */

package com.example.christian.mobilitydataapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.christian.mobilitydataapp.persistence.DataCapture;
import com.example.christian.mobilitydataapp.persistence.DataCaptureDAO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private static final String BACKUP_DB_NAME = "Backup";
    private static final String PREFERENCES_FILE_NAME = "PREFERENCES";
    private static final String PREF_LAST_DATE_BACKUP = "backupLastDate";
    private static final String PREF_DELAY_TO_BACKUP = "pref_key_delay_backup_database";
    private static final String FORMAT_DATE = "yyyy-MM-dd HH:mm:ss";

    SimpleDateFormat sdf;

    private DataCaptureDAO db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setLogo(R.mipmap.ic_launcher_inv);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setLogo(R.mipmap.ic_launcher_inv);
        }

        db = new DataCaptureDAO(this);
        db.open();

        sdf = new SimpleDateFormat(FORMAT_DATE);


        Calendar startDate = null;
        Calendar endDate = null;
        if(checkBackupTime(startDate, endDate)) {
            Log.i("DB","Now is a valid date for save backup");
            String fileName = BACKUP_DB_NAME + "_" + startDate.toString() + "_" + endDate.toString();
            if(saveFileAndRemove(fileName,startDate ,endDate)) {
                Log.i("DB","Set a new date for save backup");
                setNewDelayToBackup();
            }
        } else {
            Log.i("DB", "Now do not need save backup");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        db.open();
       }

    @Override
    public void onPause() {
        super.onPause();
        db.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings: sendMessagePreferences();
        }
        return true;
    }

    /** Called when the user clicks the Send button */
    public void sendMessageMap(View view) {
        // Do something in response to button
        // Activity is a subclass from context
        Intent intent = new Intent(this, MapsActivity.class);
        // Init the activity
        startActivity(intent);
    }
    /** Called when the user clicks the Send button */
    public void sendMessageDebug(View view) {
        // Do something in response to button
        // Activity is a subclass from context
        Intent intent = new Intent(this, DebugActivity.class);
        // Init the activity
        startActivity(intent);
    }
    /** Called when the user clicks the Send button */
    public void sendMessageMapTab(View view) {
        // Do something in response to button
        // Activity is a subclass from context
        Intent intent = new Intent(this, MapTabActivity.class);
        // Init the activity
        startActivity(intent);
    }
    /** Called when the user clicks the Send button */
    public void sendMessagePreferences() {
        Intent intent = new Intent(this,SettingsActivity.class);
        startActivity(intent);
    }


    public boolean saveFileAndRemove(String fileName, Calendar dateStart, Calendar dateEnd) {
        if(saveFile(fileName, dateStart, dateEnd)) {
            Log.i("____","          DELETE");
//            db.delete(dateStart, dateEnd);
            return true;
        } else {
            return false;
        }
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean saveFile(String fileName, Calendar dateStart, Calendar dateEnd) {
        Toast.makeText(this, "Saving file...", Toast.LENGTH_SHORT).show();
        Log.i("DB", "Saving file...");
        FileOutputStream out = null;
        DataCaptureDAO db = new DataCaptureDAO(this);
        db.open();
        List<DataCapture> data = db.get(dateStart, dateEnd);
        Log.i("DB","Find " + data.size() + " elements");
        String extension = ".csv";
        String folderName = "/mdaFolder";
        try {
            if(isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + folderName);
                dir.mkdirs();
                File file = new File (dir, fileName + extension);
                out = new FileOutputStream(file);
            } else {
                out = openFileOutput(fileName + extension, Context.MODE_PRIVATE);
            }
            String head = "_id,latitude,longitude,street,stoptype,comment,date\n";
            out.write(head.getBytes());
            for(DataCapture dc : data) {
                out.write((String.valueOf(dc.getId()) + ",").getBytes());
                out.write((String.valueOf(dc.getLatitude()) + ",").getBytes());
                out.write((String.valueOf(dc.getLongitude()) + ",").getBytes());
                if(dc.getAddress() != null) {
                    out.write(("\"" + dc.getAddress() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                if(dc.getStopType() != null) {
                    out.write(("\"" + dc.getStopType() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                if(dc.getComment() != null) {
                    out.write(("\"" + dc.getComment() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                out.write(("\"" + dc.getDate() + "\"\n").getBytes());
            }
            out.flush();
            out.close();
            Log.i("DB", "File saved");
            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                db.close();
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkBackupTime(Calendar startDate, Calendar endDate) {
        Log.i("Preferences","Checking preferences");
//        SharedPreferences settings = getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        String lastDate = settings.getString(PREF_LAST_DATE_BACKUP,"");
        Log.i("___",lastDate);
        int delay = Integer.parseInt(settings.getString(PREF_DELAY_TO_BACKUP, "0")); // number of days

        Calendar cMinDate = Calendar.getInstance();
        try {
            cMinDate.setTime(sdf.parse(lastDate));
            cMinDate.add(Calendar.DATE, delay);  // number of days to add
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar now = Calendar.getInstance();

        System.out.println(now);
        System.out.println(cMinDate);
        return now.compareTo(cMinDate) >= 0;
    }

    private void setNewDelayToBackup() {
//        SharedPreferences settings = getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Calendar newDate = Calendar.getInstance();

        String newLastDate = sdf.format(newDate.getTime());

        // Update date of last backup
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_LAST_DATE_BACKUP,newLastDate);
//        editor.commit();
        editor.apply();
    }
}