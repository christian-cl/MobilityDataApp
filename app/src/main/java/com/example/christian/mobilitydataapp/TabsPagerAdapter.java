package com.example.christian.mobilitydataapp;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by Christian on 8/05/15.
 *
 */
public class TabsPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragments;
    /**
     * @param fm
     * @param fragments
     */
    public TabsPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int index) {

        switch (index) {
            case 0:
                return new MapTabFragment();
            case 1:
                return new LogTabFragment();
            case 2:
                return new TrackFragment();
        }

        return null;
    }

    @Override
    public int getCount() {
        // get item count - equal to number of tabs
        return 3;
    }

}
