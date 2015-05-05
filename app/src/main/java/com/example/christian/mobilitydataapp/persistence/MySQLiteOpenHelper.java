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
    private static final int DATABASE_VERSION = 2;

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

    private static final String DATABASE_CREATE= "create table " + TableDataCapture.TABLE_NAME
            + "(" + TableDataCapture.COLUMN_ID + " integer primary key autoincrement, " +
            TableDataCapture.COLUMN_LATITUDE + " real not null, " +
            TableDataCapture.COLUMN_LONGITUDE + " real not null, " +
            TableDataCapture.COLUMN_ADDRESS + " text, " +
            TableDataCapture.COLUMN_STOP_TYPE + " text, " +
            TableDataCapture.COLUMN_COMMENT + " text, " +
            TableDataCapture.COLUMN_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ");";

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
        db.execSQL("drop table if exists " + TableDataCapture.TABLE_NAME);
        onCreate(db);
    }

}
