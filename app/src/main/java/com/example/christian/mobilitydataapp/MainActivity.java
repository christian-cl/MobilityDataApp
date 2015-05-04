/**
 * Christian Cintrano on 24/04/15
 *
 * Default Main Activity
 */

package com.example.christian.mobilitydataapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends ActionBarActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

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
    public void sendMessagePreferences() {
        Intent intent = new Intent(this,SettingsActivity.class);
        startActivity(intent);
    }

 /*   private void displaySharedPreferences() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(MainActivity.this);

        String username = prefs.getString("username", "Default NickName");
        String passw = prefs.getString("password", "Default Password");
        boolean checkBox = prefs.getBoolean("checkBox", false);
        String listPrefs = prefs.getString("listpref", "Default list prefs");

        StringBuilder builder = new StringBuilder();
        builder.append("Username: " + username + "\n");
        builder.append("Password: " + passw + "\n");
        builder.append("Keep me logged in: " + String.valueOf(checkBox) + "\n");
        builder.append("List preference: " + listPrefs);

        textView.setText(builder.toString());
    }*/
}