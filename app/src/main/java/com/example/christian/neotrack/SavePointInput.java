package com.example.christian.neotrack;

import android.location.Location;

import com.example.christian.neotrack.persistence.Itinerary;

/**
 * Created by CH on 14/02/2016.
 */
public class SavePointInput {

    private Itinerary itinerary;
    private Location location;

    public SavePointInput() {
    }

    public SavePointInput(Itinerary itinerary, Location location) {
        this.itinerary = itinerary;
        this.location = location;
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public void setItinerary(Itinerary itinerary) {
        this.itinerary = itinerary;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
