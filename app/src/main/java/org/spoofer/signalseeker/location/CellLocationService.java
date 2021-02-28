package org.spoofer.signalseeker.location;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import org.spoofer.signalseeker.DownloadActivity;
import org.spoofer.signalseeker.celldb.Cell;
import org.spoofer.signalseeker.celldb.CellDatabase;
import org.spoofer.signalseeker.celldb.CellDatabaseLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CellLocationService extends Service {
    private final Handler guiHandler = new Handler(Looper.getMainLooper());

    private static final int updateInterval = 10000; // milliseconds of update period
    private static final int updateDistance = 3;    // meters distance before new request.

    private final IBinder binder = new LocalBinder();

    private final List<CellLocationListener> cellListeners = new ArrayList<>();

    private final Object lock = new Object();
    private CellDatabase cellDatabase;
    private Location lastLocation;


    public interface CellLocationListener {
        void LocationUpdate(Location location);

        void LocalCellsUpdate(List<Cell> cells);
    }


    public CellLocationService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
        fetchDatabase(getCountryCode());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        if (!intent.hasExtra("countryCode")) {
            return Service.STOP_FOREGROUND_DETACH;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        try {
            startLocationListening();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopLocationListening();
        closeDatabase();
        return super.onUnbind(intent);
    }

    public void removeCellLocationListener(CellLocationListener l) {
        cellListeners.remove(l);
    }

    public void addCellLocationListener(CellLocationListener l) {
        cellListeners.add(l);
    }

    private void startLocationListening() throws SecurityException {
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

    private void stopLocationListening() {
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationReceiver);
    }

    private void fetchDatabase(String countryCode) {
        CellDatabaseLoader dbl = new CellDatabaseLoader(getApplicationContext());
        if (dbl.hasDatabase(countryCode)) {
            synchronized (lock) {
                try {
                    cellDatabase = dbl.getDatabase(countryCode);
                    updateListeners(lastLocation);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return;
        }

        Intent intent = new Intent(getApplicationContext(), CellLocationService.class);
        intent.putExtra("countryCode", countryCode);
        startService(intent);
    }

    private void closeDatabase() {
        synchronized (lock) {
            if (cellDatabase == null) {
                return;
            }
            cellDatabase.close();
            cellDatabase = null;
        }
    }


    private void updateListeners(Location location) {
        if (location == null)
            return;

        float distanceDelta = lastLocation == null ? 1 : 0; // positive value in case lastLocation is null.

        List<Cell> cells = null;

        synchronized (lock) {
            if (lastLocation != null) {
                distanceDelta = lastLocation.distanceTo(location);
            }
            lastLocation = location;

            if (cellDatabase != null) {
                cells = cellDatabase.findLocalCells(location.getLatitude(), location.getLongitude());
            }
        }

        if (distanceDelta > 0) {
            sendLocationUpdate(location);
        }

        if (cells != null) {
            sendCellUpdate(cells);
        }
    }

    private void sendLocationUpdate(final Location location) {
        guiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (CellLocationListener l : cellListeners) {
                    l.LocationUpdate(location);
                }
            }
        });
    }

    private void sendCellUpdate(final List<Cell> cells) {
        guiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (CellLocationListener l : cellListeners) {
                    l.LocalCellsUpdate(cells);
                }
            }
        });
    }

    private String getCountryCode() {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getNetworkCountryIso();
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

    public class LocalBinder extends Binder {
        public CellLocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CellLocationService.this;
        }
    }

}
