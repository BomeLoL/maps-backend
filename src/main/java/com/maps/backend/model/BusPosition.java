package com.maps.backend.model;

public class BusPosition {

    private int busId;
    private double lat;
    private double lon;

    // ✅ Constructor vacío necesario para Jackson
    public BusPosition() {
    }

    // ✅ Constructor que usamos en MainApplication
    public BusPosition(int busId, double lat, double lon) {
        this.busId = busId;
        this.lat = lat;
        this.lon = lon;
    }

    // ✅ Getters y Setters
    public int getBusId() {
        return busId;
    }

    public void setBusId(int busId) {
        this.busId = busId;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
