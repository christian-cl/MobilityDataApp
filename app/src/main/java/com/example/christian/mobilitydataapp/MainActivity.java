/**
 * Created by Christian Cintrano on 24/04/15.
 */

package com.example.christian.mobilitydataapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements LocationListener {
    private final static long TIEMPO_MIN = 10 * 1000 ; // 10 segundos
    private final static long DISTANCIA_MIN = 5 ; // 5 metros
    private final static String[] A = { "n/d", "preciso", "impreciso" };
    private final static String[] P = { "n/d", "bajo", "medio","alto" };
    private final static String[] E = { "fuera de servicio",
            "temporalmente no disponible ","disponible" };

    private LocationManager manejador;
    private String proveedor;
    private TextView salida;
    private ProgressDialog progress;

    // Process to repeat
    private int INTERVAL = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        salida = (TextView) findViewById(R.id.salida);
//        MobilitySQLite db = new MobilitySQLite(this);

        mHandler = new Handler();
        startRepeatingTask();

        progress = new ProgressDialog(this);


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

        Location localizacion = manejador.getLastKnownLocation(proveedor);
        muestraLocaliz(localizacion);

        // For alternative thread task
        IntentFilter filter = new IntentFilter();
        filter.addAction(MiIntentService.ACTION_PROGRESO);
        filter.addAction(MiIntentService.ACTION_FIN);
        ProgressReceiver rcv = new ProgressReceiver();
        registerReceiver(rcv, filter);
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
    }

    // Métodos de la interfaz LocationListener
    public void onLocationChanged(Location location) {
        log("Nueva localización: ");
        muestraLocaliz(location);
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
                + E[Math.max(0,estado)] + ", extras=" + extras +"\n");
    }

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
        Intent msgIntent = new Intent(MainActivity.this, MiIntentService.class);
        msgIntent.putExtra("iteraciones", 10);
        startService(msgIntent);



        /*// Do something in response to button
        // Activity is a subclass from context
        Intent intent = new Intent(this, MapsActivity.class);
        // Init the activity
        startActivity(intent);*/
    }


    public class ProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MiIntentService.ACTION_PROGRESO)) {
                int prog = intent.getIntExtra("progreso", 0);
                progress.setProgress(prog);
            }
            else if(intent.getAction().equals(MiIntentService.ACTION_FIN)) {
                Toast.makeText(MainActivity.this, "Tarea finalizada!", Toast.LENGTH_SHORT).show();
                MobilitySQLite db = new MobilitySQLite(MainActivity.this);
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
        log("RRLocalización desconocida\n");
    }
}