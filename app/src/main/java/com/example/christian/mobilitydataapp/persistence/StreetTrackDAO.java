package com.example.christian.mobilitydataapp.persistence;

import com.example.christian.mobilitydataapp.persistence.MySQLiteOpenHelper.TableStreetTrack;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christian Cintrano on 5/05/15.
 *
 * DAO for StreetTrack points
 */

public class StreetTrackDAO {

    private SQLiteDatabase db;
    private MySQLiteOpenHelper dbHelper;
    private String[] columns = {TableStreetTrack.COLUMN_ID,
            TableStreetTrack.COLUMN_ADDRESS, TableStreetTrack.COLUMN_START_LATITUDE,
            TableStreetTrack.COLUMN_START_LONGITUDE, TableStreetTrack.COLUMN_END_LATITUDE,
            TableStreetTrack.COLUMN_END_LONGITUDE, TableStreetTrack.COLUMN_START_DATETIME,
            TableStreetTrack.COLUMN_END_DATETIME, TableStreetTrack.COLUMN_DISTANCE};

    public StreetTrackDAO(Context context) {
        dbHelper = new MySQLiteOpenHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void create(StreetTrack streetTrack) {
        ContentValues values = new ContentValues();
        values.put(TableStreetTrack.COLUMN_ADDRESS, streetTrack.getAddress());
        values.put(TableStreetTrack.COLUMN_START_LATITUDE, streetTrack.getStartLatitude());
        values.put(TableStreetTrack.COLUMN_START_LONGITUDE, streetTrack.getStartLongitude());
        values.put(TableStreetTrack.COLUMN_END_LATITUDE, streetTrack.getEndLatitude());
        values.put(TableStreetTrack.COLUMN_END_LONGITUDE, streetTrack.getEndLongitude());
        values.put(TableStreetTrack.COLUMN_START_DATETIME, streetTrack.getStartDateTime());
        values.put(TableStreetTrack.COLUMN_END_DATETIME, streetTrack.getEndDateTime());
        values.put(TableStreetTrack.COLUMN_DISTANCE, streetTrack.getDistance());
        db.insert(TableStreetTrack.TABLE_NAME, null, values);
    }

    public List<StreetTrack> getAll() {
        List<StreetTrack> listStreetTrack = new ArrayList<>();

        Cursor cursor = db.query(TableStreetTrack.TABLE_NAME, columns, null, null,
                null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            StreetTrack streetTrack = cursorToStreetTrack(cursor);
            listStreetTrack.add(streetTrack);
            cursor.moveToNext();
        }

        cursor.close();
        return listStreetTrack;
    }

    public void delete(StreetTrack streetTrack) {
        long id = streetTrack.getId();
        db.delete(TableStreetTrack.TABLE_NAME, TableStreetTrack.COLUMN_ID + " = " + id, null);
    }

    public void deleteAll() {
        db.delete(TableStreetTrack.TABLE_NAME, null, null);
    }

    private StreetTrack cursorToStreetTrack(Cursor cursor) {
        StreetTrack streetTrack = new StreetTrack();
        streetTrack.setId(cursor.getLong(0));
        streetTrack.setAddress(cursor.getString(1));
        streetTrack.setStartLatitude(cursor.getDouble(1));
        streetTrack.setStartLongitude(cursor.getDouble(2));
        streetTrack.setEndLatitude(cursor.getDouble(3));
        streetTrack.setEndLongitude(cursor.getDouble(4));
        streetTrack.setStartDateTime(cursor.getString(5));
        streetTrack.setEndDateTime(cursor.getString(6));
        streetTrack.setDistance(cursor.getFloat(7));
        return streetTrack;
    }
}
