package com.example.christian.neotrack.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Christian Cintrano on 5/05/15.
 *
 * Data Base creation
 */
public class MySQLiteOpenHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mobilityDB";
    private static final int DATABASE_VERSION = 9;

    public static class TableDataCapture{
        public static String TABLE_NAME = "datacapture";
        public static String COLUMN_ID = "_id";
        public static String COLUMN_SESSION = "session";
        public static String COLUMN_LATITUDE = "latitude";
        public static String COLUMN_LONGITUDE = "longitude";
        public static String COLUMN_STOP_TYPE = "stoptype";
        public static String COLUMN_COMMENT = "comment";
        public static String COLUMN_DATE = "date";
        public static String COLUMN_SENSOR_ACCELERATION = "acceleration";
        public static String COLUMN_SENSOR_PRESSURE = "pressure";
        public static String COLUMN_SENSOR_LIGHT = "light";
        public static String COLUMN_SENSOR_TEMPERATURE = "temperature";
        public static String COLUMN_SENSOR_HUMIDITY = "humidity";
    }

    public static class TableItinerary {
        public static String TABLE_NAME = "itineraries";
        public static String COLUMN_ID = "_id";
        public static String COLUMN_NAME = "name";
        public static String COLUMN_LATITUDE = "latitude";
        public static String COLUMN_LONGITUDE = "longitude";
        public static String COLUMN_ADDRESS = "address";
    }

    private static final String CREATE_TABLE_DATA_CAPTURE =
            "create table " + TableDataCapture.TABLE_NAME + "(" +
            TableDataCapture.COLUMN_ID + " integer primary key autoincrement, " +
            TableDataCapture.COLUMN_SESSION + " text, " +
            TableDataCapture.COLUMN_LATITUDE + " real not null, " +
            TableDataCapture.COLUMN_LONGITUDE + " real not null, " +
            TableDataCapture.COLUMN_STOP_TYPE + " text, " +
            TableDataCapture.COLUMN_COMMENT + " text, " +
            TableDataCapture.COLUMN_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            TableDataCapture.COLUMN_SENSOR_ACCELERATION + " real, " +
            TableDataCapture.COLUMN_SENSOR_PRESSURE + " real, " +
            TableDataCapture.COLUMN_SENSOR_LIGHT + " real, " +
            TableDataCapture.COLUMN_SENSOR_TEMPERATURE + " real, " +
            TableDataCapture.COLUMN_SENSOR_HUMIDITY + " real" +
            ");";

    private static final String CREATE_TABLE_ITINERARY =
            "create table " + TableItinerary.TABLE_NAME + "(" +
            TableItinerary.COLUMN_ID + " integer primary key autoincrement, " +
            TableItinerary.COLUMN_NAME + " text not null, " +
            TableItinerary.COLUMN_LATITUDE + " real not null, " +
            TableItinerary.COLUMN_LONGITUDE + " real not null, " +
            TableItinerary.COLUMN_ADDRESS + " text not null" +
            ");";

    private static final String DROP_TABLE_DATA_CAPTURE =
            "drop table if exists "+ TableDataCapture.TABLE_NAME + ";";
    private static final String DROP_CREATE_TABLE_ITINERARY =
            "drop table if exists "+ TableItinerary.TABLE_NAME + ";";


    public MySQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(CREATE_TABLE_DATA_CAPTURE);
        db.execSQL(CREATE_TABLE_ITINERARY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        if(newVersion > oldVersion) {
            db.execSQL(DROP_TABLE_DATA_CAPTURE);
            db.execSQL(DROP_CREATE_TABLE_ITINERARY);
            onCreate(db);
        }
    }

}
