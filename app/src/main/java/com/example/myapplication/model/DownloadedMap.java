package com.example.myapplication.model;

import java.util.Date;

/**
 * Data model representing a downloaded/cached map with user-defined label
 */
public class DownloadedMap {
    private long id;
    private String label;
    private String description;
    private double centerLatitude;
    private double centerLongitude;
    private double northEastLat;
    private double northEastLng;
    private double southWestLat;
    private double southWestLng;
    private int zoomLevel;
    private Date downloadDate;
    private long fileSizeBytes;
    private String mapType; // "NORMAL", "SATELLITE", "HYBRID", "TERRAIN"
    private boolean isAvailableOffline;

    // Default constructor
    public DownloadedMap() {
        this.downloadDate = new Date();
        this.isAvailableOffline = true;
    }

    // Constructor with essential parameters
    public DownloadedMap(String label, double centerLatitude, double centerLongitude, 
                        double northEastLat, double northEastLng, double southWestLat, 
                        double southWestLng, int zoomLevel, String mapType) {
        this();
        this.label = label;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.northEastLat = northEastLat;
        this.northEastLng = northEastLng;
        this.southWestLat = southWestLat;
        this.southWestLng = southWestLng;
        this.zoomLevel = zoomLevel;
        this.mapType = mapType;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public double getNorthEastLat() {
        return northEastLat;
    }

    public void setNorthEastLat(double northEastLat) {
        this.northEastLat = northEastLat;
    }

    public double getNorthEastLng() {
        return northEastLng;
    }

    public void setNorthEastLng(double northEastLng) {
        this.northEastLng = northEastLng;
    }

    public double getSouthWestLat() {
        return southWestLat;
    }

    public void setSouthWestLat(double southWestLat) {
        this.southWestLat = southWestLat;
    }

    public double getSouthWestLng() {
        return southWestLng;
    }

    public void setSouthWestLng(double southWestLng) {
        this.southWestLng = southWestLng;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public Date getDownloadDate() {
        return downloadDate;
    }

    public void setDownloadDate(Date downloadDate) {
        this.downloadDate = downloadDate;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getMapType() {
        return mapType;
    }

    public void setMapType(String mapType) {
        this.mapType = mapType;
    }

    public boolean isAvailableOffline() {
        return isAvailableOffline;
    }

    public void setAvailableOffline(boolean availableOffline) {
        isAvailableOffline = availableOffline;
    }

    // Utility methods
    public String getFormattedSize() {
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
        }
    }

    public String getBoundsDescription() {
        return String.format("Center: %.4f, %.4f", centerLatitude, centerLongitude);
    }

    @Override
    public String toString() {
        return "DownloadedMap{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", centerLat=" + centerLatitude +
                ", centerLng=" + centerLongitude +
                ", mapType='" + mapType + '\'' +
                ", downloadDate=" + downloadDate +
                '}';
    }
}
