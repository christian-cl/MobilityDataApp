package com.example.christian.neotrack;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.example.christian.neotrack.fragments.SettingsFragment;

/**
 * Created by Christian Cintrano on 29/04/15.
 *
 * Settings Activity
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
