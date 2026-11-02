package com.attech.prayertimes;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

public class AdhanService extends Service {
    private MediaPlayer player;
    private AudioManager audio;
    private AudioFocusRequest focus;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = () -> { error("توقف الأذان بعد انتهاء مدة التشغيل القصوى."); stopSelf(); };
    private final SharedPreferences.OnSharedPreferenceChangeListener volumeListener = (prefs, key) -> {
        if ("volume".equals(key) && player != null) {
            float level = prefs.getInt("volume",100)/100f;
            player.setVolume(level,level);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        AppSettings.prefs(this).registerOnSharedPreferenceChangeListener(volumeListener);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || "STOP".equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        releaseAudio();
        PrayerScheduler.channel(this);
        int index = intent.getIntExtra("index", -1);
        VoiceCatalog.Voice voice = VoiceCatalog.find(intent.getStringExtra("voice_id") != null
            ? intent.getStringExtra("voice_id") : VoiceCatalog.current(this).id);
        String title = index >= 0 && index < 6 ? "حان وقت صلاة " + PrayerEngine.NAMES[index] : "تجربة صوت الأذان";
        if (intent.getBooleanExtra("test_alarm",false)) title = "اختبار تنبيه الأذان بعد دقيقة";
        Intent stopIntent = new Intent(this, AdhanService.class).setAction("STOP");
        PendingIntent stop = PendingIntent.getService(this, 33, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, PrayerScheduler.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title)
            .setContentText(voice.name + " • مواقيت الصلاة")
            .setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true).setOngoing(true).setContentIntent(PrayerScheduler.open(this))
            .addAction(new Notification.Action.Builder(null, "إيقاف الأذان", stop).build()).build();
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(41, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(41, n);
            AppSettings.prefs(this).edit().remove("playback_error").apply();
            audio = getSystemService(AudioManager.class);
            if (audio.getMode() != AudioManager.MODE_NORMAL) {
                error("تم كتم الأذان أثناء المكالمة. يمكنك تجربته بعد انتهاء المكالمة.");
                stopSelf(); return START_NOT_STICKY;
            }
            AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
            focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs).setOnAudioFocusChangeListener(change -> {
                    if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stopSelf();
                }).build();
            if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                error("الصوت مشغول بتطبيق آخر. جرّب الأذان بعد إيقافه."); stopSelf(); return START_NOT_STICKY;
            }
            player = new MediaPlayer();
            player.setAudioAttributes(attrs);
            player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            try (AssetFileDescriptor fd = getResources().openRawResourceFd(voice.resource)) {
                player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            }
            float volume = AppSettings.prefs(this).getInt("volume", 100) / 100f;
            player.setVolume(volume, volume);
            player.setOnPreparedListener(mp -> mp.start());
            player.setOnCompletionListener(mp -> stopSelf());
            player.setOnErrorListener((mp, what, extra) -> { error("تعذر تشغيل تسجيل الأذان."); stopSelf(); return true; });
            player.prepareAsync();
            handler.postDelayed(timeout, 10 * 60_000L);
        } catch (Exception e) {
            error("تعذر تشغيل الأذان. راجع صلاحيات التطبيق ومستوى صوت المنبه."); stopSelf();
        }
        return START_NOT_STICKY;
    }
    private void error(String message) { AppSettings.prefs(this).edit().putString("playback_error", message).apply(); }
    private void releaseAudio() {
        handler.removeCallbacks(timeout);
        if (player != null) { player.release(); player = null; }
        if (audio != null && focus != null) { audio.abandonAudioFocusRequest(focus); focus = null; }
    }
    @Override public void onDestroy() {
        AppSettings.prefs(this).unregisterOnSharedPreferenceChangeListener(volumeListener);
        releaseAudio(); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
