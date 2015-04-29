package com.example.christian.mobilitydataapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
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

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends ActionBarActivity implements LocationListener {

    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    private static final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros"};

    String title = null;

    private GoogleMap map; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private String proveedor;
    private ProgressDialog dialogWait;
    private MobilitySQLite db;

    // Process to repeat
    private static final int INTERVAL = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;
    private Location currentLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dialogWait = new ProgressDialog(this);
        dialogWait.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialogWait.setMessage("Loading. Please wait...");
        dialogWait.setIndeterminate(true);
        dialogWait.setCanceledOnTouchOutside(false);
        dialogWait.show();

        db = new MobilitySQLite(this);
System.out.println("PRE MANEJADOR");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this, "GPS is Enabled in your devide", Toast.LENGTH_SHORT).show();
        }else{
            showGPSDisabledAlertToUser();
        }
        currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        System.out.println("currentlocation");
        System.out.println(currentLocation);
        System.out.println("POS MANEJADOR");

        Criteria criterio = new Criteria();
        criterio.setCostAllowed(false);
        criterio.setAltitudeRequired(false);
        criterio.setAccuracy(Criteria.ACCURACY_FINE);
        proveedor = locationManager.getBestProvider(criterio, true);

        mHandler = new Handler();


        setUpMapIfNeeded();
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    private void showGPSDisabledAlertToUser(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Goto Settings Page To Enable GPS",
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_map, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MIN_TIME, MIN_DISTANCE, this);
            //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
            // Check if we were successful in obtaining the map.
            if (map != null) {
                dialogWait.hide();
                startRepeatingTask();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRepeatingTask();
    }


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
        System.out.println("MANEJ");
        System.out.println(locationManager);
        System.out.println(proveedor);
        System.out.println(locationManager.getProviders(true).toString());
        System.out.println(locationManager.getLastKnownLocation(proveedor));

        Location loc = locationManager.getLastKnownLocation(proveedor);

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



    // GPS
    @Override
    public void onLocationChanged(Location location) {
        System.out.println("New location!");
        currentLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        map.animateCamera(cameraUpdate);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }





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
                    Location loc = locationManager.getLastKnownLocation(proveedor);
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

        Location loc = locationManager.getLastKnownLocation(proveedor);

//        db.savePoints(loc.getLatitude(),loc.getLongitude(),getStreet(loc));
        LatLng coordinates = new LatLng(loc.getLatitude(), loc.getLongitude());
        map.addMarker(new MarkerOptions().position(coordinates).title(title));
    }

}
