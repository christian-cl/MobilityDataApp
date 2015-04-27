/**
 * Christian Cintrano on 27/04/15.
 *
 * DataBase controller
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
        super(context, "mobilityDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE points ("+
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                "latitude DOUBLE, longitude DOUBLE, date DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // En caso de una nueva versión habría que actualizar las tablas
    }


    public void savePoints(double latitude, double longitude) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO points VALUES ( null, "+
                latitude+", "+longitude+")");
        db.close();
    }

    public void resetTablePoints() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + "points");
        db.execSQL("CREATE TABLE points ("+
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                "latitude DOUBLE, longitude DOUBLE, date DATETIME DEFAULT CURRENT_TIMESTAMP)");
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

    public Vector listAllPoints() {
        Vector result = new Vector();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " +
                "points ORDER BY latitude DESC ", null);
        result.add(cursor.getColumnNames());
        while (cursor.moveToNext()){
            result.add(cursor.getInt(0)+" " +cursor.getString(1)+" " +cursor.getString(2)+" " +cursor.getString(3));
        }
        cursor.close();
        db.close();
        return result;
    }
}
