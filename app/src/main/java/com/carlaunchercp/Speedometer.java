package com.carlaunchercp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

/** GPS speed source for the speed card. */
public final class Speedometer implements LocationListener {

    public interface Listener {
        void onSpeed(float kmh, boolean hasFix);

        void onGpsPermissionMissing();
    }

    private final Activity activity;
    private final Listener listener;
    private LocationManager manager;
    private Location previous;
    private boolean running;
    private boolean permissionRequested;

    public Speedometer(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void start() {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            listener.onGpsPermissionMissing();
            if (!permissionRequested) {
                permissionRequested = true;
                activity.requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 41);
            }
            return;
        }
        if (running) return;
        manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            listener.onGpsPermissionMissing();
            return;
        }
        try {
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                listener.onSpeed(0f, false);
            }
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 900L, 0.4f, this,
                    Looper.getMainLooper());
            running = true;
        } catch (SecurityException e) {
            listener.onGpsPermissionMissing();
        } catch (Exception e) {
            listener.onSpeed(0f, false);
        }
    }

    public void stop() {
        if (manager != null && running) {
            try {
                manager.removeUpdates(this);
            } catch (Exception ignored) {
            }
        }
        running = false;
    }

    @Override
    public void onLocationChanged(Location location) {
        float kmh;
        if (location.hasSpeed()) {
            kmh = location.getSpeed() * 3.6f;
        } else if (previous != null) {
            float meters = location.distanceTo(previous);
            long millis = Math.max(1L, location.getTime() - previous.getTime());
            kmh = meters / (millis / 1000f) * 3.6f;
        } else {
            kmh = 0f;
        }
        if (kmh < 1.2f) kmh = 0f;
        previous = location;
        listener.onSpeed(kmh, true);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // not used
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        listener.onSpeed(0f, false);
    }
}
