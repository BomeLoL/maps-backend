package com.maps.backend.model;

public class BusPosition {

    private int busId;
    private double lat;
    private double lon;

    private int citId;
    private String citName;

    public BusPosition() {
    }

    // ✅ Constructor Actualizado
    public BusPosition(int busId, double lat, double lon, int citId, String citName) {
        this.busId = busId;
        this.lat = lat;
        this.lon = lon;
        this.citId = citId;
        this.citName = citName;
    }

    // Getters y Setters
    public int getBusId() { return busId; }
    public void setBusId(int busId) { this.busId = busId; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    // 🆕 Getters y Setters Nuevos
    public int getCitId() { return citId; }
    public void setCitId(int citId) { this.citId = citId; }

    public String getCitName() { return citName; }
    public void setCitName(String citName) { this.citName = citName; }
}