package org.spoofer.signalseeker.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

public class LocationService extends Service {
    private static final int updateInterval = 10000; // milliseconds of update period
    private static final int updateDistance = 3;    // meters distance before new request.

    private final IBinder binder = new LocalBinder();

    private final List<LocationServiceListener> listeners = new ArrayList<>();

    public LocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        try {
            startListening();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopListening();
        return super.onUnbind(intent);
    }

    public void removeLocationListener(LocationServiceListener l) {
        listeners.remove(l);
    }

    public void addLocationListener(LocationServiceListener l) {
        listeners.add(l);
        Location loc = getLastLocation();
        if (loc != null)
            l.LocationUpdate(loc);
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
        }
    }

    public interface LocationServiceListener {
        void LocationUpdate(Location location);
    }

    @SuppressLint("MissingPermission")
    public Location getLastLocation() {
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location l = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (l == null)
            l = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        return l;
    }

    private void startListening() throws SecurityException {
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("no permission to read location");
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateInterval, updateDistance, locationReceiver);
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, updateInterval, updateDistance, locationReceiver);
        }
    }

    private void stopListening() {
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationReceiver);
    }


    private LocationListener locationReceiver = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            updateListeners(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
    private void updateListeners(Location location) {
        for (LocationServiceListener l : listeners) {
            l.LocationUpdate(location);
        }
    }
}
