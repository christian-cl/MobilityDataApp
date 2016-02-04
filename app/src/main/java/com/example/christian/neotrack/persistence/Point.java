package com.example.christian.neotrack.persistence;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Christian Cintrano.
 */

public class Point implements Parcelable {
    private double latitude;
    private double longitude;
    private String address;

    public Point() {
    }

    public Point(double latitude, double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        if(address == null) {
            this.address = "";
        } else {
            this.address = address;
        }
    }

    public Point(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = "";
    }

    public Point(Parcel in) {
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.address = in.readString();
    }

    public Point(JSONObject obj) {
        try {
            this.latitude = obj.getDouble("latitude");
            this.longitude = obj.getDouble("longitude");
            this.address = obj.getString("address");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Point{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", address='" + address + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(address);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Point createFromParcel(Parcel in) {
            return new Point(in);
        }

        public Point[] newArray(int size) {
            return new Point[size];
        }
    };
}
