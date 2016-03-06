package com.example.christian.neotrack;

import android.location.Location;

import com.example.christian.neotrack.persistence.DataCapture;
import com.example.christian.neotrack.persistence.Itinerary;

/**
 * Created by CH on 14/02/2016.
 */
public class SavePointInput2 {

    private Itinerary itinerary;
    private DataCapture location;

    public SavePointInput2() {
    }

    public SavePointInput2(Itinerary itinerary, DataCapture location) {
        this.itinerary = itinerary;
        this.location = location;
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public void setItinerary(Itinerary itinerary) {
        this.itinerary = itinerary;
    }

    public DataCapture getLocation() {
        return location;
    }

    public void setLocation(DataCapture location) {
        this.location = location;
    }
}
