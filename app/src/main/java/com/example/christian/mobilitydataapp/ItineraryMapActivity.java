package com.example.christian.mobilitydataapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.christian.mobilitydataapp.persistence.Itinerary;
import com.example.christian.mobilitydataapp.services.ItineraryArrayAdapter;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christian Cintrano.
 */
public class ItineraryMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String EXTRA_TAB = "newItinerary";
    private static final String DEFAULT_ITINERARY_NAME = "Sin nombre";
    private static final int ZOOM = 15;
    private static final String GPS_LOADING = "Iniciando conexión GPS. Por favor, espere.";

    private List<Marker> points;
    private ProgressDialog dialogWait; // FALTA EL QUITARLO CUANDO EL MAPA TERMINE DE CARGAR
    private GoogleMap map;
    private ListView listView;
    private ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary_map);
        points = new ArrayList<>();
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_itinerary)).getMap();

        configureMapActions();
        configureListView();
        configureDialogWait();
    }

    private void configureListView() {
        listView = (ListView) this.findViewById(R.id.points_list);
        arrayAdapter = new ItineraryArrayAdapter(this, R.layout.list_marker, points);
        listView.setAdapter(arrayAdapter);
        listView.setTextFilterEnabled(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {}
        });
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
        map.setTrafficEnabled(true);

        CameraUpdate center= CameraUpdateFactory.newLatLng(
                new LatLng(36.7176109,-4.42346));
        CameraUpdate zoom= CameraUpdateFactory.zoomTo(ZOOM);
        map.moveCamera(center);
        map.animateCamera(zoom);

        map.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                // Display Options
                Log.i("Itinerary", "Removing marker");
                points.remove(marker);
//                marker.showInfoWindow();
                marker.remove();
                arrayAdapter.notifyDataSetChanged();
                return true;
            }
        });
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Marker marker = map.addMarker(new MarkerOptions().position(latLng));
                points.add(marker);
                arrayAdapter.notifyDataSetChanged();
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

    public void sendMessageSaveItinerary(View view) {
        Intent intent = new Intent(this, ItineraryActivity.class);
        List latLngList = new ArrayList();
        for(Marker m : points) {
            latLngList.add(m.getPosition());
        }
        TextView textViewName = (TextView) this.findViewById(R.id.editText_itinerary_name);
        String name = textViewName.getText().length() == 0 ? DEFAULT_ITINERARY_NAME
                : textViewName.getText().toString();
        Itinerary itinerary = new Itinerary(name, latLngList);
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_TAB, itinerary);
        intent.putExtra(EXTRA_TAB, bundle);
        startActivity(intent);
    }
}


