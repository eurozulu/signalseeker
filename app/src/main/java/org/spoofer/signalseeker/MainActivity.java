package org.spoofer.signalseeker;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.spoofer.signalseeker.location.CellLocationService;

public class MainActivity extends AppCompatActivity {
    private final Object lock = new Object();
    private CellLocationService locationSvc;
    private CellLocationService.CellLocationListener currentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        boolean hasPermiss = (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);

        if (!hasPermiss) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 123);
        } else {
            checkLocationEnabled();
            bindToLocationService();
        }

        // Monitor fragment switches to wire up supporting fragments to cellservice
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(fragLifecycleCallbacks, true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            int i = 0;
            for (; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    break;
                }
            }
            if (grantResults.length == 0 || i < grantResults.length) {
                Toast.makeText(getApplicationContext(), "No permission to continue. exiting", Toast.LENGTH_LONG).show();
                finish();
            }
            checkLocationEnabled();
            bindToLocationService();
        }
    }


    private void bindToLocationService() {
        Intent intent = new Intent(getApplicationContext(), CellLocationService.class);
        if (bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            Toast.makeText(getApplicationContext(), "Failed to connect to loction service!", Toast.LENGTH_LONG).show();
        }
    }

    private void checkLocationEnabled() {
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean netEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsEnabled && !netEnabled) {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
        } else if (!gpsEnabled) {
            Toast.makeText(getApplicationContext(),
                    "GPS is disabled.  enabling it will give more accurate results",
                    Toast.LENGTH_LONG).show();
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connectToService(((CellLocationService.LocalBinder) service).getService(), currentListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            disconnectFromService(locationSvc, currentListener);
        }
    };


    private final FragmentManager.FragmentLifecycleCallbacks fragLifecycleCallbacks =
            new FragmentManager.FragmentLifecycleCallbacks() {

                @Override
                public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
                    super.onFragmentStarted(fm, f);
                    if (!(f instanceof CellLocationService.CellLocationListener)) {
                        return;
                    }
                    connectToService(null, (CellLocationService.CellLocationListener) f);
                }

                @Override
                public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment f) {
                    super.onFragmentStopped(fm, f);
                    if (!(f instanceof CellLocationService.CellLocationListener)) {
                        return;
                    }
                    disconnectFromService(null, (CellLocationService.CellLocationListener) f);
                }
            };

    private void connectToService(CellLocationService svc, CellLocationService.CellLocationListener l) {
        synchronized (lock) {
            if (l != null)
                currentListener = l;
            if (svc != null)
                locationSvc = svc;

            if (locationSvc != null && currentListener != null)
                locationSvc.addCellLocationListener(currentListener);
        }
    }

    private void disconnectFromService(CellLocationService svc, CellLocationService.CellLocationListener l) {
        synchronized (lock) {
            if (locationSvc != null && l != null)
                locationSvc.removeCellLocationListener(l);

            if (currentListener == l)
                currentListener = null;

            if (svc == locationSvc)
                locationSvc = null;
        }
    }
}