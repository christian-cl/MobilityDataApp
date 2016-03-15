package com.example.christian.neotrack.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.christian.neotrack.TrackActivity;
import com.example.christian.neotrack.R;
import com.example.christian.neotrack.persistence.DataCapture;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 * Fragment by main tab: Map view
 */
public class MapTabFragment extends Fragment implements View.OnClickListener {

    private final static int REQ_CODE_SPEECH_INPUT = 100;
    private static final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros", "Reanudar"};

    public static enum Marker_Type {GPS, STOP, POSITION, ITINERARY}

    private Context context;
    private View view;
    private Marker currentMarker;
    private String title = null;
    private GoogleMap map; // Might be null if Google Play services APK is not available.

    private SimpleDateFormat sdf;
    private List<Marker> itineraryMarkers;

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
        ImageButton bStopSpeak = (ImageButton) view.findViewById(R.id.stop_button_speak);

//        Button bStart = (Button) view.findViewById(R.id.start_button);
//        Button bEnd = (Button) view.findViewById(R.id.end_button);
        bStop.setOnClickListener(this);
        bStopSpeak.setOnClickListener(this);
//        bStart.setOnClickListener(this);
//        bEnd.setOnClickListener(this);

        itineraryMarkers = new ArrayList<>();

        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);

        context = container.getContext();
        map = ((MapFragment) ((Activity) context)
                .getFragmentManager().findFragmentById(R.id.map)).getMap();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
                    InputMethodManager imm =
                        (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
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
                    Location loc = ((TrackActivity) context)
                            .locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

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

                    ((TrackActivity) context).runSaveData(dc);
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
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
        map.moveCamera(cameraUpdate);
    }

    public void setZoom(float zoom) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(zoom);
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
            case ITINERARY:
                itineraryMarkers.add(map.addMarker(new MarkerOptions().position(coordinates)
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                ));
                break;
            default: Log.e("MAP", "Marker type is not valid");
        }
    }

    public void clearItineraryMarkers() {
        for (Marker marker : itineraryMarkers) {
            marker.remove();
        }
        itineraryMarkers = new ArrayList<>();
    }

    @Override
    public void onClick(View v) {
        //do what you want to do when button is clicked
        System.out.println(v);
        switch(v.getId()){
            case R.id.stop_button:
                Log.i("Click", "stop_button");
                displayStopChoices();
                break;

            case R.id.stop_button_speak:
                Log.i("Click", "stop_button_speak");
//                promptSpeechInput();
                ((TrackActivity) context).restartSpeech();
                break;

//            case R.id.start_button:
//                Log.i("Click", "start_button");
//                if(!((MapTabActivity) context).runningCaptureData) startCollectingData();
//                break;
//
//            case R.id.end_button:
//                Log.i("Click", "end_button");
//                stopCollectingData();
//                break;
        }
    }

    /**
     * Showing google speech input dialog
     * */
    public void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.speech_prompt);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        try {
            ((Activity) context).startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(context, R.string.speech_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

//    public void stopCollectingData() {
//        Log.i("BG","End repeating task");
//        Toast.makeText(context, "Finalizando captura de datos",
//                Toast.LENGTH_SHORT).show();
//        ((MapTabActivity) context).stopRepeatingTask();
//    }
//
//    public void startCollectingData() {
//        Log.i("BG","Start repeating task");
//        Toast.makeText(context, "Iniciando captura de datos",
//                    Toast.LENGTH_SHORT).show();
//        ((MapTabActivity) context).startRepeatingTask();
//    }

}
