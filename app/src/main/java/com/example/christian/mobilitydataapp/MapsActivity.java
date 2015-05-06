package com.example.christian.mobilitydataapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.v7.app.ActionBarActivity;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
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

public class MapsActivity extends ActionBarActivity {

    private static final int ZOOM = 20;
    private static final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros"};
    private static enum Marker_Type {GPS, STOP, POSITION};

    private long intervalTimeGPS; // milliseconds
    private float minDistance; // meters

    String title = null;

    private GoogleMap map; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private ProgressDialog dialogWait;
    private DataCaptureDAO db;
    private TextView salida;

    private SharedPreferences pref; // Settings listener
    // Preference change listener
    private PreferenceChangeListener preferenceListener;

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
//                    dialogWait.show();
                    log("GPS searching...");
                    Toast.makeText(MapsActivity.this, "GPS_SEARCHING", Toast.LENGTH_SHORT).show();
                    System.out.println("TAG - GPS searching: ");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    log("GPS was stopped");
//                    dialogWait.show();
                    System.out.println("TAG - GPS Stopped");
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    log("GPS locked position");
                /*
                 * GPS_EVENT_FIRST_FIX Event is called when GPS is locked
                 */
                    Toast.makeText(MapsActivity.this, "GPS_LOCKED", Toast.LENGTH_SHORT).show();
                    dialogWait.dismiss();
                    Location gpslocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (gpslocation != null) {
                        String s = "GPS Info:" + gpslocation.getLatitude() + ":" + gpslocation.getLongitude();
                        System.out.println(s);
                    }
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    //                 System.out.println("TAG - GPS_EVENT_SATELLITE_STATUS");
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        loadSettings();
        preferenceListener = new PreferenceChangeListener();
        pref.registerOnSharedPreferenceChangeListener(preferenceListener);

        configureDialogWait();
        db = new DataCaptureDAO(this);
        db.open();
        mHandler = new Handler();
        salida = (TextView) findViewById(R.id.salida);
        salida.setMovementMethod(new ScrollingMovementMethod());
        log("Create activity");

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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
                        Toast.makeText(MapsActivity.this, "Enabled new provider " + provider,
                                Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onProviderDisabled(String provider) {
                        Toast.makeText(MapsActivity.this, "Disabled provider " + provider,
                                Toast.LENGTH_SHORT).show();
                    }
                };

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalTimeGPS, minDistance, gpsLocationListener);
            }
        }

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        startRepeatingTask();
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
        log("New Location");
        Toast.makeText(MapsActivity.this, "New Location", Toast.LENGTH_SHORT).show();
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
    protected void onResume() {
        super.onResume();
        db.open();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalTimeGPS, minDistance, gpsLocationListener);
        startRepeatingTask();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRepeatingTask();
        locationManager.removeUpdates(gpsLocationListener);
        db.close();
    }

    private void configureDialogWait() {
        dialogWait = new ProgressDialog(this);
        dialogWait.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialogWait.setMessage("Loading. Please wait...");
        dialogWait.setIndeterminate(true);
        dialogWait.setCanceledOnTouchOutside(false);
        dialogWait.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_map, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.action_settings:
                sendSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void sendSettings() {
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
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
            Log.i("Collect data", "Collecting data in: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());

            DataCapture dc = new DataCapture();
            dc.setLatitude(currentLocation.getLatitude());
            dc.setLongitude(currentLocation.getLongitude());
            db.create(dc);

            addMarker(Marker_Type.GPS, null, currentLocation);
        }
    }

    private String getStreet(Location localization) {
        if(localization != null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
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

    public void displayStopChoices(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // EditText by default hidden
        final EditText editText = new EditText(this);
        editText.setEnabled(false);

        builder.setTitle("Seleccione una opción");
        builder.setSingleChoiceItems(stopChoices, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (stopChoices[item].equals("Otros")) {
                    editText.setEnabled(true);
                } else {
                    editText.setEnabled(false);
                    editText.getText().clear();
                }
                title = stopChoices[item];
                String text = "Haz elegido la opcion: " + stopChoices[item];
                Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
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
//        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        db.savePoints(loc.getLatitude(),loc.getLongitude(),getStreet(loc));
        LatLng coordinates = new LatLng(loc.getLatitude(), loc.getLongitude());
        Log.i("DB", "Adding marker to (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");

        switch (type) {
            case GPS: map.addMarker(new MarkerOptions().position(coordinates)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_device_gps_fixed)));
                break;
            case STOP: map.addMarker(new MarkerOptions().position(coordinates).title(title));
                break;
            case POSITION:
                if (currentMarker != null) {
                    currentMarker.remove();
                }
                currentMarker = map.addMarker(new MarkerOptions().position(coordinates)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));
                break;
            default: Log.e("MAP", "Marker type is not valid");
        }
//        if(type.equals(Marker_Type.GPS)) {
//            map.addMarker(new MarkerOptions().position(coordinates).title(title));
//        } else if (type.equals(Marker_Type.STOP)) {
//            map.addMarker(new MarkerOptions().position(coordinates)
//                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_device_gps_fixed)));
//        } else if (type.equals(Marker_Type.POSITION)) {
//            map.addMarker(new MarkerOptions().position(coordinates)
//                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_device_gps_fixed)));
//        } else {
//            Log.e("MAP", "Marker type is not valid");
//        }
    }

    // Métodos para mostrar información
    private void log(String cadena) {
        salida.append(cadena + "\n");
        // Scrolling down
        final Layout layout = salida.getLayout();
        if(layout != null){
            int scrollDelta = layout.getLineBottom(salida.getLineCount() - 1)
                    - salida.getScrollY() - salida.getHeight();
            if(scrollDelta > 0)
                salida.scrollBy(0, scrollDelta);
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

}
