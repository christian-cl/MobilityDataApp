package com.example.christian.mobilitydataapp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.christian.mobilitydataapp.persistence.DataCapture;
import com.example.christian.mobilitydataapp.persistence.DataCaptureDAO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 * Maps Activity with tabs
 */
public class MapTabActivity extends ActionBarActivity implements ActionBar.TabListener/*, MapTabFragment.OnHeadlineSelectedListener*/{

    private final static String DIALOG_SAVE_FILE_TITLE = "Guardar archivo";
    private final static String B_OK = "Aceptar";
    private final static String B_CANCEL = "Cancelar";

    private ViewPager viewPager;
    private android.support.v7.app.ActionBar actionBar;
    // Tab titles
    private String[] tabs = { "Map", "Log"};

    private AlertDialog.Builder saveFileDialog;
//    private DatePickerDialog fromDatePickerDialog;
    private DatePickerDialog toDatePickerDialog;

    private SimpleDateFormat dateFormatter;
    private EditText fromDateEtxt;
    private EditText toDateEtxt;

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
        TabsPagerAdapter mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Adding Tabs
        for (String tab_name : tabs) {
            actionBar.addTab(actionBar.newTab().setText(tab_name)
                    .setTabListener(this));
        }

        dateFormatter = new SimpleDateFormat("dd-MM-yyyy",Locale.US);
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
        List<DataCapture> data = db.getAll();
//        String fileName = "datos.txt";
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
            String head = "_id,latitude,longitude,street,stoptype,comment,date";
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



    @Override
    public void onTabSelected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction ft) {
        // on tab selected
        // show respected fragment view
        viewPager.setCurrentItem(tab.getPosition());

    }

    @Override
    public void onTabUnselected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction ft) {

    }


    public void displaySaveFile() {
        final DatePickerDialog subDialog = new DatePickerDialog(this,new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                System.out.println("--------------");
                System.out.println("--------------");
                System.out.println("--------------");
//                fromDateEtxt.append("TEXTTOOO");
            }
        },2015,10,5);
        subDialog.setMessage("New Dialog Opened");
//                .setCancelable(true)
        subDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg2, int which) {
                System.out.println("OUTTTTT");
//                        fromDateEtxt.append("OUTTTTT");

                dlg2.cancel();
                saveFileDialog.show();
            }
        });

//        final EditText editText = new EditText(saveFileDialog.getContext());
        saveFileDialog = new AlertDialog.Builder(this)
                .setCancelable(true)
                .setMessage("First Dialog Opened")
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        subDialog.show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
        .setView(R.layout.dialog_save_file);
//        fromDateEtxt = new EditText(getApplication());
//        saveFileDialog.setView(fromDateEtxt);
//        saveFileDialog.setView(editText);
        saveFileDialog.show();
        /*
        fromDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                fromDateEtxt.setText(dateFormatter.format(newDate.getTime()));
            }

        }, 2015, 5, 11);
//                .setMessage("New Dialog Opened")
        fromDatePickerDialog.setCancelable(true);
        fromDatePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE,"Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        saveFileDialog.show();
                    }
                });


        saveFileDialog = new AlertDialog.Builder(this);
        saveFileDialog.setCancelable(true);

        // EditText by default hidden
        final EditText editText = new EditText(this);
//        editText.setEnabled(false);
        editText.append("TEXTO POR DEFECTO");

        saveFileDialog.setTitle(DIALOG_SAVE_FILE_TITLE);

        saveFileDialog.setView(editText).setMessage("Nombre del archivo");
        fromDateEtxt = new EditText(this);
        fromDateEtxt.setInputType(InputType.TYPE_NULL);
        fromDateEtxt.requestFocus();
        saveFileDialog.setView(fromDateEtxt);

        saveFileDialog.setNeutralButton("Edit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fromDatePickerDialog.show();
            }
        });
        saveFileDialog.setPositiveButton(B_OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveFile(editText.getText().toString());
            }
        });
        saveFileDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        saveFileDialog.show();
        */
    }
        /*

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // EditText by default hidden
        final EditText editText = new EditText(this);
//        editText.setEnabled(false);
        editText.append("TEXTO POR DEFECTO");

        builder.setTitle(DIALOG_SAVE_FILE_TITLE);

        builder.setView(editText).setMessage("Nombre del archivo");
        fromDateEtxt = new EditText(this);
        fromDateEtxt.setInputType(InputType.TYPE_NULL);
        fromDateEtxt.requestFocus();
        builder.setView(fromDateEtxt);

        builder.setNeutralButton("Edit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setDateTimeField(builder.getContext());
                fromDatePickerDialog.show();
            }
        });
        builder.setPositiveButton(B_OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveFile(editText.getText().toString());
            }
        });
        builder.setNegativeButton(B_CANCEL, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void setDateTimeField(Context context) {
//        fromDateEtxt.setOnClickListener(this);
//        toDateEtxt.setOnClickListener(this);

        Calendar newCalendar = Calendar.getInstance();
        fromDatePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                fromDateEtxt.setText(dateFormatter.format(newDate.getTime()));
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        fromDatePickerDialog.setButton(DatePickerDialog.BUTTON_NEUTRAL,"DONE",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
//        toDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
//
//            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
//                Calendar newDate = Calendar.getInstance();
//                newDate.set(year, monthOfYear, dayOfMonth);
//                toDateEtxt.setText(dateFormatter.format(newDate.getTime()));
//            }
//
//        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
    }
*/
}