package com.example.christian.mobilitydataapp;

import android.app.Application;

/**
 * Created by Christian Cintrano on 4/05/15.
 *
 * Class for global variables
 */
public class GlobalClass extends Application {

    private int milliSeconds = 1;

    public int getMilliSeconds() {
        return milliSeconds;
    }

    public void setMilliSeconds(int milliSeconds) {
        this.milliSeconds = milliSeconds;
    }
}
