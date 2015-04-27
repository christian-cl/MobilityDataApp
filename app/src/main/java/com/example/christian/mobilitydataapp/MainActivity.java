/**
 * Christian Cintrano on 24/04/15
 *
 * Default Main Activity
 */

package com.example.christian.mobilitydataapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

public class MainActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
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

}