package com.example.christian.mobilitydataapp;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

import com.example.christian.mobilitydataapp.persistence.DataCapture;
import com.example.christian.mobilitydataapp.persistence.DataCaptureDAO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 * Maps Activity with tabs
 */
public class MapTabActivity extends ActionBarActivity implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener/*ActionBar.TabListener*/ {

    private final static String DIALOG_SAVE_FILE_TITLE = "Guardar archivo";
    private final static String B_OK = "Aceptar";
    private final static String B_CANCEL = "Cancelar";


    private TabHost mTabHost;
    private ViewPager mViewPager;
    private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, MapTabActivity.TabInfo>();
    private PagerAdapter mPagerAdapter;
    /**
     *
     * @author mwho
     * Maintains extrinsic info of a tab's construct
     */
    private class TabInfo {
        private String tag;
        private Class<?> clss;
        private Bundle args;
        private Fragment fragment;
        TabInfo(String tag, Class<?> clazz, Bundle args) {
            this.tag = tag;
            this.clss = clazz;
            this.args = args;
        }

    }
    /**
     * A simple factory that returns dummy views to the Tabhost
     * @author mwho
     */
    class TabFactory implements TabHost.TabContentFactory {

        private final Context mContext;

        /**
         * @param context
         */
        public TabFactory(Context context) {
            mContext = context;
        }

        /** (non-Javadoc)
         * @see android.widget.TabHost.TabContentFactory#createTabContent(java.lang.String)
         */
        public View createTabContent(String tag) {
            View v = new View(mContext);
            v.setMinimumWidth(0);
            v.setMinimumHeight(0);
            return v;
        }

    }

    private ViewPager viewPager;
    private android.support.v7.app.ActionBar actionBar;
    // Tab titles
    private String[] tabs = { "Mapa", "Registro", "Información"};

    private AlertDialog saveFileDialog;
    private DatePickerDialog dateInitDialog;

    private SimpleDateFormat dateFormatter;
    private EditText etDateStart;
    private EditText etDateEnd;
    private DatePickerDialog dateEndDialog;
    private EditText etNameSaveFile;
    private Calendar newDateStart;
    private Calendar newDateEnd;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_map);
        // Initilization
        viewPager = (ViewPager) findViewById(R.id.fragment_container);
        /**
         * on swiping the viewpager make respective tab selected
         * */
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // on changing the page
                // make respected tab selected
                actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });

        actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Adding Tabs
//        for (String tab_name : tabs) {
//            actionBar.addTab(actionBar.newTab().setText(tab_name).setTabListener(this));
//        }

        TabsPagerAdapter mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);

        dateFormatter = new SimpleDateFormat("dd-MM-yyyy");

        this.initialiseTabHost(savedInstanceState);
        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
        }
        // Intialise ViewPager
        this.intialiseViewPager();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_map, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                sendSettings();
                return true;
            case R.id.action_save_file:
                displaySaveFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void sendSettings() {
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void saveFile(String fileName) {
        Toast.makeText(this, "Saving file...", Toast.LENGTH_SHORT).show();
        Log.i("DB", "Saving file...");
        FileOutputStream out = null;
        DataCaptureDAO db = new DataCaptureDAO(this);
        db.open();
        List<DataCapture> data = db.get(newDateStart, newDateEnd);
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
        } catch (Exception e) {
            e.printStackTrace();
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

//    @Override
//    public void onTabSelected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction ft) {
//        Log.i(" A                 ",String.valueOf(tab.getPosition()));
//        viewPager.setCurrentItem(tab.getPosition());
////        TabInfo tabInfo = (TabInfo) tab.getTag();
////        for ( int i = 0; i < mTabs.size(); i++ ) {
////            if ( mTabs.get( i ) == tabInfo ) {
////                viewPager.setCurrentItem( i );
////            }
////        }
//
//    }
//
//    @Override
//    public void onTabUnselected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction ft) {
//        Log.i(" B                 ",String.valueOf(tab.getPosition()));
////        ft.detach(viewPager.);
////        if (mFragment != null) {
//            //ft.detach(mFragment); //requires API 13
////            ft.remove(mFragment); //this does not do the same thing as detach
////        }
//    }
//
//    @Override
//    public void onTabReselected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction ft) {
//        Log.i(" C                 ",String.valueOf(tab.getPosition()));
//
//    }


    public void displaySaveFile() {
        Calendar newCalendar = Calendar.getInstance();

        newDateStart = Calendar.getInstance();
        newDateStart.set(newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH),0,0,0);
        newDateEnd = Calendar.getInstance();
        newDateEnd.set(newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH),23,59,59);
        newDateEnd.set(Calendar.HOUR,23);
        dateInitDialog = new DatePickerDialog(this,new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.i("Dialog", "Change datepicker");
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                etDateStart.setText(dateFormatter.format(newDate.getTime()));
            }
        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        dateInitDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg2, int which) {
                dlg2.cancel();
                saveFileDialog.show();
            }
        });
        dateInitDialog.getDatePicker().init(newCalendar.get(Calendar.YEAR),
                newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                Log.i("Dialog", "Change datepicker");
                newDateStart = Calendar.getInstance();
                newDateStart.set(year, monthOfYear, dayOfMonth,0,0,0);
                etDateStart.setText(dateFormatter.format(newDateStart.getTime()));
            }
        });

        dateEndDialog = new DatePickerDialog(this,new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.i("Dialog", "Change datepicker");
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                etDateEnd.setText(dateFormatter.format(newDate.getTime()));
            }
        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        dateEndDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg2, int which) {
                dlg2.cancel();
                saveFileDialog.show();
            }
        });
        dateEndDialog.getDatePicker().init(newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                Log.i("Dialog", "Change datepicker");
                newDateEnd = Calendar.getInstance();
                newDateEnd.set(year, monthOfYear, dayOfMonth,23,59,59);
                newDateEnd.set(Calendar.HOUR,23);
                etDateEnd.setText(dateFormatter.format(newDateEnd.getTime()));
            }
        });

        AlertDialog.Builder saveFileDialogBuilder = new AlertDialog.Builder(this)
                .setCancelable(true)
                .setMessage(DIALOG_SAVE_FILE_TITLE)
                .setPositiveButton(B_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        dateInitDialog.show();
                        saveFile(etNameSaveFile.getText().toString());
                    }
                })
                .setNegativeButton(B_CANCEL, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setView(R.layout.dialog_save_file);
        saveFileDialog = saveFileDialogBuilder.create();

        saveFileDialog.show();

        etNameSaveFile = (EditText) saveFileDialog.findViewById(R.id.d_save_file_name);
        etDateStart = (EditText) saveFileDialog.findViewById(R.id.d_save_file_date_start);
        etDateEnd = (EditText) saveFileDialog.findViewById(R.id.d_save_file_date_end);
        Button bStart = (Button) saveFileDialog.findViewById(R.id.db_save_file_date_start);
        bStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateInitDialog.show();
            }
        });
        Button bEnd = (Button) saveFileDialog.findViewById(R.id.db_save_file_date_end);
        bEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateEndDialog.show();
            }
        });
        etDateStart.setText(dateFormatter.format(newCalendar.getTime()));
        etDateEnd.setText(dateFormatter.format(newCalendar.getTime()));
        etNameSaveFile.setText(getNameSaveFile());
    }

    private String getNameSaveFile() {
        return "info_track" + "_" + etDateStart.getText() + "_" + etDateEnd.getText();
    }






//    public TabListener(Activity activity, String tag, Class<T> clz, Bundle args) {
//        mActivity = activity;
//        mTag = tag;
//        mClass = clz;
//        mArgs = args;
//
//        // Check to see if we already have a fragment for this tab, probably
//        // from a previously saved state.  If so, deactivate it, because our
//        // initial state is that a tab isn't shown.
//        mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
//        if (mFragment != null) { // && !mFragment.isDetached()) {
//            FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
//            //ft.detach(mFragment);
//            ft.remove(mFragment);
//            ft.commit();
//        }
//    }
/*
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        //if (mFragment == null) {
        mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
        ft.add(android.R.id.content, mFragment, mTag);
        //} else {
        //    ft.attach(mFragment);
        //}
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        if (mFragment != null) {
            //ft.detach(mFragment); //requires API 13
            ft.remove(mFragment); //this does not do the same thing as detach
        }
    }*/


    /** (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
     */
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the tab selected
        super.onSaveInstanceState(outState);
    }

    /**
     * Initialise ViewPager
     */
    private void intialiseViewPager() {

        List<Fragment> fragments = new Vector<Fragment>();
        fragments.add(Fragment.instantiate(this, MapTabFragment.class.getName()));
        fragments.add(Fragment.instantiate(this, LogTabFragment.class.getName()));
        fragments.add(Fragment.instantiate(this, TrackFragment.class.getName()));
        this.mPagerAdapter  = new TabsPagerAdapter(super.getSupportFragmentManager(), fragments);
        //
        this.mViewPager = (ViewPager)super.findViewById(R.id.fragment_container);
        this.mViewPager.setAdapter(this.mPagerAdapter);
        this.mViewPager.setOnPageChangeListener(this);
    }

    /**
     * Initialise the Tab Host
     */
    private void initialiseTabHost(Bundle args) {
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        TabInfo tabInfo = null;
        MapTabActivity.AddTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab1").setIndicator("Tab 1"), ( tabInfo = new TabInfo("Tab1", MapTabFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        MapTabActivity.AddTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab2").setIndicator("Tab 2"), ( tabInfo = new TabInfo("Tab2", LogTabFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        MapTabActivity.AddTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab3").setIndicator("Tab 3"), ( tabInfo = new TabInfo("Tab3", TrackFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        // Default to first tab
        //this.onTabChanged("Tab1");
        //
        mTabHost.setOnTabChangedListener(this);
    }

    /**
     * Add Tab content to the Tabhost
     * @param activity
     * @param tabHost
     * @param tabSpec
     */
    private static void AddTab(MapTabActivity activity, TabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo) {
        // Attach a Tab view factory to the spec
        tabSpec.setContent(activity.new TabFactory(activity));
        tabHost.addTab(tabSpec);
    }

    /** (non-Javadoc)
     * @see android.widget.TabHost.OnTabChangeListener#onTabChanged(java.lang.String)
     */
    public void onTabChanged(String tag) {
        //TabInfo newTab = this.mapTabInfo.get(tag);
        int pos = this.mTabHost.getCurrentTab();
        this.mViewPager.setCurrentItem(pos);
    }

    /* (non-Javadoc)
     * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled(int, float, int)
     */
    @Override
    public void onPageScrolled(int position, float positionOffset,
                               int positionOffsetPixels) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected(int)
     */
    @Override
    public void onPageSelected(int position) {
        // TODO Auto-generated method stub
        this.mTabHost.setCurrentTab(position);
    }

    /* (non-Javadoc)
     * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrollStateChanged(int)
     */
    @Override
    public void onPageScrollStateChanged(int state) {
        // TODO Auto-generated method stub

    }
}