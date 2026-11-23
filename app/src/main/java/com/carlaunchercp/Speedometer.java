package com.carlaunchercp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * GPS speed source for the speed card.
 *
 * <p>Raw GPS speed jitters by a km/h or two even when the car is parked, which used to make the
 * gauge read "1" and then fall back to "0". The readings are therefore filtered with a dead band
 * plus hysteresis: below {@link #STOPPED_KMH} the car counts as stopped and the speed is exactly
 * zero, and it only starts moving again above {@link #MOVING_KMH}. A stale fix is dropped instead
 * of freezing the last value on screen.</p>
 */
public final class Speedometer implements LocationListener {

    public interface Listener {
        void onSpeed(float kmh, boolean hasFix);

        void onGpsPermissionMissing();
    }

    /** Parked: up to 3 km/h is GPS drift, so it is reported as 0 km/h. */
    private static final float STOPPED_KMH = 3f;
    /** Moving again: the bit of hysteresis that keeps the reading from flickering at a stop. */
    private static final float MOVING_KMH = 4f;
    /** A car that produces no fix for this long is not moving as far as the gauge knows. */
    private static final long FIX_TIMEOUT_MS = 12_000L;

    private final Activity activity;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private LocationManager manager;
    private Location previous;
    private boolean running;
    private boolean permissionRequested;
    private boolean moving;
    private float smoothed;

    private final Runnable fixTimeout = new Runnable() {
        @Override
        public void run() {
            previous = null;
            moving = false;
            smoothed = 0f;
            listener.onSpeed(0f, true);
        }
    };

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
            previous = null;
            moving = false;
            smoothed = 0f;
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                listener.onSpeed(0f, false);
            }
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 900L, 0f, this,
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
        handler.removeCallbacks(fixTimeout);
        running = false;
        previous = null;
        moving = false;
        smoothed = 0f;
    }

    @Override
    public void onLocationChanged(Location location) {
        float raw = rawSpeed(location);
        previous = location;

        if (!moving) {
            if (raw >= MOVING_KMH) {
                moving = true;
            } else {
                raw = 0f;
            }
        } else if (raw < STOPPED_KMH) {
            moving = false;
            raw = 0f;
        }

        if (raw <= 0f) {
            // Standstill: snap to zero so the gauge never lingers on "1 km/h".
            smoothed = 0f;
        } else {
            float factor = raw > smoothed ? 0.5f : 0.3f;
            smoothed += (raw - smoothed) * factor;
        }

        handler.removeCallbacks(fixTimeout);
        handler.postDelayed(fixTimeout, FIX_TIMEOUT_MS);
        listener.onSpeed(smoothed, true);
    }

    private float rawSpeed(Location location) {
        if (location.hasSpeed()) {
            return Math.max(0f, location.getSpeed() * 3.6f);
        }
        if (previous != null) {
            float meters = location.distanceTo(previous);
            long millis = Math.max(200L, location.getTime() - previous.getTime());
            return Math.max(0f, meters / (millis / 1000f) * 3.6f);
        }
        return 0f;
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
        handler.removeCallbacks(fixTimeout);
        previous = null;
        moving = false;
        smoothed = 0f;
        listener.onSpeed(0f, false);
    }
}
