package com.example.christian.neotrack.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Christian Cintrano on 5/05/15.
 *
 * DAO for DataCapture points
 */

public class DataCaptureDAO {

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss",Locale.US);

    private SQLiteDatabase db;
    private MySQLiteOpenHelper dbHelper;
    private String[] columns = {MySQLiteOpenHelper.TableDataCapture.COLUMN_ID,
            MySQLiteOpenHelper.TableDataCapture.COLUMN_SESSION, MySQLiteOpenHelper.TableDataCapture.COLUMN_LATITUDE,
            MySQLiteOpenHelper.TableDataCapture.COLUMN_LONGITUDE, MySQLiteOpenHelper.TableDataCapture.COLUMN_STOP_TYPE,
            MySQLiteOpenHelper.TableDataCapture.COLUMN_COMMENT, MySQLiteOpenHelper.TableDataCapture.COLUMN_DATE,
            MySQLiteOpenHelper.TableDataCapture.COLUMN_SENSOR_ACCELERATION, MySQLiteOpenHelper.TableDataCapture.COLUMN_SENSOR_PRESSURE,
            MySQLiteOpenHelper.TableDataCapture.COLUMN_SENSOR_LIGHT, MySQLiteOpenHelper.TableDataCapture.COLUMN_SENSOR_TEMPERATURE,
            MySQLiteOpenHelper.TableDataCapture.COLUMN_SENSOR_HUMIDITY};

    public DataCaptureDAO(Context context) {
        dbHelper = new MySQLiteOpenHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void create(DataCapture dataCapture) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_LATITUDE, dataCapture.getLatitude());
        values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_LONGITUDE, dataCapture.getLongitude());
        if(dataCapture.getSession() != null)
            values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_SESSION, dataCapture.getSession());
        if(dataCapture.getStopType() != null)
            values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_STOP_TYPE, dataCapture.getStopType());
        if(dataCapture.getComment() != null)
            values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_COMMENT, dataCapture.getComment());
        if(dataCapture.getDate() != null)
            values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_DATE, dataCapture.getDate());
        values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_SENSOR_ACCELERATION, dataCapture.getSensorAcceleration());
        values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_SENSOR_PRESSURE, dataCapture.getSensorPressure());
        values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_SENSOR_LIGHT, dataCapture.getSensorLight());
        values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_SENSOR_TEMPERATURE, dataCapture.getSensorTemperature());
        values.put(MySQLiteOpenHelper.TableDataCapture.COLUMN_SENSOR_HUMIDITY, dataCapture.getSensorHumidity());
        db.insert(MySQLiteOpenHelper.TableDataCapture.TABLE_NAME, null, values);
    }

    public List<DataCapture> getAll() {
        List<DataCapture> listDataCapture = new ArrayList<>();

        Cursor cursor = db.query(MySQLiteOpenHelper.TableDataCapture.TABLE_NAME, columns, null, null,
                null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            DataCapture dataCapture = cursorToDataCapture(cursor);
            listDataCapture.add(dataCapture);
            cursor.moveToNext();
        }

        cursor.close();
        return listDataCapture;
    }


    /**
     * List of elements between two dates
     *
     * @param dateStart initial date
     * @param dateEnd finish date
     * @return list of results
     */
    public List<DataCapture> get(Calendar dateStart, Calendar dateEnd) {
        List<DataCapture> listDataCapture = new ArrayList<>();

        String[] arg = new String[] { dateFormatter.format(dateStart.getTime()),
                dateFormatter.format(dateEnd.getTime())};
        String where = MySQLiteOpenHelper.TableDataCapture.COLUMN_DATE + ">=? and " + MySQLiteOpenHelper.TableDataCapture.COLUMN_DATE + "<=?";
        Cursor cursor = db.query(MySQLiteOpenHelper.TableDataCapture.TABLE_NAME, columns, where,arg, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            DataCapture dataCapture = cursorToDataCapture(cursor);
            listDataCapture.add(dataCapture);
            cursor.moveToNext();
        }

        cursor.close();
        return listDataCapture;
    }
    /**
     * List of elements of a specific session
     *
     * @param sessionId sessionId
     * @return list of results
     */
    public List<DataCapture> get(String sessionId) {
        List<DataCapture> listDataCapture = new ArrayList<>();

        String[] arg = new String[] {sessionId};
        String where = MySQLiteOpenHelper.TableDataCapture.COLUMN_SESSION + "=?";
        Cursor cursor = db.query(MySQLiteOpenHelper.TableDataCapture.TABLE_NAME, columns, where,arg, null, null, MySQLiteOpenHelper.TableDataCapture.COLUMN_DATE + " ASC", null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            DataCapture dataCapture = cursorToDataCapture(cursor);
            listDataCapture.add(dataCapture);
            cursor.moveToNext();
        }

        cursor.close();
        return listDataCapture;
    }

    public void delete(DataCapture dataCapture) {
        long id = dataCapture.getId();
        db.delete(MySQLiteOpenHelper.TableDataCapture.TABLE_NAME, MySQLiteOpenHelper.TableDataCapture.COLUMN_ID + " = " + id, null);
    }

    /**
     * Delete all rows between two dates
     * @param dateStart initial date
     * @param dateEnd finish date
     */
    public void delete(String dateStart, String dateEnd) {
        String where =  MySQLiteOpenHelper.TableDataCapture.COLUMN_DATE + " <= \"" + dateEnd +
                "\" and " + MySQLiteOpenHelper.TableDataCapture.COLUMN_DATE + " >= \"" + dateStart + "\"";
        db.delete(MySQLiteOpenHelper.TableDataCapture.TABLE_NAME,where , null);
    }

    /**
     * Delete all rows between two dates
     * @param dateStart initial date
     * @param dateEnd finish date
     */
    public void delete(Calendar dateStart, Calendar dateEnd) {
        String FORMAT_DATE = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        delete(sdf.format(dateStart.getTime()),sdf.format(dateEnd.getTime()));
    }

    public void deleteAll() {
        db.delete(MySQLiteOpenHelper.TableDataCapture.TABLE_NAME, null, null);
    }

    private DataCapture cursorToDataCapture(Cursor cursor) {
        DataCapture dataCapture = new DataCapture();
        dataCapture.setId(cursor.getLong(0));
        dataCapture.setSession(cursor.getString(1));
        dataCapture.setLatitude(cursor.getDouble(2));
        dataCapture.setLongitude(cursor.getDouble(3));
        dataCapture.setStopType(cursor.getString(4));
        dataCapture.setComment(cursor.getString(5));
        dataCapture.setDate(cursor.getString(6));
        dataCapture.setSensorAcceleration(cursor.getDouble(7));
        dataCapture.setSensorPressure(cursor.getDouble(8));
        dataCapture.setSensorLight(cursor.getDouble(9));
        dataCapture.setSensorTemperature(cursor.getDouble(10));
        dataCapture.setSensorHumidity(cursor.getDouble(11));
        return dataCapture;
    }
}
