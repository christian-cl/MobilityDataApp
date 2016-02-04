package com.example.christian.neotrack;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.christian.neotrack.fragments.DatePickerFragment;
import com.example.christian.neotrack.persistence.StreetTrack;
import com.example.christian.neotrack.persistence.StreetTrackDAO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
        dataTable.removeAllViewsInLayout();
        dataTable.bringToFront();
        List<StreetTrack> data = searchByLayoutFields();

        TableRow header = new TableRow(this);
        header.setPadding(20,10,20,10);
        for(String s : TABLE_HEADERS) {
            TextView h = new TextView(this);
            h.setText(s);
            header.addView(h);
        }

        View div = new View(this);
        div.setBackgroundColor(Color.parseColor("#FF909090"));
        div.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));

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

    private List<StreetTrack> searchByLayoutFields() {
        Log.i("DB", "searchByLayoutFields()");
        TextView startDate = (TextView) findViewById(R.id.tvConsultStartDate);
        TextView endDate = (TextView) findViewById(R.id.tvConsultEndDate);
        EditText street = (EditText) findViewById(R.id.et_streetParam);
        //crear criterion
        return db.get(street.getText().toString(), startDate.getText().toString(), endDate.getText().toString());
    }

    public void clearFields(View v) {
        TextView startDate = (TextView) findViewById(R.id.tvConsultStartDate);
        TextView endDate = (TextView) findViewById(R.id.tvConsultEndDate);
        EditText street = (EditText) findViewById(R.id.et_streetParam);
        startDate.setText(null);
        endDate.setText(null);
        street.setText(null);
    }

    public void saveFields(View v) {
        saveFile("consult");
    }
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    public void saveFile(String fileName) {
        Toast.makeText(this, "Saving file...", Toast.LENGTH_SHORT).show();
        Log.i("DB", "Saving file...");
        FileOutputStream outST = null;
        StreetTrackDAO dbST = new StreetTrackDAO(this);
        db.open();
        dbST.open();
        List<StreetTrack> dataST = searchByLayoutFields();
        Log.i("DB","Find " + dataST.size() + " StreetTrack elements");
        String extension = ".csv";
        String folderName = "/neoTrack";
        try {
            if(isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + folderName);
                dir.mkdirs();
                File fileST = new File (dir, "ST" + fileName + extension);
                outST = new FileOutputStream(fileST);
            } else {
                outST = openFileOutput(fileName + extension, Context.MODE_PRIVATE);
            }

            String headST = "_id,address;latitude start;longitude start;" +
                    "latitude end;longitude end;datetime start;datetime end;distance\n";
            outST.write(headST.getBytes());
            for(StreetTrack st : dataST) {
                outST.write((String.valueOf(st.getId()) + ";").getBytes());
                outST.write(("\"" + st.getAddress() + "\";").getBytes());
                outST.write((String.valueOf(st.getStartLatitude()) + ";").getBytes());
                outST.write((String.valueOf(st.getStartLongitude()) + ";").getBytes());
                outST.write((String.valueOf(st.getEndLatitude()) + ";").getBytes());
                outST.write((String.valueOf(st.getEndLongitude()) + ";").getBytes());
                outST.write(("\"" + st.getStartDateTime() + "\";").getBytes());
                outST.write(("\"" + st.getEndDateTime() + "\";").getBytes());
                outST.write((String.valueOf(st.getDistance()) + "\n").getBytes());
            }
            outST.flush();
            outST.close();

            Log.i("DB", "File saved");
            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                db.close();
                dbST.close();
                if(outST != null) {
                    outST.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
