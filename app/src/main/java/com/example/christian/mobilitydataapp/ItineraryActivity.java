package com.example.christian.mobilitydataapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.example.christian.mobilitydataapp.persistence.Itinerary;
import com.example.christian.mobilitydataapp.services.ExpandableListAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christian Cintrano.
 */
public class ItineraryActivity extends AppCompatActivity {

    private static final String TAG = "ItineraryActivity";
    private final String EXTRA_TAB = "newItinerary";
    private final String FOLDER_PATH = "/neoTrack";
    private final String FILE_NAME = "itinerarios";
    private final String FILE_EXTENSION = ".json";

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<Itinerary> itineraryList;
    private String ITIENRARIES_SAVED = "Itinerarios guardados";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary);

        itineraryList = new ArrayList<>();

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expandableListView_itineraries);
        // preparing list data
//        prepareListData();
        listAdapter = new ExpandableListAdapter(this, itineraryList);
        // setting list adapter
        expListView.setAdapter(listAdapter);

        Bundle newItinerary = getIntent().getParcelableExtra(EXTRA_TAB);
        if(newItinerary != null) {
            Log.i(TAG, newItinerary.getParcelable(EXTRA_TAB).toString());
            itineraryList.add((Itinerary) newItinerary.getParcelable(EXTRA_TAB));
            listAdapter.notifyDataSetChanged();
        }
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

    public void createItinerary(View view) {
        Log.i("ItineraryActivity", "new itinerary...");
        Intent intent = new Intent(this, ItineraryMapActivity.class);
        startActivity(intent);
    }

    public void editItinerary(int index) {
        Log.i("ItineraryActivity", "editing itinerary...");
        Intent intent = new Intent(this, ItineraryMapActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_TAB, itineraryList.get(index));
        intent.putExtra(EXTRA_TAB, bundle);
        startActivity(intent);
    }

    public void removeItinerary(int index) {
        Log.i("ItineraryActivity", "deleting...");
        alertMessage(index);
    }

    public void alertMessage(final int index) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        itineraryList.remove(index);
                        // SKET remove in database
                        listAdapter.notifyDataSetChanged();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE: // No button clicked // do nothing
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("¿Está seguro?")
                .setPositiveButton("Sí", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    public void sendMessageExportItinerary(View view) {
        List<JSONObject> itineraries = new ArrayList<>();
        for(Itinerary i : itineraryList) {
            itineraries.add(i.getJSONObject());
        }
        Log.i(TAG, "saving... " + new JSONArray(itineraries));
        saveJSON(new JSONArray(itineraries));
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void saveJSON(JSONArray json) {
        String content = json.toString();
        FileOutputStream out = null;
        try {
            if (isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + FOLDER_PATH);
                dir.mkdirs();
                File file = new File (dir, FILE_NAME + FILE_EXTENSION);
                out = new FileOutputStream(file);
                out.write(content.getBytes());
                out.close();
                Toast.makeText(this, ITIENRARIES_SAVED, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }





    /*
     * Preparing the list data
     */
//    private void prepareListData() {
//        listDataHeader = new ArrayList<>();
//        listDataChild = new HashMap<>();
//
//        // Adding child data
//        listDataHeader.add("Top 250");
//        listDataHeader.add("Now Showing");
//        listDataHeader.add("Coming Soon..");
//
//        // Adding child data
//        List<String> top250 = new ArrayList<>();
//        top250.add("The Shawshank Redemption");
//        top250.add("The Godfather");
//        top250.add("The Godfather: Part II");
//        top250.add("Pulp Fiction");
//        top250.add("The Good, the Bad and the Ugly");
//        top250.add("The Dark Knight");
//        top250.add("12 Angry Men");
//
//        List<String> nowShowing = new ArrayList<>();
//        nowShowing.add("The Conjuring");
//        nowShowing.add("Despicable Me 2");
//        nowShowing.add("Turbo");
//        nowShowing.add("Grown Ups 2");
//        nowShowing.add("Red 2");
//        nowShowing.add("The Wolverine");
//
//        List<String> comingSoon = new ArrayList<>();
//        comingSoon.add("2 Guns");
//        comingSoon.add("The Smurfs 2");
//        comingSoon.add("The Spectacular Now");
//        comingSoon.add("The Canyons");
//        comingSoon.add("Europa Report");
//
//        listDataChild.put(listDataHeader.get(0), top250); // Header, Child data
//        listDataChild.put(listDataHeader.get(1), nowShowing);
//        listDataChild.put(listDataHeader.get(2), comingSoon);
//    }
}


