package com.example.christian.mobilitydataapp.persistence;

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
    private static final int DATABASE_VERSION = 4;

    public static class TableDataCapture{
        public static String TABLE_NAME = "datacapture";
        public static String COLUMN_ID = "_id";
        public static String COLUMN_LATITUDE = "latitude";
        public static String COLUMN_LONGITUDE = "longitude";
        public static String COLUMN_ADDRESS = "address";
        public static String COLUMN_STOP_TYPE = "stoptype";
        public static String COLUMN_COMMENT = "comment";
        public static String COLUMN_DATE = "date";
    }

    public static class TableStreetTrack {
        public static String TABLE_NAME = "streettrack";
        public static String COLUMN_ID = "_id";
        public static String COLUMN_ADDRESS = "address";
        public static String COLUMN_START_LATITUDE = "startLatitude";
        public static String COLUMN_START_LONGITUDE = "startLongitude";
        public static String COLUMN_END_LATITUDE = "endLatitude";
        public static String COLUMN_END_LONGITUDE = "endLongitude";
        public static String COLUMN_START_DATETIME = "startDateTime";
        public static String COLUMN_END_DATETIME = "endDateTime";
        public static String COLUMN_DISTANCE = "distance";
    }

    private static final String CREATE_TABLE_DATA_CAPTURE =
            "create table " + TableDataCapture.TABLE_NAME + "(" +
            TableDataCapture.COLUMN_ID + " integer primary key autoincrement, " +
            TableDataCapture.COLUMN_LATITUDE + " real not null, " +
            TableDataCapture.COLUMN_LONGITUDE + " real not null, " +
            TableDataCapture.COLUMN_ADDRESS + " text, " +
            TableDataCapture.COLUMN_STOP_TYPE + " text, " +
            TableDataCapture.COLUMN_COMMENT + " text, " +
            TableDataCapture.COLUMN_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ");";

    private static final String CREATE_TABLE_STREET_TRACK =
            "create table " + TableStreetTrack.TABLE_NAME + "(" +
            TableStreetTrack.COLUMN_ID + " integer primary key autoincrement, " +
            TableStreetTrack.COLUMN_ADDRESS + " text not null, " +
            TableStreetTrack.COLUMN_START_LATITUDE + " real not null, " +
            TableStreetTrack.COLUMN_START_LONGITUDE + " real not null, " +
            TableStreetTrack.COLUMN_END_LATITUDE + " real not null, " +
            TableStreetTrack.COLUMN_END_LONGITUDE + " real not null, " +
            TableStreetTrack.COLUMN_START_DATETIME + " DATETIME not null, " +
            TableStreetTrack.COLUMN_END_DATETIME + " DATETIME not null, " +
            TableStreetTrack.COLUMN_DISTANCE + " real not null, " +
            ");";

    private static final String DROP_TABLE_DATA_CAPTURE =
            "drop table if exists "+ TableDataCapture.TABLE_NAME + ";";
    private static final String DROP_CREATE_TABLE_STREET_TRACK =
            "drop table if exists "+ TableStreetTrack.TABLE_NAME + ";";

    private static final String DATABASE_CREATE =
            CREATE_TABLE_DATA_CAPTURE + CREATE_TABLE_STREET_TRACK;

    private static final String DATABASE_DROP =
            DROP_TABLE_DATA_CAPTURE + DROP_CREATE_TABLE_STREET_TRACK;

    public MySQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL(DATABASE_DROP);
        onCreate(db);
    }

}
