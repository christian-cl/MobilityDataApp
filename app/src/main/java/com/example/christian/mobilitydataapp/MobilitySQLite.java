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

    public enum query {
        CREATE_TABLE_POINTS("CREATE TABLE points (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " latitude DOUBLE, longitude DOUBLE, address STRING, " +
                "date DATETIME DEFAULT CURRENT_TIMESTAMP)"),
        DROP_TABLE_POINTS("DROP TABLE IF EXISTS points");
//        INSERT_POINTS("INSERT INTO points VALUES ( null, %s, %s, %s)",latitude,longitude,address);

        private final String name;

        private query(String s) {
            name = s;
        }

        public String toString(){
            return name;
        }

    }

    //Métodos de SQLiteOpenHelper
    public MobilitySQLite(Context context) {
        super(context, "mobilityDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(query.CREATE_TABLE_POINTS.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // En caso de una nueva versión habría que actualizar las tablas
    }


    public void savePoints(double latitude, double longitude, String address) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO points (latitude, longitude, address) VALUES ("+
                latitude+", "+longitude+", '" + address + "')");
        db.close();
    }

    public void resetTablePoints() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(query.DROP_TABLE_POINTS.toString());
        db.execSQL(query.CREATE_TABLE_POINTS.toString());
        db.close();
    }

    public Vector listAllPoints() {
        Vector result = new Vector();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " +
                "points ORDER BY latitude DESC ", null);
        while (cursor.moveToNext()){
            String row = cursor.getInt(0)+" " +cursor.getString(1)+" " +
                    cursor.getString(2)+" " +cursor.getString(3)+" " +
                    cursor.getString(4);
            result.add(row);
        }
        cursor.close();
        db.close();
        return result;
    }
}
