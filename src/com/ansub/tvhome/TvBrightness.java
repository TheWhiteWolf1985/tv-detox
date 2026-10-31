package com.ansub.tvhome;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

/** SHARP picture control consumed by the TV's existing privileged settings service. */
final class TvBrightness {
    static final String KEY = "picture_brightness";
    private final Context context;

    static boolean required() {
        return "AQUOS_TVE19A".equals(Build.MODEL) && "SHARP".equalsIgnoreCase(Build.MANUFACTURER);
    }

    TvBrightness(Context context) { this.context = context; }

    int level() {
        return Settings.Global.getInt(context.getContentResolver(), KEY, -1);
    }

    boolean canWrite() {
        return context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    void set(int percent) {
        if (!canWrite()) throw new SecurityException("TV picture control needs WRITE_SECURE_SETTINGS");
        if (percent < 0 || percent > 100) throw new IllegalArgumentException("Brightness outside the TV range");
        // TVMenuSettingsService watches this exact Global URI, maps it to g_video__brightness,
        // and applies/saves it through its native API under the system service's own permissions.
        // No other picture, backlight, automatic-mode or system setting is changed here.
        if (!Settings.Global.putInt(context.getContentResolver(), KEY, percent))
            throw new IllegalStateException("TV picture setting was rejected");
        if (level() != percent) throw new IllegalStateException("TV picture setting did not update");
    }
}
