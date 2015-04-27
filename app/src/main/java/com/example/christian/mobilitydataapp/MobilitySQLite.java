/**
 * Created by Christian Cintrano on 27/04/15.
 */

package com.example.christian.mobilitydataapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Vector;

//Métodos de SQLiteOpenHelper
public class MobilitySQLite extends SQLiteOpenHelper {

    //Métodos de SQLiteOpenHelper
    public MobilitySQLite(Context context) {
        super(context, "puntuaciones", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE points ("+
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                "latitude DOUBLE, longitude DOUBLE, date LONG)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // En caso de una nueva versión habría que actualizar las tablas
    }


    public void savePoints(double latitude, double longitude, long date) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO points VALUES ( null, "+
                latitude+", '"+longitude+"', "+date+")");
        db.close();
    }

    public Vector listPoints(int maxElem) {
        Vector result = new Vector();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT latitude, longitude FROM " +
                "points ORDER BY latitude DESC LIMIT " +maxElem, null);
        while (cursor.moveToNext()){
            result.add(cursor.getInt(0)+" " +cursor.getString(1));
        }
        cursor.close();
        db.close();
        return result;
    }
}
