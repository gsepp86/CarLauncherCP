package com.carlaunchercp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import java.util.List;

/** Resolves and launches the radio, music and phone experiences. */
public final class LauncherActions {

    /** Packages commonly used by Android head units for the FM tuner. */
    private static final String[] RADIO_PACKAGES = {
            "com.android.fmradio",
            "com.car.fmradio",
            "com.nwd.radio",
            "com.syu.radio",
            "com.dofun.radio",
            "com.tw.radio",
            "com.mediatek.fmradio",
            "com.motorola.fmplayer",
            "com.huawei.android.FMRadio",
            "com.unihertz.radio",
            "com.ts.radio",
            "com.autochips.radio",
            "com.zqc.radio"
    };

    /** Packages commonly used as turn-by-turn navigation on Android head units. */
    private static final String[] NAV_PACKAGES = {
            "com.google.android.apps.maps",
            "com.waze",
            "com.here.app.maps",
            "com.tomtom.gplay.navapp",
            "com.tomtom.navapp",
            "com.sygic.aura",
            "net.osmand.plus",
            "com.mapfactor.navigator",
            "ru.yandex.yandexnavi",
            "com.navngo.igo.javaclient",
            "com.navigon.navigator_select",
            "com.huawei.maps.app",
            "com.baidu.navi",
            "com.autonavi.amapauto"
    };

    private static final String[] MUSIC_PACKAGES = {
            "com.spotify.music",
            "com.google.android.music",
            "com.android.music",
            "com.syu.music",
            "com.nwd.music",
            "com.dofun.music"
    };

    private LauncherActions() {
    }

    public static boolean launchComponent(Context context, String flattened) {
        ComponentName component = ComponentName.unflattenFromString(flattened == null ? "" : flattened);
        if (component == null) return false;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(component);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Saved radio app first, then known head-unit packages. Remembers what worked. */
    public static boolean openRadio(Context context, Prefs prefs) {
        String saved = prefs.radioTarget();
        if (saved != null && launchComponent(context, saved)) return true;

        String found = findFirstInstalled(context, RADIO_PACKAGES);
        if (found != null) {
            prefs.setRadioTarget(found);
            return launchComponent(context, found);
        }
        return false;
    }

    /** Saved music app first, then the system music app, then common players. */
    public static boolean openMusic(Context context, Prefs prefs) {
        String saved = prefs.musicTarget();
        if (saved != null && launchComponent(context, saved)) return true;

        Intent music = new Intent(Intent.ACTION_MAIN);
        music.addCategory(Intent.CATEGORY_APP_MUSIC);
        music.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (context.getPackageManager().resolveActivity(music, 0) != null) {
            try {
                context.startActivity(music);
                return true;
            } catch (Exception ignored) {
            }
        }
        String found = findFirstInstalled(context, MUSIC_PACKAGES);
        if (found != null) {
            prefs.setMusicTarget(found);
            return launchComponent(context, found);
        }
        return false;
    }

    /**
     * Saved navigator first, then a known navigation package, then whatever the system
     * advertises for {@code CATEGORY_APP_MAPS}. Remembers the app that was found.
     */
    public static boolean openNavigation(Context context, Prefs prefs) {
        String saved = prefs.navTarget();
        if (saved != null && launchComponent(context, saved)) return true;

        String found = findFirstInstalled(context, NAV_PACKAGES);
        if (found != null) {
            prefs.setNavTarget(found);
            return launchComponent(context, found);
        }

        Intent maps = new Intent(Intent.ACTION_MAIN);
        maps.addCategory(Intent.CATEGORY_APP_MAPS);
        maps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            ResolveInfo info = context.getPackageManager().resolveActivity(maps, 0);
            if (info != null && info.activityInfo != null) {
                prefs.setNavTarget(info.activityInfo.packageName + "/" + info.activityInfo.name);
                context.startActivity(maps);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean openDialer(Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_DIAL)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String appLabel(Context context, String flattened) {
        ComponentName component = ComponentName.unflattenFromString(flattened == null ? "" : flattened);
        if (component == null) return "";
        try {
            return context.getPackageManager()
                    .getActivityInfo(component, 0)
                    .loadLabel(context.getPackageManager()).toString();
        } catch (Exception e) {
            return component.getPackageName();
        }
    }

    private static String findFirstInstalled(Context context, String[] packages) {
        Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launchers = context.getPackageManager().queryIntentActivities(probe, 0);
        for (String pkg : packages) {
            for (ResolveInfo info : launchers) {
                if (info.activityInfo != null && pkg.equals(info.activityInfo.packageName)) {
                    return pkg + "/" + info.activityInfo.name;
                }
            }
        }
        return null;
    }
}
