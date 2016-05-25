package com.example.christian.mobilitydataapp.services;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.example.christian.mobilitydataapp.persistence.DataCapture;
import com.example.christian.mobilitydataapp.persistence.StreetTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by Christian Cintrano on 8/06/15.
 *
 */
public class SaveData {
    private final static String EXTENSION = ".csv";
    private final static String FOLDER_NAME = "/neoTrack";
    private final static String NULL_SYMBOL = "NULL";

    public final static String TYPE_DATA_CAPTURE = "dataCapture";
    private final static String DATA_CAPTURE_HEAD =
            "_id;latitude;longitude;street;stoptype;comment;date\n";
    public final static String TYPE_STREET_TRACK = "streetTrack";
    private final static String STREET_TRACK_HEAD = "_id;address;latitude start;longitude start;" +
            "latitude end;longitude end;datetime start;datetime end;distance\n";

    /* Checks if external storage is available for read and write */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean saveFile(String fileName, List data, String type, Context context) {
//        Toast.makeText(this, "Saving file...", Toast.LENGTH_SHORT).show();
        Log.i("DB", "Saving file...");
        FileOutputStream out = null;
        try {
            if(isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + FOLDER_NAME);
                if(dir.mkdirs()) Log.i("DB", "Folder created");
                File file = new File (dir, fileName + EXTENSION);
                out = new FileOutputStream(file);
            } else {
                out = context.openFileOutput(fileName + EXTENSION, Context.MODE_PRIVATE);
            }
            switch(type) {
                case TYPE_DATA_CAPTURE:
                    saveDataCaptureIntoFile(out,(List<DataCapture>) data);
                    break;
                case TYPE_STREET_TRACK :
                    saveStreetTrackIntoFile(out,(List<StreetTrack>) data);
                    break;
            }

            out.flush();
            out.close();
            Log.i("DB", "File saved with name: " + fileName);
//            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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

    private static void saveDataCaptureIntoFile(FileOutputStream out, List<DataCapture> data)
            throws IOException{
        out.write(DATA_CAPTURE_HEAD.getBytes());
        for(DataCapture dc : data) {
            out.write((String.valueOf(dc.getId()) + ";").getBytes());
            out.write((String.valueOf(dc.getLatitude()) + ";").getBytes());
            out.write((String.valueOf(dc.getLongitude()) + ";").getBytes());
            if(dc.getAddress() != null) {
                out.write(("\"" + dc.getAddress() + "\";").getBytes());
            } else {
                out.write((NULL_SYMBOL +";").getBytes());
            }
            if(dc.getStopType() != null) {
                out.write(("\"" + dc.getStopType() + "\";").getBytes());
            } else {
                out.write((NULL_SYMBOL + ";").getBytes());
            }
            if(dc.getComment() != null) {
                out.write(("\"" + dc.getComment() + "\";").getBytes());
            } else {
                out.write((NULL_SYMBOL + ";").getBytes());
            }
            out.write(("\"" + dc.getDate() + "\"\n").getBytes());
        }
    }

    private static void saveStreetTrackIntoFile(FileOutputStream out, List<StreetTrack> data)
            throws IOException{
        out.write(STREET_TRACK_HEAD.getBytes());
        for(StreetTrack st : data) {
            out.write((String.valueOf(st.getId()) + ";").getBytes());
            out.write(("\"" + st.getAddress() + "\";").getBytes());
            out.write((String.valueOf(st.getStartLatitude()) + ";").getBytes());
            out.write((String.valueOf(st.getStartLongitude()) + ";").getBytes());
            out.write((String.valueOf(st.getEndLatitude()) + ";").getBytes());
            out.write((String.valueOf(st.getEndLongitude()) + ";").getBytes());
            out.write(("\"" + st.getStartDateTime() + "\";").getBytes());
            out.write(("\"" + st.getEndDateTime() + "\";").getBytes());
            out.write((String.valueOf(st.getDistance()) + "\n").getBytes());
        }
    }
}
