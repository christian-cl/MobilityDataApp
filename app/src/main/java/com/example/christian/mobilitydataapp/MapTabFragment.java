package com.example.christian.mobilitydataapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.christian.mobilitydataapp.persistence.DataCapture;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 * Fragment by main tab: Map view
 */
public class MapTabFragment extends Fragment implements View.OnClickListener {

    private static final int ZOOM = 20;
    private static final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros"};


    public static enum Marker_Type {GPS, STOP, POSITION}

    private Context context;

    private View view;

    private Marker currentMarker;

    String title = null;
    private GoogleMap map; // Might be null if Google Play services APK is not available.

    private SimpleDateFormat sdf;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.activity_maps, container, false);
        } catch (InflateException e) {
        /* map is already there, just return view as it is */
            Log.e("M", e.getMessage());
            e.printStackTrace();
        }

        ImageButton bStop = (ImageButton) view.findViewById(R.id.stop_button);
        Button bStart = (Button) view.findViewById(R.id.start_button);
        Button bEnd = (Button) view.findViewById(R.id.end_button);
        bStop.setOnClickListener(this);
        bStart.setOnClickListener(this);
        bEnd.setOnClickListener(this);

        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);

        context = container.getContext();
        map = ((MapFragment) ((Activity) context).getFragmentManager().findFragmentById(R.id.map)).getMap();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    //        startRepeatingTask();
    }

    @Override
    public void onPause() {
        super.onPause();
//        stopRepeatingTask();
//        ((LogTabFragment) getHiddenFragment()).appendLog(DATA_END);
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        locationManager.removeGpsStatusListener(mGPSStatusListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        locationManager.removeGpsStatusListener(mGPSStatusListener);
    }



//    private String getStreet(Location localization) {
//        if(localization != null) {
//            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
//            List<Address> addresses;
//            try {
//                addresses = geocoder.getFromLocation(localization.getLatitude(), localization.getLongitude(), 1);
//                // Only considered the first result
//                if(addresses != null) {
//                    return addresses.get(0).getAddressLine(0);
//                } else {
//                    return null;
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                return null;
//            }
//        } else {
//            return null;
//        }
//    }

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
                    Location loc = ((MapTabActivity) context).locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    String text = null;
                    if(title.equals("Otros")) {
                        text = editText.getText().toString();
                    }

                    DataCapture dc = new DataCapture();
                    dc.setLatitude(loc.getLatitude());
                    dc.setLongitude(loc.getLongitude());
                    dc.setStopType(title);
                    dc.setComment(text);
                    dc.setDate(sdf.format(Calendar.getInstance().getTime()));

                    ((MapTabActivity) context).saveData(dc);

//                            ((MapTabActivity) context).db.create(dc);

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


    public void setCamera(LatLng latLng) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, ZOOM);
        map.animateCamera(cameraUpdate);
    }

    /**
     * Simple method to add markers to the map
     * @param title text of the marker
     * @param type If is GPS marker or Stop marker
     * @param loc location of market
     */
    public void addMarker(Marker_Type type, String title, Location loc) {

        LatLng coordinates = new LatLng(loc.getLatitude(), loc.getLongitude());


        Log.i("DB", "Adding marker to (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");

        switch (type) {
            case GPS: map.addMarker(new MarkerOptions().position(coordinates)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_device_gps_fixed)));
//                ((LogTabFragment) getHiddenFragment(Tab_Type.LogTabFragment)).appendLog(NEW_POSITION + loc.getLatitude() + ", " + loc.getLongitude());
                break;
            case STOP: map.addMarker(new MarkerOptions().position(coordinates).title(title));
//                ((LogTabFragment) getHiddenFragment(Tab_Type.LogTabFragment)).appendLog(NEW_POSITION + loc.getLatitude() + ", " + loc.getLongitude());
                break;
            case POSITION:
                if (currentMarker != null) {
                    currentMarker.remove();
                }
                currentMarker = map.addMarker(new MarkerOptions().position(coordinates)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));
//                ((LogTabFragment) getHiddenFragment(Tab_Type.LogTabFragment)).appendLog(NEW_GPS + loc.getLatitude() + ", " + loc.getLongitude());
                break;
            default: Log.e("MAP", "Marker type is not valid");
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
                if(!((MapTabActivity) context).runningCaptureData) startCollectingData();
                break;

            case R.id.end_button:
                stopCollectingData();
                break;
        }
    }


    public void stopCollectingData() {
        Log.i("BG","End repeating task");
        Toast.makeText(context, "Finalizando captura de datos",
                Toast.LENGTH_SHORT).show();
        ((MapTabActivity) context).stopRepeatingTask();
//        ((LogTabFragment) getHiddenFragment(Tab_Type.LogTabFragment)).appendLog(DATA_END);
    }

    public void startCollectingData() {
        Log.i("BG","Start repeating task");
        Toast.makeText(context, "Iniciando captura de datos",
                    Toast.LENGTH_SHORT).show();
        ((MapTabActivity) context).startRepeatingTask();
//        ((LogTabFragment) getHiddenFragment(Tab_Type.LogTabFragment)).appendLog(DATA_START);
    }






}
