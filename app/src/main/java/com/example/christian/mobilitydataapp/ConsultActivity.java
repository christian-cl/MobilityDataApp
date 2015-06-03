package com.example.christian.mobilitydataapp;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.christian.mobilitydataapp.persistence.StreetTrack;
import com.example.christian.mobilitydataapp.persistence.StreetTrackDAO;

import java.util.List;

/**
 * Created by Christian Cintrano on 3/06/15.
 *
 */
public class ConsultActivity extends AppCompatActivity {
    private static final String DATE_START_TAG = "datePickerStart";
    private static final String DATE_END_TAG = "datePickerEnd";

    private StreetTrackDAO db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consult_db);

        final ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        }

        db = new StreetTrackDAO(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        db.open();
    }

    @Override
    public void onPause() {
        super.onPause();
        db.close();
    }

    public void showDatePickerStartDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), DATE_START_TAG);
    }

    public void showDatePickerEndDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), DATE_END_TAG);
    }

    public void displayDate(int year, int month, int day, String tag) {
        Log.i("ConsultActivity", year + " " + month + " " + day + " " + tag);
        TextView tv;
        String text;
        switch (tag) {
            case DATE_START_TAG :
                tv = (TextView) findViewById(R.id.tvConsultStartDate);
                text = year + "-" + (month+1) + "-" + day;
                tv.setText(text);
                break;
            case DATE_END_TAG :
                tv = (TextView) findViewById(R.id.tvConsultEndDate);
                text = year + "-" + (month+1) + "-" + day;
                tv.setText(text);
                break;
        }
    }

    /** Called when the user clicks the Send button */
    public void findData(View view) {
        TableLayout dataTable = (TableLayout)findViewById(R.id.dataTable);
        dataTable.setStretchAllColumns(true);
        dataTable.bringToFront();
        List<StreetTrack> data = db.getAll();

        TableRow header =  new TableRow(this);
        TextView h1 = new TextView(this);
        h1.setText("Dirección");
        header.addView(h1);
        TextView h2 = new TextView(this);
        h2.setText("Fecha inicio");
        header.addView(h2);
        TextView h3 = new TextView(this);
        h3.setText("Fecha fin");
        header.addView(h3);
        TextView h4 = new TextView(this);
        h4.setText("Distancia");
        header.addView(h4);

        View div = new View(this);//<!--android:layout_height="2dip"-->
        div.setBackgroundColor(Color.parseColor("#FF909090"));
        div.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                2));

        dataTable.addView(header);
        dataTable.addView(div);
System.out.println("SIZE " + data.size());

        System.out.println(data.get(data.size()-1).getStartLatitude());
        System.out.println(data.get(data.size()-1).getStartLongitude());
        System.out.println(data.get(data.size()-1).getEndLatitude());
        System.out.println(data.get(data.size()-1).getEndLongitude());
        System.out.println(data.get(data.size()-1).getStartDateTime());
        System.out.println(data.get(data.size()-1).getEndDateTime());
        System.out.println(data.get(data.size()-1).getDistance());
        for(StreetTrack st : data){
            TableRow tr =  new TableRow(this);

            TextView c1 = new TextView(this);
            c1.setText(st.getAddress());

            TextView c2 = new TextView(this);
            c2.setText(st.getStartDateTime());
            TextView c3 = new TextView(this);
            c3.setText(st.getEndDateTime());
            TextView c4 = new TextView(this);
            c4.setText(String.valueOf(st.getDistance()));

            tr.addView(c1);
            tr.addView(c2);
            tr.addView(c3);
            tr.addView(c4);
            dataTable.addView(tr);
        }

    }
}
