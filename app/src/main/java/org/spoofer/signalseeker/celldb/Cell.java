package org.spoofer.signalseeker.celldb;

import java.util.Date;

public class Cell {
    final String cellID;
    final String mobileCountryCode;
    final String mobileNetworkCode;
    final String locationAreaCode;
    final double latitude;
    final double longitude;
    final Date lastUpdate;
    final long distance;


    public Cell(String cellID, String mobileCountryCode, String mobileNetworkCode, String locationAreaCode, double latitude, double longitude, long distance, Date lastUpdate) {
        this.cellID = cellID;
        this.mobileCountryCode = mobileCountryCode;
        this.mobileNetworkCode = mobileNetworkCode;
        this.locationAreaCode = locationAreaCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.lastUpdate = lastUpdate;
    }

    public String getCellID() {
        return cellID;
    }

    public String getMobileCountryCode() {
        return mobileCountryCode;
    }

    public String getMobileNetworkCode() {
        return mobileNetworkCode;
    }

    public String getLocationAreaCode() {
        return locationAreaCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public long getDistance() {
        return distance;
    }
}

