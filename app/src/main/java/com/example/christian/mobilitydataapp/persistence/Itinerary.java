package com.example.christian.mobilitydataapp.persistence;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christian Cintrano.
 */
public class Itinerary implements Parcelable {
    private String name;
    private List points;

    public Itinerary(String name, List points) {
        this.name = name;
        this.points = points;
    }
    public Itinerary(Parcel in) {
        this.name = in.readString();
        this.points = new ArrayList<>();
        in.readList(points,LatLng.class.getClassLoader());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List getPoints() {
        return points;
    }

    public void setPoints(List points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return "Itinerary{" +
                "name='" + name + '\'' +
                ", points=" + points +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeList(points);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Itinerary createFromParcel(Parcel in) {
            return new Itinerary(in);
        }

        public Itinerary[] newArray(int size) {
            return new Itinerary[size];
        }
    };

    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            List<JSONObject> points = new ArrayList<>();
            for (Object p : points) {
                JSONObject jsonPoint = new JSONObject();
                jsonPoint.put("latitude", ((Point) p).getLatitude());
                jsonPoint.put("longitude", ((Point) p).getLongitude());
                jsonPoint.put("address", ((Point) p).getAddress());
                points.add(jsonPoint);
            }
            jsonObject.put("points", new JSONArray(points));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
