package com.example.christian.mobilitydataapp;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.ExpandableListView;

import com.example.christian.mobilitydataapp.services.ExpandableListAdapter;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Christian Cintrano.
 */
public class ItineraryMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String GPS_LOADING = "Iniciando conexión GPS. Por favor, espere.";

    private List<Marker> points;
    private ProgressDialog dialogWait; // FALTA EL QUITARLO CUANDO EL MAPA TERMINE DE CARGAR
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary_map);
        points = new ArrayList<>();
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_itinerary)).getMap();
        configureMapActions();

        configureDialogWait();

    }



    private void configureMapActions() {
        //Behavior OnClick map - create mark with number
//        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
//            @Override
//            public void onMapClick(LatLng latLng) {
//                Marker marker = map.addMarker(new MarkerOptions().position(latLng));
//                points.add(marker);
//            }
//        });
        map.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                // Display Options
                points.remove(marker);
//                marker.showInfoWindow();
                return true;
            }
        });
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Marker marker = map.addMarker(new MarkerOptions().position(latLng));
                points.add(marker);
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap map) {
        dialogWait.dismiss();
    }

    private void configureDialogWait() {
        dialogWait = new ProgressDialog(this);
        dialogWait.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialogWait.setMessage(GPS_LOADING);
        dialogWait.setIndeterminate(true);
        dialogWait.setCanceledOnTouchOutside(false);
        dialogWait.show();
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
    public void onDestroy() {
        super.onDestroy();
    }

    public class MarkerInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        public MarkerInfoWindowAdapter() {
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            View v  = getLayoutInflater().inflate(R.layout.infowindow_layout, null);

            Marker myMarker = points.get(points.indexOf(marker));
//            ImageView markerIcon = (ImageView) v.findViewById(R.id.marker_icon);
//            TextView markerLabel = (TextView)v.findViewById(R.id.marker_label);
//            markerIcon.setImageResource(manageMarkerIcon(myMarker.getmIcon()));
//            markerLabel.setText(myMarker.getmLabel());
            return v;
        }
    }
}


