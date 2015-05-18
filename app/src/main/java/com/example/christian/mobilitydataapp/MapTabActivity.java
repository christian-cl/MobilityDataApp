package com.example.christian.mobilitydataapp;

import android.app.IntentService;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.example.christian.mobilitydataapp.persistence.DataCapture;
import com.example.christian.mobilitydataapp.persistence.DataCaptureDAO;
import com.example.christian.mobilitydataapp.persistence.StreetTrack;
import com.example.christian.mobilitydataapp.persistence.StreetTrackDAO;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 * Maps Activity with tabs
 */
public class MapTabActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final static String DIALOG_SAVE_FILE_TITLE = "Guardar archivo";
    private final static String B_OK = "Aceptar";
    private final static String B_CANCEL = "Cancelar";

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

    private GoogleApiClient mGoogleApiClient;
    private boolean mAddressRequested;


    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    private Fragment mapFragment;
    private Fragment trackFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tab_map);
        mViewPager = (ViewPager) findViewById(R.id.fragment_container);

        buildGoogleApiClient();
//        fetchAddressButtonHandler(mViewPager);


        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(bar.newTab().setText("Mapa"), MapTabFragment.class, null);
//        mTabsAdapter.addTab(bar.newTab().setText("Información"), LogTabFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText("Datos"), TrackFragment.class, null);

        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }

        dateFormatter = new SimpleDateFormat("dd-MM-yyyy",Locale.US);


        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        loadSettings();
        PreferenceChangeListener preferenceListener = new PreferenceChangeListener();
        pref.registerOnSharedPreferenceChangeListener(preferenceListener);

        configureDialogWait();
        db = new DataCaptureDAO(this);
        db.open();
        mHandler = new Handler();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            if (locationManager != null) {
                // Register GPSStatus listener for events
                locationManager.addGpsStatusListener(mGPSStatusListener);
                gpsLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        myLocationChanged(location);
                        mResultReceiver = new AddressResultReceiver(mHandler);
                        startIntentService();
                    }
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        // TODO Auto-generated method stub
                    }
                    @Override
                    public void onProviderEnabled(String provider) {
                        Toast.makeText(MapTabActivity.this, "Enabled new provider " + provider,
                                Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onProviderDisabled(String provider) {
                        Toast.makeText(MapTabActivity.this, "Disabled provider " + provider,
                                Toast.LENGTH_SHORT).show();
                    }
                };

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalTimeGPS, minDistance, gpsLocationListener);
            }
        }
    }



    @Override
    public void onResume() {
        super.onResume();
//        setHiddenFragment();
        db.open();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalTimeGPS, minDistance, gpsLocationListener);
//        startRepeatingTask();
    }

    @Override
    public void onPause() {
        super.onPause();
//        stopRepeatingTask();
//        ((LogTabFragment) getHiddenFragment()).appendLog(DATA_END);
        locationManager.removeUpdates(gpsLocationListener);
        db.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeGpsStatusListener(mGPSStatusListener);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
    }


    /*****
     * GPS
     */

    private static final String GPS_LOADING = "Iniciando conexión GPS. Por favor, espere.";
    private static final String DATA_START = "Iniciando la recogida de los datos...";
    private static final String DATA_END = "Recogida de datos terminada";
    private static final String NEW_POSITION = "Guardando la siguiente posición: ";
    private static final String NEW_GPS = "Nueva posición GPS: ";

    private long intervalTimeGPS; // milliseconds
    private float minDistance; // meters

    private SimpleDateFormat sdf;

    public LocationManager locationManager;
    private ProgressDialog dialogWait;
    public DataCaptureDAO db;

    private SharedPreferences pref; // Settings listener

    public boolean runningCaptureData;
    private float trackDistance;
    private DataCapture currentTrackPoint;

    private DataCapture startTrackPoint;

    // Process to repeat
    private int intervalCapture;
    private Handler mHandler;
    private Location currentLocation;
    private LocationListener gpsLocationListener;
    public GpsStatus.Listener mGPSStatusListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.i("GPS", "Searching...");
                    Toast.makeText(MapTabActivity.this, "GPS_SEARCHING", Toast.LENGTH_SHORT).show();
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.i("GPS", "STOPPED");
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.i("GPS", "Locked position");
                /*
                 * GPS_EVENT_FIRST_FIX Event is called when GPS is locked
                 */
                    Toast.makeText(MapTabActivity.this, "GPS_LOCKED", Toast.LENGTH_SHORT).show();
                    dialogWait.dismiss();
                    Location gpslocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (gpslocation != null) {
                        String s = gpslocation.getLatitude() + ":" + gpslocation.getLongitude();
                        Log.i("GPS Info", s);
                    }
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    break;
            }
        }
    };


    private void loadSettings() {
        Log.i("MapActivity","Loading settings...");
        String syncConnPref = pref.getString("pref_key_interval_time", "0");
        int intervalTimeSetting = Integer.parseInt(syncConnPref);

        syncConnPref = pref.getString("pref_key_interval_capture", "0");
        int intervalCaptureSetting = Integer.parseInt(syncConnPref);

        syncConnPref = pref.getString("pref_key_min_distance", "0");
        int minDistanceSetting = Integer.parseInt(syncConnPref);

        intervalTimeGPS = intervalTimeSetting * 1000;
        intervalCapture = intervalCaptureSetting * 1000;
        minDistance = minDistanceSetting;
    }

    private void myLocationChanged(Location location) {
        Toast.makeText(this, "New Location", Toast.LENGTH_SHORT).show();

        currentLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
setHiddenFragment();
        ((MapTabFragment) mapFragment).setCamera(latLng);
        processTrackData(location); // Global process information
        ((MapTabFragment) mapFragment).addMarker(MapTabFragment.Marker_Type.POSITION, null, currentLocation);
    }



    private void configureDialogWait() {
        dialogWait = new ProgressDialog(this);
        dialogWait.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialogWait.setMessage(GPS_LOADING);
        dialogWait.setIndeterminate(true);
        dialogWait.setCanceledOnTouchOutside(false);
        dialogWait.show();
    }


    // Repeat process for catch information
    public Runnable mStatusChecker = new Runnable() {

        @Override
        public void run() {
            updateStatus(); //this function can change value of mInterval.
            mHandler.postDelayed(mStatusChecker, intervalCapture);
        }
    };

    public void startRepeatingTask() {
        mStatusChecker.run();
        runningCaptureData = true;
    }

    public void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
        runningCaptureData = false;
    }

    private void updateStatus() {
        if (currentLocation != null) {
            Log.i("Background", "Collecting data in: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());

            DataCapture dc = new DataCapture();
            dc.setLatitude(currentLocation.getLatitude());
            dc.setLongitude(currentLocation.getLongitude());
//            dc.setAddress(getStreet(currentLocation));
            dc.setDate(sdf.format(Calendar.getInstance().getTime()));

            DataCaptureDAO dbLocalInstance = new DataCaptureDAO(this);
            dbLocalInstance.open();
            dbLocalInstance.create(dc);
            dbLocalInstance.close();

            ((MapTabFragment) mapFragment).addMarker(MapTabFragment.Marker_Type.GPS, null, currentLocation);
        }
    }

    public void setHiddenFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        Log.i("Activity","Num of fragments: " + fragments.size());
        for(Fragment fragment : fragments){
            if(fragment != null) {
                if (fragment instanceof MapTabFragment)//!fragment.isVisible())
                    mapFragment = fragment;
                else if (fragment instanceof TrackFragment)//!fragment.isVisible())
                    trackFragment = fragment;
            }
        }
    }

    /**
     * Handle preferences changes
     */
    private class PreferenceChangeListener implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            Log.i("Settings", "Changed settings");
            loadSettings();
        }
    }

    public void processTrackData(Location location) {
//        ((TrackFragment) getHiddenFragment(Tab_Type.TrackFragment)).appendLog("Hollaaa");
        if(startTrackPoint == null) {
            startTrackPoint = new DataCapture();
            startTrackPoint.setLatitude(location.getLatitude());
            startTrackPoint.setLongitude(location.getLongitude());
//            startTrackPoint.setAddress(getStreet(location));
            startTrackPoint.setDate(sdf.format(Calendar.getInstance().getTime()));
            trackDistance = 0;
        } else {
            String street = null;//getStreet(location);
            if(street == null) {
                Log.e("Geocoder", "Address is null");
            } else {
                if(startTrackPoint.getAddress().equals(street)) {
                    Location start = new Location("");
                    start.setLatitude(startTrackPoint.getLatitude());
                    start.setLongitude(startTrackPoint.getLongitude());
                    trackDistance += start.distanceTo(location);

                    currentTrackPoint = new DataCapture();
                    startTrackPoint.setLatitude(location.getLatitude());
                    startTrackPoint.setLongitude(location.getLongitude());
//                    startTrackPoint.setAddress(getStreet(location));
                    startTrackPoint.setDate(sdf.format(Calendar.getInstance().getTime()));
                } else {
                    // Save track data
                    DataCaptureDAO dbLocalInstanceDC = new DataCaptureDAO(this);
                    dbLocalInstanceDC.open();
                    dbLocalInstanceDC.create(startTrackPoint);
                    dbLocalInstanceDC.create(currentTrackPoint);
                    dbLocalInstanceDC.close();

                    StreetTrack st = new StreetTrack(startTrackPoint.getAddress(),
                            startTrackPoint.getLatitude(), startTrackPoint.getLongitude(),
                            currentTrackPoint.getLatitude(), currentTrackPoint.getLongitude(),
                            startTrackPoint.getDate(), currentTrackPoint.getDate(),
                            trackDistance);


                    StreetTrackDAO dbLocalInstanceST = new StreetTrackDAO(this);
                    dbLocalInstanceST.open();
                    dbLocalInstanceST.create(st);
                    dbLocalInstanceST.close();
                    // Display Information
                    String line = "Dirección: " + st.getAddress() + "\n"
                            + "\t Distancia recorrida: " + st.getDistance() + " m.\n"
                            + "\t Tiempo transcurrido: " + st.getDistance() + " s.";

//                    ((TrackFragment) getHiddenFragment(Tab_Type.TrackFragment)).appendLog(line);


                    startTrackPoint = new DataCapture();
                    startTrackPoint.setLatitude(location.getLatitude());
                    startTrackPoint.setLongitude(location.getLongitude());
//                    startTrackPoint.setAddress(getStreet(location));
                    startTrackPoint.setDate(sdf.format(Calendar.getInstance().getTime()));
                    trackDistance = 0;
                }
            }
        }
    }


    /**
     * GEOCODER
     *
     */

    @Override
    public void onConnected(Bundle connectionHint) {
        // Gets the best and most recent location currently available,
        // which may be null in rare cases when a location is not available.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                return;
            }

            if (mAddressRequested) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                startIntentService();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    protected Location mLastLocation;
    private AddressResultReceiver mResultReceiver;

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, currentLocation);
        startService(intent);
    }

    public void fetchAddressButtonHandler(View view) {
        // Only start the service to fetch the address if GoogleApiClient is
        // connected.
        if (mGoogleApiClient.isConnected() && mLastLocation != null) {
            startIntentService();
        }
        // If GoogleApiClient isn't connected, process the user's request by
        // setting mAddressRequested to true. Later, when GoogleApiClient connects,
        // launch the service to fetch the address. As far as the user is
        // concerned, pressing the Fetch Address button
        // immediately kicks off the process of getting the address.
        mAddressRequested = true;
//        updateUIWidgets();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    class AddressResultReceiver extends ResultReceiver {
        private String mAddressOutput;

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            Toast.makeText(MapTabActivity.this, "Nueva Dirección", Toast.LENGTH_SHORT).show();
            Toast.makeText(MapTabActivity.this, mAddressOutput, Toast.LENGTH_SHORT).show();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
















}