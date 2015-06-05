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

    private static final String[] TABLE_HEADERS =
            {"Dirección", "Fecha inicio", "Fecha fin", "Distancia"};

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

        TableRow header = new TableRow(this);
        header.setPadding(20,10,20,10);
        for(String s : TABLE_HEADERS) {
            TextView h = new TextView(this);
            h.setText("Dirección  ");
            header.addView(h);
        }

        View div = new View(this);//<!--android:layout_height="2dip"-->
        div.setBackgroundColor(Color.parseColor("#FF909090"));
        div.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                2));

        dataTable.addView(header);
        dataTable.addView(div);

        for(StreetTrack st : data){
            TableRow tr =  new TableRow(this);
            tr.setPadding(10,10,10,10);

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
