/**
 * Christian Cintrano on 27/04/15.
 */

package com.example.christian.mobilitydataapp;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DebugActivity extends ActionBarActivity implements LocationListener {
    private final static long TIEMPO_MIN = 10 * 1000; // 10 segundos
    private final static long DISTANCIA_MIN = 5; // 5 metros
    private final static String[] A = {"n/d", "preciso", "impreciso"};
    private final static String[] P = {"n/d", "bajo", "medio", "alto"};
    private final static String[] E = {"fuera de servicio",
            "temporalmente no disponible ", "disponible"};

    private LocationManager manejador;
    private String proveedor;
    private TextView salida;
    private ProgressDialog progress;
    MobilitySQLite db;
    ProgressReceiver rcv;

    // Process to repeat
    private int INTERVAL = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;
    private boolean isGPSEnabled;
    private long MIN_TIME_BW_UPDATES_GPS = 2000;
    private long MIN_DISTANCE_CHANGE_FOR_UPDATES_GPS = 1;
    private LocationListener gpslocationListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        salida = (TextView) findViewById(R.id.salida);
        db = new MobilitySQLite(this);
        manejador = (LocationManager) getSystemService(LOCATION_SERVICE);


        log("Proveedores de localización: \n ");
        muestraProveedores();

        Criteria criterio = new Criteria();
        criterio.setCostAllowed(false);
        criterio.setAltitudeRequired(false);
        criterio.setAccuracy(Criteria.ACCURACY_FINE);
        proveedor = manejador.getBestProvider(criterio, true);
        log("Mejor proveedor: " + proveedor + "\n");
        log("Comenzamos con la última localización conocida:");

        Location localization = manejador.getLastKnownLocation(proveedor);
        muestraLocaliz(localization);

//        log("======================");
        manejador = (LocationManager)this.getSystemService(LOCATION_SERVICE);
        isGPSEnabled = manejador.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled)
        {
            if (manejador != null)
            {

                // Register GPSStatus listener for events
                manejador.addGpsStatusListener(mGPSStatusListener);
                gpslocationListener = new LocationListener()
                {
                    public void onLocationChanged(Location loc)
                    {
                    }
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                    public void onProviderEnabled(String provider) {}
                    public void onProviderDisabled(String provider) {}
                };

                manejador.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES_GPS, MIN_DISTANCE_CHANGE_FOR_UPDATES_GPS,
                        gpslocationListener);
            }
        }
//        log("======================");


//        mHandler = new Handler();
//
//        progress = new ProgressDialog(this);
//
//        // For alternative thread task
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(MiIntentService.ACTION_PROGRESO);
//        filter.addAction(MiIntentService.ACTION_FIN);
//        rcv = new ProgressReceiver();
//        registerReceiver(rcv, filter);
    }

    // Métodos del ciclo de vida de la actividad
    @Override
    protected void onResume() {
        super.onResume();
        // Activamos notificaciones de localización
        manejador.requestLocationUpdates(proveedor, TIEMPO_MIN, DISTANCIA_MIN, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        manejador.removeUpdates(this);
        unregisterReceiver(rcv);
    }

    // Métodos de la interfaz LocationListener
    public void onLocationChanged(Location location) {
        log("Nueva localización: ");
        muestraLocaliz(location);
       // processInformation(location);
    }

    private void processInformation(Location location) {
        // Get latitude and longitude
        double lat = location.getLatitude();
        double lon = location.getLongitude();
//        float speed = location.getSpeed();

        // Get address
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(lat, lon, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Address address = addresses.get(0);
        address.getAddressLine(0);
    }

    public void geo(Location localization) {
        if (localization != null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        List<Address> addresses = geocoder.getFromLocationName(myLocation, 1);
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(localization.getLatitude(), localization.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            log(addresses.toString());
//        Address address = addresses.get(0);
//        double longitude = address.getLongitude();
//        double latitude = address.getLatitude();

        } else {
            log("LOCATION NULL");
        }
    }

    public void onProviderDisabled(String proveedor) {
        log("Proveedor deshabilitado: " + proveedor + "\n");
    }

    public void onProviderEnabled(String proveedor) {
        log("Proveedor habilitado: " + proveedor + "\n");
    }

    public void onStatusChanged(String proveedor, int estado,
                                Bundle extras) {
        log("Cambia estado proveedor: " + proveedor + ", estado="
                + E[Math.max(0, estado)] + ", extras=" + extras + "\n");
    }

    public GpsStatus.Listener mGPSStatusListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                Toast.makeText(DebugActivity.this, "GPS_SEARCHING", Toast.LENGTH_SHORT).show();
                System.out.println("TAG - GPS searching: ");
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                System.out.println("TAG - GPS Stopped");
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:

                /*
                 * GPS_EVENT_FIRST_FIX Event is called when GPS is locked
                 */
                Toast.makeText(DebugActivity.this, "GPS_LOCKED", Toast.LENGTH_SHORT).show();
                Location gpslocation = manejador
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (gpslocation != null) {
                    System.out.println("GPS Info:" + gpslocation.getLatitude() + ":" + gpslocation.getLongitude());

                    /*
                     * Removing the GPS status listener once GPS is locked
                     */
                    manejador.removeGpsStatusListener(mGPSStatusListener);
                }

                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                //                 System.out.println("TAG - GPS_EVENT_SATELLITE_STATUS");
                break;
        }
        }
    };

    // Métodos para mostrar información
    private void log(String cadena) {
        salida.append(cadena + "\n");
    }

    private void muestraLocaliz(Location localizacion) {
        if (localizacion == null)
            log("Localización desconocida\n");
        else
            log(localizacion.toString() + "\n");
    }

    private void muestraProveedores() {
        log("Proveedor de localización: \n");
        List<String> proveedores = manejador.getAllProviders();
        for (String proveedor : proveedores) {
            muestraProveedor(proveedor);
        }
    }

    private void muestraProveedor(String proveedor) {
        LocationProvider info = manejador.getProvider(proveedor);
        log("LocationProvider[ "+"getName=" + info.getName()
                + ", isProviderEnabled="
                + manejador.isProviderEnabled(proveedor)+", getAccuracy="
                + A[Math.max(0, info.getAccuracy())]+ ", getPowerRequirement="
                + P[Math.max(0, info.getPowerRequirement())]
                +", hasMonetaryCost=" + info.hasMonetaryCost()
                + ", requiresCell=" + info.requiresCell()
                + ", requiresNetwork=" + info.requiresNetwork()
                + ", requiresSatellite=" + info.requiresSatellite()
                + ", supportsAltitude=" + info.supportsAltitude()
                + ", supportsBearing=" + info.supportsBearing()
                + ", supportsSpeed=" + info.supportsSpeed()+" ]\n");
    }

    /** Called when the user clicks the Send button */
    public void sendMessage(View view) {
//        progress.create();
        progress.show();
        Intent msgIntent = new Intent(DebugActivity.this, MiIntentService.class);
        msgIntent.putExtra("iteraciones", 10);
        startService(msgIntent);
    }

    public void start(View view) {
        startRepeatingTask();
    }

    public void stop(View view) {
        stopRepeatingTask();
    }

    public void show(View view) {
        log(db.listAllPoints().toString());
    }

    public void reset(View view) {
        db.resetTablePoints();
    }


    public void saveFile(View view) {
        String root = Environment.getExternalStorageDirectory().toString();
        File dir = new File(root + "/mdaFolder");
        log(root + "/mdaFolder");
        dir.mkdirs();
        String text = "TEXTO DE PruEBA";
        String fileName = "datos.txt";
        File file = new File (dir, fileName);
        if (file.exists())
            file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public class ProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MiIntentService.ACTION_PROGRESO)) {
                int prog = intent.getIntExtra("progreso", 0);
                progress.setProgress(prog);
            }
            else if(intent.getAction().equals(MiIntentService.ACTION_FIN)) {
                Toast.makeText(DebugActivity.this, "Tarea finalizada!", Toast.LENGTH_SHORT).show();
                MobilitySQLite db = new MobilitySQLite(DebugActivity.this);
                log(db.listAllPoints().toString());
            }
        }
    }




    // Repeat process for catch information
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            updateStatus(); //this function can change value of mInterval.
            mHandler.postDelayed(mStatusChecker, INTERVAL);
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    void updateStatus() {
        Location localizacion = manejador.getLastKnownLocation(proveedor);

        db.savePoints(50,-9.674,"ET 742");
        muestraLocaliz(localizacion);
//        log("RRLocalización desconocida\n");
    }

/*
    private void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

}
