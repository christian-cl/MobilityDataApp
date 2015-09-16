package com.example.christian.mobilitydataapp;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.example.christian.mobilitydataapp.fragments.MapTabFragment;
import com.example.christian.mobilitydataapp.fragments.TrackFragment;
import com.example.christian.mobilitydataapp.persistence.DataCapture;
import com.example.christian.mobilitydataapp.persistence.DataCaptureDAO;
import com.example.christian.mobilitydataapp.persistence.Itinerary;
import com.example.christian.mobilitydataapp.persistence.ItineraryDAO;
import com.example.christian.mobilitydataapp.persistence.Point;
import com.example.christian.mobilitydataapp.persistence.StreetTrack;
import com.example.christian.mobilitydataapp.persistence.StreetTrackDAO;
import com.example.christian.mobilitydataapp.services.FetchAddressIntentService;
import com.example.christian.mobilitydataapp.services.TabsAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 * Maps Activity with tabs
 */
public class MapTabActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final static int REQ_CODE_SPEECH_INPUT = 100;

    private final static String DIALOG_SAVE_FILE_TITLE = "Guardar archivo";
    private final static String B_OK = "Aceptar";
    private final static String B_CANCEL = "Cancelar";

    private static final int ZOOM = 20;

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
    private boolean isFirstLocation = true;
    private String addressPattern = "ZZZZZZZZZZ"; // cadena imposible

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tab_map);
        mViewPager = (ViewPager) findViewById(R.id.fragment_container);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        dateFormatter = new SimpleDateFormat("dd-MM-yyyy",Locale.US);
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);

        buildGoogleApiClient();
        mHandler = new Handler();
        addressHandler = new Handler();
        configureActionBar(savedInstanceState);
        configurePreference();
        configureDialogWait();
        configureLocation();
    }

    private void configureLocation() {
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
                        if(!runningCaptureData) {
                            startRepeatingTask();
                        }
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

    private void configurePreference() {
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        loadSettings();
        PreferenceChangeListener preferenceListener = new PreferenceChangeListener();
        pref.registerOnSharedPreferenceChangeListener(preferenceListener);
    }

    private void configureActionBar(Bundle savedInstanceState) {
        final ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

            mTabsAdapter = new TabsAdapter(this, mViewPager);
            mTabsAdapter.addTab(bar.newTab().setText("Mapa"), MapTabFragment.class, null);
            mTabsAdapter.addTab(bar.newTab().setText("Datos"), TrackFragment.class, null);

            if (savedInstanceState != null) {
                bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        locationManager.addGpsStatusListener(mGPSStatusListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalTimeGPS, minDistance, gpsLocationListener);
//        startRepeatingTask();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRepeatingTask();
        locationManager.removeUpdates(gpsLocationListener);
        locationManager.removeGpsStatusListener(mGPSStatusListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRepeatingTask();
        locationManager.removeUpdates(gpsLocationListener);
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

    public void displayItineraries(View view) {
        ItineraryDAO db = new ItineraryDAO(MapTabActivity.this);
        db.open();
        ArrayList<Itinerary> itineraryList = new ArrayList<>();
        itineraryList.addAll(db.getAll());
        db.close();

        List<String> list = new ArrayList<>();
        for(Itinerary i : itineraryList) {
            list.add(i.getName());
        }

        String title = "Elija un itinerario";
        CharSequence[] array = list.toArray(new CharSequence[list.size()]);
        Dialog dialog = onCreateDialogSingleChoice(title, array, itineraryList);
        dialog.show();
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
        FileOutputStream outST = null;
        DataCaptureDAO db = new DataCaptureDAO(this);
        StreetTrackDAO dbST = new StreetTrackDAO(this);
        db.open();
        dbST.open();
        List<DataCapture> data = db.get(newDateStart, newDateEnd);
        List<StreetTrack> dataST = dbST.getAll();
        Log.i("DB","Find " + data.size() + " DataCapture elements");
        Log.i("DB","Find " + dataST.size() + " StreetTrack elements");
        String extension = ".csv";
        String folderName = "/neoTrack";
        try {
            if(isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + folderName);
                dir.mkdirs();
                File file = new File (dir, fileName + extension);
                File fileST = new File (dir, "ST" + fileName + extension);
                out = new FileOutputStream(file);
                outST = new FileOutputStream(fileST);
            } else {
                out = openFileOutput(fileName + extension, Context.MODE_PRIVATE);
                outST = openFileOutput("ST" + fileName + extension, Context.MODE_PRIVATE);
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

            String headST = "_id,address,latitude start,longitude start," +
                    "latitude end,longitude end,datetime start,datetime end,distance\n";
            outST.write(headST.getBytes());
            for(StreetTrack st : dataST) {
                outST.write((String.valueOf(st.getId()) + ",").getBytes());
                outST.write(("\"" + st.getAddress() + "\",").getBytes());
                outST.write((String.valueOf(st.getStartLatitude()) + ",").getBytes());
                outST.write((String.valueOf(st.getStartLongitude()) + ",").getBytes());
                outST.write((String.valueOf(st.getEndLatitude()) + ",").getBytes());
                outST.write((String.valueOf(st.getEndLongitude()) + ",").getBytes());
                outST.write(("\"" + st.getStartDateTime() + "\",").getBytes());
                outST.write(("\"" + st.getEndDateTime() + "\",").getBytes());
                outST.write((String.valueOf(st.getDistance()) + "\n").getBytes());
            }
            outST.flush();
            outST.close();

            Log.i("DB", "File saved");
            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                db.close();
                dbST.close();
                if (out != null) {
                    out.close();
                }
                if(outST != null) {
                    outST.close();
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



    private void configureDialogWait() {
        dialogWait = new ProgressDialog(this);
        dialogWait.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialogWait.setMessage(GPS_LOADING);
        dialogWait.setIndeterminate(true);
        dialogWait.setCanceledOnTouchOutside(false);
        dialogWait.show();
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

    //GPS periodico
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
    public Handler mHandler;
    public Handler addressHandler;
    private Location currentLocation;
    private LocationListener gpsLocationListener;
    public GpsStatus.Listener mGPSStatusListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.i("GPS", "Searching...");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.i("GPS", "STOPPED");
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    // GPS_EVENT_FIRST_FIX Event is called when GPS is locked
                    Log.i("GPS", "Locked position");
                    gpsFirstFixed();
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
//                    Log.i("GPS", "GPS_EVENT_SATELLITE_STATUS");
//                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                    System.out.println(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
//                    if (location != null) {
//                        myLocationChanged(location);
//                        String s = location.getLatitude() + ":" + location.getLongitude();
//                        Log.i("GPS Info", s);
//                    }
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
        currentLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        setHiddenFragment();
        ((MapTabFragment) mapFragment).setCamera(latLng);
        processTrackData(location); // Global process information
        ((MapTabFragment) mapFragment).addMarker(MapTabFragment.Marker_Type.POSITION, null, currentLocation);
        if(isFirstLocation) {
            ((MapTabFragment) mapFragment).setZoom(ZOOM);
            isFirstLocation = false;
        }
    }

    private void gpsFirstFixed() {
        dialogWait.dismiss();
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

            AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
            receiver.setDataCapture(dc);
            receiver.setIsInserted(true);
            startIntentService(receiver);
//            db.create(dc);
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
//        Log.e("TRACK",startTrackPoint.toString());
        if((startTrackPoint != null) && (startTrackPoint.getAddress() != null)) {
            DataCapture dc = new DataCapture();
            dc.setLatitude(location.getLatitude());
            dc.setLongitude(location.getLongitude());
            dc.setDate(sdf.format(Calendar.getInstance().getTime()));

            AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
            receiver.setDataCapture(dc);
            receiver.setIsInserted(false);
            startIntentService(receiver);
        } else {
            startTrackPoint = new DataCapture();
            startTrackPoint.setLatitude(location.getLatitude());
            startTrackPoint.setLongitude(location.getLongitude());
//            startTrackPoint.setAddress(getStreet(location));
            startTrackPoint.setDate(sdf.format(Calendar.getInstance().getTime()));
            AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
            receiver.setDataCapture(startTrackPoint);
            receiver.setIsInserted(true);
            startIntentService(receiver);
            currentTrackPoint = startTrackPoint;
            Log.i("Track","Set start track point in " +startTrackPoint.getLatitude() + " " + startTrackPoint.getLongitude());

            trackDistance = 0;
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
//                AddressResultReceiver mResultReceiver = new AddressResultReceiver(mHandler);
//                startIntentService(mResultReceiver);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    protected Location mLastLocation;

    protected void startIntentService(AddressResultReceiver mResultReceiver) {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, currentLocation);
        startService(intent);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public class AddressResultReceiver extends ResultReceiver {
        private String mAddressOutput;
        private DataCapture dataCapture;
        private boolean isInserted;

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            mAddressOutput.replace("\n", "");
            dataCapture.setAddress(mAddressOutput);

            if(isInserted) {
                db = new DataCaptureDAO(MapTabActivity.this);
                db.open();
                db.create(dataCapture);
                db.close();
            } else {
                callbackTrack(dataCapture);
            }
        }

        public DataCapture getDataCapture() {
            return dataCapture;
        }

        public void setDataCapture(DataCapture dataCapture) {
            this.dataCapture = dataCapture;
        }

        public void setIsInserted(boolean isInserted) {
            this.isInserted = isInserted;
        }
    }

    private void callbackTrack(DataCapture dataCapture) {
        if(dataCapture.getAddress() == null) {
            Log.e("Geocoder", "Address is null");
            startTrackPoint = dataCapture;
//            startTrackPoint.setLatitude(location.getLatitude());
//            startTrackPoint.setLongitude(location.getLongitude());
////            startTrackPoint.setAddress(getStreet(location));
//            startTrackPoint.setDate(sdf.format(Calendar.getInstance().getTime()));
//            AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
//            receiver.setDataCapture(startTrackPoint);
//            receiver.setIsInserted(true);
//            startIntentService(receiver);
//            currentTrackPoint = startTrackPoint;
//            Log.i("Track","Set start track point in " +startTrackPoint.getLatitude() + " " + startTrackPoint.getLongitude());
            trackDistance = 0;
        } else {
//            if(startTrackPoint.getAddress().equals(dataCapture.getAddress())) {
            Log.i("--------", addressPattern + " " + dataCapture.getAddress());
            if(dataCapture.getAddress().contains(addressPattern)) {
                Location start = new Location("");
                start.setLatitude(currentTrackPoint.getLatitude());
                start.setLongitude(currentTrackPoint.getLongitude());
                Location end = new Location("");
                end.setLatitude(dataCapture.getLatitude());
                end.setLongitude(dataCapture.getLongitude());
                Log.i("-TT-------", trackDistance + " " + start.distanceTo(end));
                Log.i("-TT-------", start.getLatitude() + " " + start.getLongitude());
                Log.i("-TT-------", end.getLatitude() + " " + end.getLongitude());
                trackDistance += start.distanceTo(end);

                // set new current
                currentTrackPoint = dataCapture;
            } else {
                // Save track data
                AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
                receiver.setDataCapture(dataCapture);
                receiver.setIsInserted(true);
                startIntentService(receiver);

                StreetTrack st = new StreetTrack(startTrackPoint.getAddress(),
                        startTrackPoint.getLatitude(), startTrackPoint.getLongitude(),
                        currentTrackPoint.getLatitude(), currentTrackPoint.getLongitude(),
                        startTrackPoint.getDate(), currentTrackPoint.getDate(),
                        trackDistance);

                StreetTrackDAO dbLocalInstanceST = new StreetTrackDAO(this);
                dbLocalInstanceST.open();
                dbLocalInstanceST.create(st);
                dbLocalInstanceST.close();
                // Elapsed

                long time = 0;
                try {
                    Date dateStart = sdf.parse(st.getStartDateTime());
                    Date dateEnd = sdf.parse(st.getEndDateTime());
                    time = (dateEnd.getTime()-dateStart.getTime())/1000;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                // Display Information
                String line = "Dirección: " + st.getAddress() + "\n"
                        + "\t Distancia recorrida: " + st.getDistance()+ " m.\n"
                        + "\t Tiempo transcurrido: " + time + " s.\n"
                        + "\t Punto de entrada: " + st.getStartLatitude() + " "
                        + st.getStartLongitude() + "\n"
                        + "\t Punto de salida: " + st.getEndLatitude() + " "
                        + st.getEndLongitude() + "\n";

//                    ((TrackFragment) getHiddenFragment(Tab_Type.TrackFragment)).appendLog(line);
                ((TrackFragment) trackFragment).appendLog(line);

                startTrackPoint = dataCapture;
                int index = startTrackPoint.getAddress().indexOf(",");
                if(index == -1) {
                    index = startTrackPoint.getAddress().length();
                }
                addressPattern = startTrackPoint.getAddress().substring(0,index);
                Log.i("addressPattern",addressPattern);
                currentTrackPoint = dataCapture;
                trackDistance = 0;
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    public void saveData(DataCapture dc) {
        AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
        receiver.setDataCapture(dc);
        receiver.setIsInserted(true);
        startIntentService(receiver);
    }







    /**
     * Receiving speech input
     * */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //// Para debug
                    System.out.println(result);
                    String out = "";
                    for(String s : result) {
                        out += s + " ";
                    }
                    Log.i("Speak",out);
                    //// Para debug END
                    if(getStopType(result) == null) {
                        Log.e("Speak","Not match stop cause");
                    }
                    processStopChoice(getStopType(result),null);
                }
                break;
            }

        }
    }

    private String getStopType(ArrayList<String> result) {
        final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros"};
        final String[] stopChoicesPattern = {"asco", "bra", "ente", "tro"};

        for(String str : result) {
         System.out.println(str);
            str = Normalizer.normalize(str, Normalizer.Form.NFD);
            // remove accents
            str = str.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            for(int i = 0; i<stopChoicesPattern.length;i++) {
                if(str.toLowerCase().contains(stopChoicesPattern[i])) {
                    return stopChoices[i];
                }
            }
        }
        return null;
    }

    public void processStopChoice(String title, String text) {
        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        DataCapture dc = new DataCapture();
        dc.setLatitude(loc.getLatitude());
        dc.setLongitude(loc.getLongitude());
        dc.setStopType(title);
        dc.setComment(text);
        dc.setDate(sdf.format(Calendar.getInstance().getTime()));
        saveData(dc);

        ((MapTabFragment) mapFragment).addMarker(MapTabFragment.Marker_Type.STOP, title, loc);
    }

    public Dialog onCreateDialogSingleChoice(String title, CharSequence[] array, final List dataList) {

        int index = -1;
        //Initialize the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set the dialog title
        builder.setTitle(title)
        // Specify the list array, the items to be selected by default (null for none),
        // and the listener through which to receive callbacks when items are selected
                .setSingleChoiceItems(array, 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
//                        index = which;
                    }
                })
                // Set the action buttons
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the result somewhere
                        // or return them to the component that opened the dialog
                        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                        displayItineraySelected((Itinerary) dataList.get(selectedPosition));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        return builder.create();
    }

    private void displayItineraySelected(Itinerary itinerary) {
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        Log.i("-----", "\n\n\n\n\n\n\n\n\n\n---");
        for(Object point : itinerary.getPoints()) {
            Log.i("----", point.toString());
            Location location = new Location("Test");
            location.setLatitude(((Point) point).getLatitude());
            location.setLongitude(((Point) point).getLongitude());
            ((MapTabFragment) mapFragment).addMarker(MapTabFragment.Marker_Type.ITINERARY,
                    ((Point) point).getAddress(), location);
        }
    }
}