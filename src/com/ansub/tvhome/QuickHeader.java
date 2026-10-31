package com.ansub.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Fixed status bar and remote-operated quick settings, outside the home page's ScrollView. */
final class QuickHeader extends LinearLayout {
    static final int HEIGHT = 80; // design pixels at 1920px, like MainActivity
    private final Activity activity;
    private final float scale;
    private final AudioManager audio;
    private final ConnectivityManager network;
    private final Runnable store, settings, picture;
    private final String clockPreference;
    private final TvBrightness tvBrightness;
    private final TextView time, date;
    private final ImageView connection, speaker;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean started, observingNetwork, syncing;
    private Dialog shade;
    private SeekBar volume, brightness;
    private TextView volumeValue, brightnessValue, mute;
    private int networkState = -1, volumeState = -1;
    private int audibleVolume = 1;

    QuickHeader(Activity activity, String clockPreference, Runnable store, Runnable settings, Runnable picture) {
        super(activity);
        this.activity = activity;
        this.clockPreference = clockPreference;
        this.store = store;
        this.settings = settings;
        this.picture = picture;
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        network = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        tvBrightness = TvBrightness.required() ? new TvBrightness(activity) : null;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(px(64), 0, px(64), 0);
        setFocusable(true);
        setClickable(true);
        setId(View.generateViewId());
        setBackgroundColor(0xF5161D29);
        setOnFocusChangeListener((v, focused) -> setBackgroundColor(focused ? 0xFF30465E : 0xF5161D29));
        setOnClickListener(v -> open());

        time = text(30, Color.WHITE);
        time.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        addView(time, new LayoutParams(-2, -2));
        date = text(25, 0xD9FFFFFF);
        LayoutParams dp = new LayoutParams(0, -2, 1);
        dp.leftMargin = px(28);
        addView(date, dp);
        connection = icon(LineIcon.ETHERNET);
        speaker = icon(LineIcon.SOUND);
        ImageView arrow = icon(LineIcon.CHEVRON_DOWN);
        arrow.setAlpha(0.65f);
        refresh();
    }

    private String label(String english, String italian) {
        return "it".equals(Locale.getDefault().getLanguage()) ? italian : english;
    }

    private int px(int value) { return Math.round(scale * value); }

    private TextView text(int size, int color) {
        TextView view = new TextView(activity);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        view.setTextColor(color);
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private ImageView icon(int[] shape) {
        ImageView view = new ImageView(activity);
        view.setImageDrawable(new LineIcon(shape));
        LayoutParams lp = new LayoutParams(px(34), px(34));
        lp.leftMargin = px(30);
        addView(view, lp);
        return view;
    }

    private final BroadcastReceiver changed = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) { refresh(); }
    };
    private final ContentObserver brightnessChanged = new ContentObserver(handler) {
        @Override public void onChange(boolean self) { refreshControls(); }
    };
    private final ConnectivityManager.NetworkCallback networkChanged = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network n) { handler.post(QuickHeader.this::refresh); }
        @Override public void onLost(Network n) { handler.post(QuickHeader.this::refresh); }
        @Override public void onCapabilitiesChanged(Network n, NetworkCapabilities c) { handler.post(QuickHeader.this::refresh); }
    };

    void start() {
        if (started) return;
        started = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
        filter.addAction("com.android.tv.settings.partnercustomizer.tvsettingservice.notify_back_light");
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(changed, filter, Context.RECEIVER_NOT_EXPORTED);
        else activity.registerReceiver(changed, filter);
        activity.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, brightnessChanged);
        if (tvBrightness != null) activity.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(TvBrightness.KEY), false, brightnessChanged);
        if (network != null) try {
            if (Build.VERSION.SDK_INT >= 24) network.registerDefaultNetworkCallback(networkChanged);
            else network.registerNetworkCallback(new NetworkRequest.Builder().build(), networkChanged);
            observingNetwork = true;
        } catch (RuntimeException ignored) { /* static status remains available */ }
        refresh();
    }

    void stop() {
        close();
        if (!started) return;
        started = false;
        activity.unregisterReceiver(changed);
        activity.getContentResolver().unregisterContentObserver(brightnessChanged);
        if (observingNetwork) {
            network.unregisterNetworkCallback(networkChanged);
            observingNetwork = false;
        }
        handler.removeCallbacksAndMessages(null);
    }

    private int connectionState() {
        if (network == null) return 0;
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                NetworkCapabilities c = network.getNetworkCapabilities(network.getActiveNetwork());
                if (c == null) return 0;
                if (c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return 1;
                if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return 2;
                return 3; // a VPN or another transport is not mislabeled as Ethernet
            }
            android.net.NetworkInfo n = network.getActiveNetworkInfo();
            if (n == null || !n.isConnected()) return 0;
            return n.getType() == ConnectivityManager.TYPE_ETHERNET ? 1
                    : n.getType() == ConnectivityManager.TYPE_WIFI ? 2 : 3;
        } catch (RuntimeException e) { return 0; }
    }

    void refresh() {
        Locale locale = Locale.getDefault();
        boolean h24 = "24".equals(clockPreference) || (!"12".equals(clockPreference)
                && android.text.format.DateFormat.is24HourFormat(activity));
        time.setText(new SimpleDateFormat(h24 ? "HH:mm" : "h:mm a", locale).format(new Date()));
        date.setText(HeaderValues.date(System.currentTimeMillis(), locale, TimeZone.getDefault()));
        int state = connectionState();
        String networkLabel = state == 1 ? "LAN" : state == 2 ? "Wi-Fi"
                : state == 3 ? label("Network", "Rete") : label("Offline", "Non connesso");
        if (state != networkState) {
            networkState = state;
            Drawable image = new LineIcon(state == 1 ? LineIcon.ETHERNET : LineIcon.WIFI);
            if (state == 0) image = new LayerDrawable(new Drawable[] {image, new LineIcon(LineIcon.SLASH)});
            connection.setImageDrawable(image);
        }
        connection.setContentDescription(networkLabel);
        boolean muted = audio == null || audio.isStreamMute(AudioManager.STREAM_MUSIC)
                || audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0;
        if (!muted) audibleVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        if ((muted ? 1 : 0) != volumeState) {
            volumeState = muted ? 1 : 0;
            speaker.setImageDrawable(new LineIcon(muted ? LineIcon.SOUND_MUTED : LineIcon.SOUND));
        }
        String sound = label(muted ? "Muted" : "Sound on", muted ? "Audio muto" : "Audio attivo");
        speaker.setContentDescription(sound);
        setContentDescription(time.getText() + ", " + date.getText() + ", " + networkLabel + ", " + sound
                + ". " + label("OK: quick settings", "OK: impostazioni rapide"));
        refreshControls();
    }

    void open() {
        if (shade != null && shade.isShowing()) return;
        shade = new Dialog(activity, android.R.style.Theme_Material_Dialog_NoActionBar);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(VERTICAL);
        box.setPadding(px(96), px(28), px(96), px(28));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFF161D29);
        background.setCornerRadii(new float[] {0,0,0,0,px(32),px(32),px(32),px(32)});
        box.setBackground(background);
        TextView title = text(30, Color.WHITE);
        title.setText(label("Quick settings", "Impostazioni rapide"));
        box.addView(title);

        volumeValue = text(25, 0xE6FFFFFF);
        volume = slider(box, volumeValue, LineIcon.SOUND);
        volume.setId(View.generateViewId());
        volume.setOnKeyListener((v, key, event) -> {
            if (key != KeyEvent.KEYCODE_DPAD_UP) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) close();
            return true;
        });
        volume.setOnSeekBarChangeListener(listener(value -> {
            if (audio == null || audio.isVolumeFixed()) return;
            try { audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0); }
            catch (RuntimeException e) { notice(label("Use the TV remote for volume", "Usa il telecomando per il volume")); }
            refresh();
        }));

        brightnessValue = text(25, 0xE6FFFFFF);
        brightness = slider(box, brightnessValue, LineIcon.SUN);
        brightness.setId(View.generateViewId());
        volume.setNextFocusDownId(brightness.getId());
        brightness.setNextFocusUpId(volume.getId());
        brightness.setMax(tvBrightness == null ? 99 : 100);
        brightness.setKeyProgressIncrement(5);
        brightness.setOnSeekBarChangeListener(listener(value -> changeBrightness(value + (tvBrightness == null ? 1 : 0))));

        LinearLayout actions = new LinearLayout(activity);
        actions.setGravity(Gravity.CENTER);
        LayoutParams ap = new LayoutParams(-1, px(74));
        ap.topMargin = px(18);
        box.addView(actions, ap);
        mute = action(actions, label("Mute", "Muto"), LineIcon.SOUND_MUTED, () -> {
            if (audio != null && !audio.isVolumeFixed()) try {
                boolean isMuted = audio.isStreamMute(AudioManager.STREAM_MUSIC)
                        || audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0;
                if (isMuted) {
                    // Some TV firmware restores a vendor default on unmute. Preserve the user's
                    // previous audible level instead of unexpectedly raising it.
                    int restore = audibleVolume;
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, restore, 0);
                } else {
                    audibleVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                }
            } catch (RuntimeException e) { notice(label("Use the TV remote", "Usa il telecomando")); }
            refresh();
        });
        brightness.setNextFocusDownId(mute.getId());
        action(actions, "Play Store", LineIcon.PLAY, () -> { close(); store.run(); });
        action(actions, Strings.t("Settings"), LineIcon.SETTINGS, () -> { close(); settings.run(); });
        TextView hint = text(21, 0xA8FFFFFF);
        hint.setText(label("← → adjust · ↑ from volume / Back: close", "← → regola · ↑ dal volume / Indietro: chiudi"));
        hint.setGravity(Gravity.CENTER);
        LayoutParams hp = new LayoutParams(-1, px(46));
        hp.topMargin = px(16);
        box.addView(hint, hp);
        shade.setContentView(box);
        shade.setOnDismissListener(d -> {
            shade = null;
            volume = brightness = null;
            requestFocus();
        });
        Window window = shade.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setDimAmount(0.55f);
        shade.show();
        window.setLayout(-1, -2);
        WindowManager.LayoutParams wp = window.getAttributes();
        wp.gravity = Gravity.TOP;
        wp.y = px(HEIGHT);
        window.setAttributes(wp);
        refreshControls();
        volume.requestFocus();
    }

    private SeekBar slider(LinearLayout parent, TextView value, int[] shape) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams rp = new LayoutParams(-1, px(102));
        rp.topMargin = px(14);
        parent.addView(row, rp);
        ImageView image = new ImageView(activity);
        image.setImageDrawable(new LineIcon(shape));
        row.addView(image, new LayoutParams(px(38), px(38)));
        LayoutParams vp = new LayoutParams(px(300), -1);
        vp.leftMargin = px(24);
        row.addView(value, vp);
        SeekBar control = new SeekBar(activity);
        control.setFocusable(true);
        control.setSplitTrack(false);
        control.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF8BC7FF));
        control.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        control.setPadding(px(24), px(12), px(24), px(12));
        control.setBackground(focusBackground());
        row.addView(control, new LayoutParams(0, px(80), 1));
        return control;
    }

    private android.graphics.drawable.StateListDrawable focusBackground() {
        android.graphics.drawable.StateListDrawable result = new android.graphics.drawable.StateListDrawable();
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0xFF30465E);
        focused.setCornerRadius(px(18));
        focused.setStroke(Math.max(1, px(2)), 0xFF8BC7FF);
        result.addState(new int[] {android.R.attr.state_focused}, focused);
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(0xFF222C3B);
        normal.setCornerRadius(px(18));
        result.addState(new int[] {}, normal);
        return result;
    }

    private TextView action(LinearLayout parent, String label, int[] icon, Runnable action) {
        TextView button = text(25, Color.WHITE);
        button.setText(label);
        button.setId(View.generateViewId());
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        button.setBackground(focusBackground());
        Drawable image = new LineIcon(icon);
        image.setBounds(0, 0, px(30), px(30));
        button.setCompoundDrawables(image, null, null, null);
        button.setCompoundDrawablePadding(px(14));
        button.setPadding(px(28), 0, px(28), 0);
        button.setOnClickListener(v -> action.run());
        button.setNextFocusUpId(brightness.getId());
        LayoutParams bp = new LayoutParams(0, -1, 1);
        bp.setMargins(px(8), 0, px(8), 0);
        parent.addView(button, bp);
        return button;
    }

    private interface Change { void to(int value); }
    private SeekBar.OnSeekBarChangeListener listener(Change change) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (user && !syncing) change.to(value);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        };
    }

    private void refreshControls() {
        if (volume == null || brightness == null) return;
        syncing = true;
        int max = audio == null ? 0 : audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audio == null ? 0 : audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        boolean fixed = audio == null || audio.isVolumeFixed();
        volume.setMax(max);
        volume.setKeyProgressIncrement(1);
        volume.setProgress(current);
        volume.setEnabled(!fixed);
        boolean muted = audio == null || audio.isStreamMute(AudioManager.STREAM_MUSIC) || current == 0;
        volumeValue.setText(label("Volume", "Volume") + " · " + (fixed ? label("TV remote", "Telecomando TV")
                : muted ? label("Muted", "Muto") : HeaderValues.percent(current, max) + "%"));
        volume.setContentDescription(volumeValue.getText());
        mute.setText(label(muted ? "Unmute" : "Mute", muted ? "Attiva audio" : "Muto"));
        mute.setEnabled(!fixed);
        if (tvBrightness != null) {
            int level = tvBrightness.level();
            boolean available = level >= 0 && level <= 100 && tvBrightness.canWrite();
            brightness.setProgress(Math.max(0, level));
            String status = level < 0 ? label("Unavailable", "Non disponibile")
                    : !tvBrightness.canWrite() ? label("Authorization needed", "Richiede autorizzazione") : level + "%";
            brightnessValue.setText(Strings.t("Brightness") + " · " + status);
            brightness.setContentDescription(brightnessValue.getText());
            brightness.setAlpha(available ? 1f : 0.55f);
            syncing = false;
            return;
        }
        int raw = Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
        boolean writable = Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(activity);
        boolean automatic = Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == 1;
        boolean available = raw >= 0 && writable && !automatic;
        brightness.setEnabled(true); // still opens the appropriate system page when not writable
        brightness.setProgress(Math.max(0, HeaderValues.percent(raw, 255) - 1));
        String status = raw < 0 ? label("TV settings", "Impostazioni TV") : automatic ? "Auto"
                : !writable ? label("Enable control", "Abilita controllo") : HeaderValues.percent(raw, 255) + "%";
        brightnessValue.setText(Strings.t("Brightness") + " · " + status);
        brightness.setContentDescription(brightnessValue.getText());
        brightness.setAlpha(available ? 1f : 0.55f);
        syncing = false;
    }

    private void changeBrightness(int percent) {
        if (tvBrightness != null) {
            try { tvBrightness.set(percent); }
            catch (SecurityException e) {
                notice(label("Authorize TV brightness control first", "Serve autorizzare il controllo luminosità TV"));
            } catch (RuntimeException e) {
                notice(label("TV brightness could not be applied", "Impossibile applicare la luminosità TV"));
            }
            refreshControls();
            return;
        }
        int current = Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
        boolean automatic = Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == 1;
        if (current < 0 || automatic) {
            close();
            picture.run();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(activity)) {
            close();
            try { activity.startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    android.net.Uri.parse("package:" + activity.getPackageName()))); }
            catch (RuntimeException e) { picture.run(); }
            return;
        }
        try {
            boolean saved = Settings.System.putInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                    HeaderValues.brightness(percent));
            if (!saved) notice(label("Use the TV picture settings", "Usa le impostazioni immagine della TV"));
        } catch (RuntimeException e) { notice(label("Brightness unavailable", "Luminosità non disponibile")); }
        refreshControls();
    }

    private void notice(String text) { Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
    private void close() { if (shade != null) shade.dismiss(); }
}
