package com.carlaunchercp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort bridge to the CAN data an aftermarket Android head unit already has.
 *
 * <p>Android itself has no public CAN API: every unit exposes its vehicle bus through vendor
 * broadcasts or system properties. This reader listens for the broadcasts the most common
 * Chinese platforms send (and reads a matching set of properties) and picks out odometer, trip
 * distance and fuel level. When the unit says nothing, nothing is reported and the launcher simply
 * hides the extra row: no fake numbers, and no crash on a radio without a CAN box.</p>
 */
public final class CanBusReader {

    /** A snapshot of the vehicle values that are actually available. Missing ones stay null. */
    public static final class Data {

        public final Float tripKm;
        public final Float totalKm;
        public final Float petrolPercent;
        public final Float petrolLiters;
        public final Float methanePercent;
        public final Float methaneLiters;

        public Data(Float tripKm, Float totalKm, Float petrolPercent, Float petrolLiters,
                    Float methanePercent, Float methaneLiters) {
            this.tripKm = tripKm;
            this.totalKm = totalKm;
            this.petrolPercent = petrolPercent;
            this.petrolLiters = petrolLiters;
            this.methanePercent = methanePercent;
            this.methaneLiters = methaneLiters;
        }

        public boolean isEmpty() {
            return tripKm == null && totalKm == null
                    && petrolPercent == null && petrolLiters == null
                    && methanePercent == null && methaneLiters == null;
        }

        boolean sameAs(Data other) {
            return other != null
                    && Objects.equals(tripKm, other.tripKm)
                    && Objects.equals(totalKm, other.totalKm)
                    && Objects.equals(petrolPercent, other.petrolPercent)
                    && Objects.equals(petrolLiters, other.petrolLiters)
                    && Objects.equals(methanePercent, other.methanePercent)
                    && Objects.equals(methaneLiters, other.methaneLiters);
        }
    }

    public interface Listener {
        void onCanData(Data data);
    }

    /** Actions used by the head-unit platforms that expose the bus this way. */
    private static final String[] ACTIONS = {
            "com.canbus.action.CANBUS_INFO",
            "com.canbus.CAN_INFO",
            "com.canbus.data",
            "com.syu.canbus.broadcast",
            "com.syu.canservice.CanbusData",
            "com.hiworld.canbus.DATA",
            "com.autochips.canbus",
            "com.mcu.canbus",
            "android.intent.action.CANBUS",
            "android.intent.action.CAN_DATA"
    };

    private static final String[] TOTAL_KM_KEYS = {
            "ro.can.odometer", "persist.can.odometer", "persist.sys.can.odometer",
            "ro.canbus.odometer", "persist.canbus.odometer", "persist.sys.odometer", "sys.odo"
    };
    private static final String[] TRIP_KM_KEYS = {
            "persist.can.trip", "persist.sys.can.trip", "persist.canbus.trip", "persist.sys.trip"
    };
    private static final String[] PETROL_KEYS = {
            "persist.can.fuel", "ro.can.fuel", "persist.sys.can.fuel",
            "persist.canbus.fuel", "persist.sys.fuel", "persist.can.petrol"
    };
    private static final String[] METHANE_KEYS = {
            "persist.can.methane", "persist.can.metano", "persist.sys.can.methane",
            "persist.canbus.gas", "ro.can.gas", "persist.sys.gas"
    };

    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?");
    private static final long POLL_MS = 5_000L;

    private final Context context;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Float tripKm;
    private Float totalKm;
    private Float petrolPercent;
    private Float petrolLiters;
    private Float methanePercent;
    private Float methaneLiters;

    private Data last;
    private boolean registered;
    private boolean polling;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent == null) return;
            Bundle extras = intent.getExtras();
            if (extras == null) return;
            for (String key : extras.keySet()) {
                apply(key, extras.get(key));
            }
            publish();
        }
    };

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            refreshFromProperties();
            publish();
            handler.postDelayed(this, POLL_MS);
        }
    };

    public CanBusReader(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void start() {
        if (!registered) {
            IntentFilter filter = new IntentFilter();
            for (String action : ACTIONS) filter.addAction(action);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
                } else {
                    context.registerReceiver(receiver, filter);
                }
                registered = true;
            } catch (Exception ignored) {
                // Unit without a CAN service: the extra row simply never appears.
            }
        }
        refreshFromProperties();
        publish();
        if (!polling) {
            polling = true;
            handler.postDelayed(poll, POLL_MS);
        }
    }

    public void stop() {
        handler.removeCallbacks(poll);
        polling = false;
        if (registered) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Exception ignored) {
            }
            registered = false;
        }
    }

    // ------------------------------------------------------------------ parsing

    private void apply(String key, Object value) {
        if (key == null || value == null) return;
        Float number = toNumber(value);
        if (number == null) return;
        String k = key.toLowerCase(Locale.ROOT);
        if (isMethane(k)) {
            setMethane(number);
        } else if (isPetrol(k)) {
            setPetrol(number);
        } else if (isTrip(k)) {
            if (number >= 0f) tripKm = number;
        } else if (isOdometer(k)) {
            if (number >= 0f) totalKm = number;
        }
    }

    private static boolean isMethane(String key) {
        return key.contains("methan") || key.contains("metano") || key.contains("cng");
    }

    private static boolean isPetrol(String key) {
        return key.contains("fuel") || key.contains("petrol") || key.contains("benzin")
                || key.contains("gasolin") || key.contains("carburante") || key.equals("gas");
    }

    private static boolean isTrip(String key) {
        return key.contains("trip") || key.contains("journey") || key.contains("parziale");
    }

    private static boolean isOdometer(String key) {
        return key.contains("odo") || key.contains("mileage") || key.contains("total_km")
                || key.contains("totalkm") || key.contains("km_total") || key.contains("km_totali")
                || key.contains("chilometr");
    }

    /** Values come as percent, litres or raw counters: up to 100 is read as a percentage. */
    private void setPetrol(float value) {
        if (value < 0f) return;
        if (value <= 100.5f) {
            petrolPercent = value;
            petrolLiters = null;
        } else if (value <= 1000f) {
            petrolLiters = value;
            petrolPercent = null;
        }
    }

    private void setMethane(float value) {
        if (value < 0f) return;
        if (value <= 100.5f) {
            methanePercent = value;
            methaneLiters = null;
        } else if (value <= 1000f) {
            methaneLiters = value;
            methanePercent = null;
        }
    }

    private static Float toNumber(Object value) {
        if (value instanceof Number) return ((Number) value).floatValue();
        if (value instanceof Boolean || value == null) return null;
        Matcher matcher = NUMBER.matcher(String.valueOf(value));
        if (!matcher.find()) return null;
        try {
            return Float.parseFloat(matcher.group().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void refreshFromProperties() {
        Float odometer = firstNumber(TOTAL_KM_KEYS);
        if (odometer != null) totalKm = odometer;
        Float trip = firstNumber(TRIP_KM_KEYS);
        if (trip != null) tripKm = trip;
        Float petrol = firstNumber(PETROL_KEYS);
        if (petrol != null) setPetrol(petrol);
        Float methane = firstNumber(METHANE_KEYS);
        if (methane != null) setMethane(methane);
    }

    private Float firstNumber(String[] keys) {
        for (String key : keys) {
            Float value = toNumber(property(key));
            if (value != null) return value;
        }
        return null;
    }

    /** Reads a system property; the class is hidden from the SDK, so it is reached by reflection. */
    private static String property(String key) {
        try {
            Class<?> properties = Class.forName("android.os.SystemProperties");
            Method get = properties.getMethod("get", String.class);
            Object value = get.invoke(null, key);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void publish() {
        Data data = new Data(tripKm, totalKm, petrolPercent, petrolLiters,
                methanePercent, methaneLiters);
        if (last != null && last.sameAs(data)) return;
        last = data;
        if (listener != null) listener.onCanData(data);
    }
}
