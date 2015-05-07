package com.example.christian.mobilitydataapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.example.christian.mobilitydataapp.persistence.DataCaptureDAO;

import java.util.List;

/**
 * Created by Christian Cintrano on 29/04/15.
 *
 * Settings Activity
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        SettingsFragment customFragment = (SettingsFragment) getFragmentManager().findFragmentById(R.xml.preferences);
//        EditTextPreference textPreference = (EditTextPreference) customFragment.findPreference("pref_key_remove_database");
//
//        textPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            public boolean onPreferenceClick(Preference preference) {
//                System.out.println("0000000000000000000000000000000");
//                if(preference.equals("pref_key_remove_database")) {
//                    System.out.println("1111111111111111111111111111");
//                    displayConfirmationDialog();
//                    return true;
//                }
//                return false;
//            }
//        });
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

}
