package com.example.christian.mobilitydataapp.persistence;

import com.example.christian.mobilitydataapp.persistence.MySQLiteOpenHelper.TableDataCapture;
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
    private String[] columns = {TableDataCapture.COLUMN_ID,
            TableDataCapture.COLUMN_LATITUDE, TableDataCapture.COLUMN_LONGITUDE,
            TableDataCapture.COLUMN_ADDRESS, TableDataCapture.COLUMN_STOP_TYPE,
            TableDataCapture.COLUMN_COMMENT, TableDataCapture.COLUMN_DATE};

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
        values.put(TableDataCapture.COLUMN_LATITUDE, dataCapture.getLatitude());
        values.put(TableDataCapture.COLUMN_LONGITUDE, dataCapture.getLongitude());
        if(dataCapture.getAddress() != null)
            values.put(TableDataCapture.COLUMN_ADDRESS, dataCapture.getAddress());
        if(dataCapture.getStopType() != null)
            values.put(TableDataCapture.COLUMN_STOP_TYPE, dataCapture.getStopType());
        if(dataCapture.getComment() != null)
            values.put(TableDataCapture.COLUMN_COMMENT, dataCapture.getComment());
        if(dataCapture.getDate() != null)
            values.put(TableDataCapture.COLUMN_DATE, dataCapture.getDate());
        db.insert(TableDataCapture.TABLE_NAME, null, values);
    }

    public List<DataCapture> getAll() {
        List<DataCapture> listDataCapture = new ArrayList<>();

        Cursor cursor = db.query(TableDataCapture.TABLE_NAME, columns, null, null,
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
        String where = TableDataCapture.COLUMN_DATE + ">=? and " + TableDataCapture.COLUMN_DATE + "<=?";
        Cursor cursor = db.query(TableDataCapture.TABLE_NAME, columns, where,arg, null, null, null, null);

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
        db.delete(TableDataCapture.TABLE_NAME, TableDataCapture.COLUMN_ID + " = " + id, null);
    }

    /**
     * Delete all rows between two dates
     * @param dateStart initial date
     * @param dateEnd finish date
     */
    public void delete(String dateStart, String dateEnd) {
        String where =  TableDataCapture.COLUMN_DATE + " <= \"" + dateEnd +
                "\" and " + TableDataCapture.COLUMN_DATE + " >= \"" + dateStart + "\"";
        db.delete(TableDataCapture.TABLE_NAME,where , null);
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
        db.delete(TableDataCapture.TABLE_NAME, null, null);
    }

    private DataCapture cursorToDataCapture(Cursor cursor) {
        DataCapture dataCapture = new DataCapture();
        dataCapture.setId(cursor.getLong(0));
        dataCapture.setLatitude(cursor.getDouble(1));
        dataCapture.setLongitude(cursor.getDouble(2));
        dataCapture.setAddress(cursor.getString(3));
        dataCapture.setStopType(cursor.getString(4));
        dataCapture.setComment(cursor.getString(5));
        dataCapture.setDate(cursor.getString(6));
        return dataCapture;
    }
}
