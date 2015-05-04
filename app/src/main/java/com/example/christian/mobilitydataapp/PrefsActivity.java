package com.example.christian.mobilitydataapp;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by christian on 29/04/15.
 */
public class PrefsActivity extends PreferenceActivity {

    private GlobalClass globalVariable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_preferences);

        globalVariable = (GlobalClass) getApplicationContext();
    }
}
