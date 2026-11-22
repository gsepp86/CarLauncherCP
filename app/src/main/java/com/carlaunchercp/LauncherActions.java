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
