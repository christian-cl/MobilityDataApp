package com.example.christian.mobilitydataapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends ActionBarActivity {

    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 10;
    private static final int ZOOM = 80;
    private static final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros"};

    String title = null;

    private GoogleMap map; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private String provider;
    private ProgressDialog dialogWait;
    private MobilitySQLite db;

    // Process to repeat
    private static final int INTERVAL = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;
    private Location currentLocation;
    private boolean isGPSEnabled;
    private LocationListener gpslocationListener;
    private GoogleApiClient mGoogleApiClient;
    public GpsStatus.Listener mGPSStatusListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_STARTED:
                    dialogWait.show();
                    Toast.makeText(MapsActivity.this, "GPS_SEARCHING", Toast.LENGTH_SHORT).show();
                    System.out.println("TAG - GPS searching: ");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    dialogWait.show();
                    System.out.println("TAG - GPS Stopped");
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:

                /*
                 * GPS_EVENT_FIRST_FIX Event is called when GPS is locked
                 */
                    Toast.makeText(MapsActivity.this, "GPS_LOCKED", Toast.LENGTH_SHORT).show();
                    dialogWait.hide();
                    Location gpslocation = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if (gpslocation != null) {
                        System.out.println("GPS Info:" + gpslocation.getLatitude() + ":" + gpslocation.getLongitude());

                    /*
                     * Removing the GPS status listener once GPS is locked
                     */
                        locationManager.removeGpsStatusListener(mGPSStatusListener);
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
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        configureDialogWait();
        db = new MobilitySQLite(this);
        mHandler = new Handler();

        locationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            if (locationManager != null) {
                // Register GPSStatus listener for events
                locationManager.addGpsStatusListener(mGPSStatusListener);
                gpslocationListener = new LocationListener()
                {
                    @Override
                    public void onLocationChanged(Location location) {
                        int lat = (int) (location.getLatitude());
                        int lng = (int) (location.getLongitude());
                        System.out.println("LAT " + String.valueOf(lat));
                        System.out.println("LON " + String.valueOf(lng));

                        currentLocation = location;
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, ZOOM);
                        map.animateCamera(cameraUpdate);
                        locationManager.removeUpdates(this);
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

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, gpslocationListener);
            }
        }
//        log("======================");

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, gpslocationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRepeatingTask();
        locationManager.removeUpdates(gpslocationListener);
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

//    @Override
//    public void onLocationChanged(Location location) {
//        int lat = (int) (location.getLatitude());
//        int lng = (int) (location.getLongitude());
//        System.out.println("LAT " + String.valueOf(lat));
//        System.out.println("LON " + String.valueOf(lng));
//    }
//
//    @Override
//    public void onStatusChanged(String provider, int status, Bundle extras) {
//        // TODO Auto-generated method stub
//        dialogWait.hide();
//    }
//
//    @Override
//    public void onProviderEnabled(String provider) {
//        Toast.makeText(this, "Enabled new provider " + provider,
//                Toast.LENGTH_SHORT).show();
//    }
//
//    @Override
//    public void onProviderDisabled(String provider) {
//        Toast.makeText(this, "Disabled provider " + provider,
//                Toast.LENGTH_SHORT).show();
//    }

    // Repeat process for catch information
    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            updateStatus(); //this function can change value of mInterval.
            mHandler.postDelayed(mStatusChecker, INTERVAL);
        }
    };

    private void startRepeatingTask() {
        mStatusChecker.run();
    }

    private void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    private void updateStatus() {
        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        System.out.println(loc);
        if (loc != null) {
            db.savePoints(loc.getLatitude(),loc.getLongitude(),getStreet(loc));
        }
    }

    private String getStreet(Location localization) {
        if(localization != null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(localization.getLatitude(), localization.getLongitude(), 1);
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
                    String street = getStreet(loc);
                    String text = null;
                    if(title.equals("Otros")) {
                        text = editText.getText().toString();
                    }
                    db.saveComment(loc.getLatitude(),loc.getLongitude(),street, title, text);
                    addMarker(title);
                }
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Simple method to add markers to the map
     * @param title text of the marker
     */
    private void addMarker(String title) {
        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        db.savePoints(loc.getLatitude(),loc.getLongitude(),getStreet(loc));
        LatLng coordinates = new LatLng(loc.getLatitude(), loc.getLongitude());
        map.addMarker(new MarkerOptions().position(coordinates).title(title));
    }

}
