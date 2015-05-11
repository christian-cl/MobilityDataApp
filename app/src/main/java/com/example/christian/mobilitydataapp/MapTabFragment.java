package com.example.christian.mobilitydataapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.christian.mobilitydataapp.persistence.DataCapture;
import com.example.christian.mobilitydataapp.persistence.DataCaptureDAO;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 * Fragment by main tab: Map view
 */
public class MapTabFragment extends Fragment implements View.OnClickListener {

    private static final String GPS_LOADING = "Iniciando conexión GPS. Por favor, espere.";
    private static final String DATA_START = "Iniciando la recogida de los datos...";
    private static final String DATA_END = "Recogida de datos terminada";
    private static final String NEW_POSITION = "Guardando la siguiente posición: ";
    private static final String NEW_GPS = "Nueva posición GPS: ";

    private static final int ZOOM = 20;
    private static final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros"};
    private Context context;

    private static enum Marker_Type {GPS, STOP, POSITION}

    private long intervalTimeGPS; // milliseconds
    private float minDistance; // meters

    String title = null;

    private GoogleMap map; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private ProgressDialog dialogWait;
    private DataCaptureDAO db;

    private SharedPreferences pref; // Settings listener

    // Process to repeat
    private int intervalCapture;
    private Handler mHandler;
    private Location currentLocation;
    private Marker currentMarker;
    private LocationListener gpsLocationListener;
    public GpsStatus.Listener mGPSStatusListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.i("GPS", "Searching...");
                    Toast.makeText(context, "GPS_SEARCHING", Toast.LENGTH_SHORT).show();
                    System.out.println("TAG - GPS searching: ");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.i("GPS", "STOPPED");
                    System.out.println("TAG - GPS Stopped");
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.i("GPS", "Locked position");
                /*
                 * GPS_EVENT_FIRST_FIX Event is called when GPS is locked
                 */
                    Toast.makeText(context, "GPS_LOCKED", Toast.LENGTH_SHORT).show();
                    dialogWait.dismiss();
                    Location gpslocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (gpslocation != null) {
                        String s = gpslocation.getLatitude() + ":" + gpslocation.getLongitude();
                        Log.i("GPS Info", s);
                    }
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    //                 System.out.println("TAG - GPS_EVENT_SATELLITE_STATUS");
                    break;
            }
        }
    };


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.activity_maps, container, false);

        ImageButton bStop = (ImageButton) view.findViewById(R.id.stop_button);
        Button bStart = (Button) view.findViewById(R.id.start_button);
        Button bEnd = (Button) view.findViewById(R.id.end_button);
        bStop.setOnClickListener(this);
        bStart.setOnClickListener(this);
        bEnd.setOnClickListener(this);

        context = container.getContext();

        pref = PreferenceManager.getDefaultSharedPreferences(context);
        loadSettings();
        PreferenceChangeListener preferenceListener = new PreferenceChangeListener();
        pref.registerOnSharedPreferenceChangeListener(preferenceListener);

        configureDialogWait();
        db = new DataCaptureDAO(context);
        db.open();
        mHandler = new Handler();

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            if (locationManager != null) {
                // Register GPSStatus listener for events
                locationManager.addGpsStatusListener(mGPSStatusListener);
                gpsLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        myLocationChanged(location);
                    }
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        // TODO Auto-generated method stub
                    }
                    @Override
                    public void onProviderEnabled(String provider) {
                        Toast.makeText(context, "Enabled new provider " + provider,
                                Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onProviderDisabled(String provider) {
                        Toast.makeText(context, "Disabled provider " + provider,
                                Toast.LENGTH_SHORT).show();
                    }
                };

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalTimeGPS, minDistance, gpsLocationListener);
            }
        }

        map = ((MapFragment) ((Activity) context).getFragmentManager().findFragmentById(R.id.map)).getMap();

        return view;
    }


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
        Toast.makeText(context, "New Location", Toast.LENGTH_SHORT).show();
        int lat = (int) (location.getLatitude());
        int lng = (int) (location.getLongitude());
        System.out.println("LAT " + String.valueOf(lat));
        System.out.println("LON " + String.valueOf(lng));

        currentLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, ZOOM);
        map.animateCamera(cameraUpdate);

        addMarker(Marker_Type.POSITION, null, currentLocation);
    }

    @Override
    public void onResume() {
        super.onResume();
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

    private void configureDialogWait() {
        dialogWait = new ProgressDialog(context);
        dialogWait.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialogWait.setMessage(GPS_LOADING);
        dialogWait.setIndeterminate(true);
        dialogWait.setCanceledOnTouchOutside(false);
        dialogWait.show();
    }


    // Repeat process for catch information
    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            updateStatus(); //this function can change value of mInterval.
            mHandler.postDelayed(mStatusChecker, intervalCapture);
        }
    };

    private void startRepeatingTask() {
        mStatusChecker.run();
    }

    private void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    private void updateStatus() {
        if (currentLocation != null) {
            Log.i("Background", "Collecting data in: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());

            DataCapture dc = new DataCapture();
            dc.setLatitude(currentLocation.getLatitude());
            dc.setLongitude(currentLocation.getLongitude());

            DataCaptureDAO dbLocalInstance = new DataCaptureDAO(context);
            dbLocalInstance.open();
            dbLocalInstance.create(dc);
            dbLocalInstance.close();

            addMarker(Marker_Type.GPS, null, currentLocation);
        }
    }

    private String getStreet(Location localization) {
        if(localization != null) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = geocoder.getFromLocation(localization.getLatitude(), localization.getLongitude(), 1);
                System.out.println("Address");
                System.out.println(addresses);
                // Only considered the first result
                return addresses.get(0).getAddressLine(0);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public void displayStopChoices() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // EditText by default hidden
        final EditText editText = new EditText(context);
        editText.setEnabled(false);

        builder.setTitle("Seleccione una opción");
        builder.setSingleChoiceItems(stopChoices, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (stopChoices[item].equals("Otros")) {
                    editText.setEnabled(true);
                    editText.requestFocus();
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    editText.setEnabled(false);
                    editText.getText().clear();
                }
                title = stopChoices[item];
                String text = "Haz elegido la opcion: " + stopChoices[item];
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        builder.setView(editText);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(title != null) {
                    Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    String street = null;//getStreet(loc);
                    String text = null;
                    if(title.equals("Otros")) {
                        text = editText.getText().toString();
                    }

                    DataCapture dc = new DataCapture();
                    dc.setLatitude(loc.getLatitude());
                    dc.setLongitude(loc.getLongitude());
                    dc.setAddress(street);
                    dc.setStopType(title);
                    dc.setComment(text);
                    db.create(dc);

                    //Location loc = currentLocation;//locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    addMarker(Marker_Type.STOP, title, loc);
                }
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Simple method to add markers to the map
     * @param title text of the marker
     * @param type If is GPS marker or Stop marker
     */
    private void addMarker(Marker_Type type, String title, Location loc) {

        LatLng coordinates = new LatLng(loc.getLatitude(), loc.getLongitude());


        Log.i("DB", "Adding marker to (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");

        switch (type) {
            case GPS: map.addMarker(new MarkerOptions().position(coordinates)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_device_gps_fixed)));
                ((LogTabFragment) getHiddenFragment()).appendLog(NEW_POSITION + loc.getLatitude() + ", " + loc.getLongitude());
                break;
            case STOP: map.addMarker(new MarkerOptions().position(coordinates).title(title));
                ((LogTabFragment) getHiddenFragment()).appendLog(NEW_POSITION + loc.getLatitude() + ", " + loc.getLongitude());
                break;
            case POSITION:
                if (currentMarker != null) {
                    currentMarker.remove();
                }
                currentMarker = map.addMarker(new MarkerOptions().position(coordinates)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));
                ((LogTabFragment) getHiddenFragment()).appendLog(NEW_GPS + loc.getLatitude() + ", " + loc.getLongitude());
                break;
            default: Log.e("MAP", "Marker type is not valid");
        }
    }

    public Fragment getHiddenFragment(){
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        System.out.println(fragments.size());
        for(Fragment fragment : fragments){
            if(fragment != null && (fragment instanceof LogTabFragment) )//!fragment.isVisible())
                return fragment;
        }
        return null;
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

    @Override
    public void onClick(View v) {
        //do what you want to do when button is clicked
        switch(v.getId()){
            case R.id.stop_button:
                displayStopChoices();
                break;

            case R.id.start_button:
                startCollectingData();
                break;

            case R.id.end_button:
                stopCollectingData();
                break;
        }
    }


    public void stopCollectingData() {
        Log.i("BG","End repeating task");
        stopRepeatingTask();
        ((LogTabFragment) getHiddenFragment()).appendLog(DATA_END);
    }

    public void startCollectingData() {
        Log.i("BG","Start repeating task");
        startRepeatingTask();
        ((LogTabFragment) getHiddenFragment()).appendLog(DATA_START);
    }

}