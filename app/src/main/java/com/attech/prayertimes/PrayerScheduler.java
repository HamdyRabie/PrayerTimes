package com.attech.prayertimes;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

final class PrayerScheduler {
    static final String CHANNEL = "adhan_playback_v1";
    static final String IQAMA_CHANNEL = "iqama_reminder_v1";
    static final String PRAYER_ACTION = "com.attech.prayertimes.PRAYER";
    static final String IQAMA_ACTION = "com.attech.prayertimes.IQAMA";
    static final String TEST_ACTION = "com.attech.prayertimes.TEST";
    static final String TEST_IQAMA_ACTION = "com.attech.prayertimes.TEST_IQAMA";
    static final int REQUEST = 701;

    static void channel(Context c) {
        NotificationManager manager = c.getSystemService(NotificationManager.class);
        NotificationChannel adhan = new NotificationChannel(CHANNEL,"الأذان",NotificationManager.IMPORTANCE_HIGH);
        adhan.setDescription("تنبيه مواقيت الصلاة والتحكم في صوت الأذان");
        adhan.setSound(null,null); adhan.enableVibration(false);
        manager.createNotificationChannel(adhan);
        NotificationChannel iqama = new NotificationChannel(IQAMA_CHANNEL,"تذكير الإقامة",NotificationManager.IMPORTANCE_HIGH);
        iqama.setDescription("جرس قصير عند موعد الإقامة الذي حددته لكل صلاة");
        iqama.setSound(Uri.parse("android.resource://"+c.getPackageName()+"/raw/iqama_chime"),
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        iqama.enableVibration(false);
        manager.createNotificationChannel(iqama);
    }
    static boolean appNotificationsAllowed(Context c) {
        if (Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return false;
        return c.getSystemService(NotificationManager.class).areNotificationsEnabled();
    }
    static boolean notificationsAllowed(Context c) { return notificationsAllowed(c,CHANNEL); }
    static boolean notificationsAllowed(Context c,String channel) {
        if (!appNotificationsAllowed(c)) return false;
        NotificationChannel ch=c.getSystemService(NotificationManager.class).getNotificationChannel(channel);
        return ch==null || ch.getImportance()!=NotificationManager.IMPORTANCE_NONE;
    }
    static boolean exactAllowed(Context c) {
        return Build.VERSION.SDK_INT<31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }
    static synchronized void schedule(Context c) {
        channel(c);
        AppSettings.prefs(c).edit().remove("schedule_error").apply();
        long now=System.currentTimeMillis();
        scheduleAdhan(c,now);
        scheduleIqama(c,now);
        if (!AppSettings.active(c) || !exactAllowed(c) || !appNotificationsAllowed(c)) cancelTest(c);
    }
    // Each stream rearms independently: an Adhan cannot erase a coincident Iqama.
    static synchronized void scheduleAdhan(Context c,long after) {
        cancel(c,REQUEST,PRAYER_ACTION);
        AppSettings.prefs(c).edit().remove("next_alarm").apply();
        if (!AppSettings.active(c) || !exactAllowed(c) || !notificationsAllowed(c)) return;
        try {
            PrayerEngine.Event next=PrayerEngine.next(after,AppSettings.config(c),true);
            if (next!=null) set(c,REQUEST,PRAYER_ACTION,next,"next_alarm");
        } catch (RuntimeException e) { scheduleError(c); }
    }
    static synchronized void scheduleIqama(Context c,long after) {
        cancel(c,REQUEST+2,IQAMA_ACTION);
        AppSettings.prefs(c).edit().remove("next_iqama").apply();
        if (!AppSettings.active(c) || !exactAllowed(c) || !notificationsAllowed(c,IQAMA_CHANNEL)) return;
        try {
            PrayerEngine.Event next=PrayerEngine.nextIqama(after,AppSettings.config(c));
            if (next!=null) set(c,REQUEST+2,IQAMA_ACTION,next,"next_iqama");
        } catch (RuntimeException e) { scheduleError(c); }
    }
    private static void set(Context c,int request,String action,PrayerEngine.Event event,String pref) {
        Intent i=new Intent(c,AlarmReceiver.class).setAction(action).putExtra("index",event.index).putExtra("scheduled",event.time);
        PendingIntent fire=PendingIntent.getBroadcast(c,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        // User-visible alarm clocks avoid the idle quota between a close Adhan and Iqama.
        c.getSystemService(AlarmManager.class).setAlarmClock(new AlarmManager.AlarmClockInfo(event.time,open(c)),fire);
        AppSettings.prefs(c).edit().putLong(pref,event.time).apply();
    }
    private static void cancel(Context c,int request,String action) {
        PendingIntent old=PendingIntent.getBroadcast(c,request,new Intent(c,AlarmReceiver.class).setAction(action),
            PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
        if (old!=null) c.getSystemService(AlarmManager.class).cancel(old);
    }
    static void cancelTest(Context c) {
        cancel(c,REQUEST+1,TEST_ACTION); cancel(c,REQUEST+3,TEST_IQAMA_ACTION);
        c.getSystemService(NotificationManager.class).cancel(199);
    }
    static void testInOneMinute(Context c) { testInOneMinute(c,false); }
    static void testInOneMinute(Context c,boolean iqama) {
        PrayerEngine.Event event=new PrayerEngine.Event(-1,System.currentTimeMillis()+60_000,iqama);
        set(c,iqama?REQUEST+3:REQUEST+1,iqama?TEST_IQAMA_ACTION:TEST_ACTION,event,iqama?"test_iqama":"test_adhan");
    }
    static void showIqama(Context c,int index,long due,boolean test) {
        channel(c);
        String name=test?"اختبار تنبيه الإقامة":"حان موعد إقامة صلاة "+PrayerEngine.NAMES[index];
        String detail=test?"هذا اختبار للجرس والإشعار فقط":"وفق الموعد الذي ضبطته • "+AppSettings.config(c).city;
        Notification n=new Notification.Builder(c,IQAMA_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(name).setContentText(detail)
            .setStyle(new Notification.BigTextStyle().bigText(detail))
            .setCategory(Notification.CATEGORY_REMINDER).setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(open(c)).setAutoCancel(true).setWhen(due).setShowWhen(true)
            .setTimeoutAfter(30*60_000L).build();
        c.getSystemService(NotificationManager.class).notify(test?199:100+index,n);
    }
    private static void scheduleError(Context c) {
        AppSettings.prefs(c).edit().putString("schedule_error","تعذر ضبط التنبيهات. راجع صلاحية المنبهات والموقع.").apply();
    }
    static PendingIntent open(Context c) {
        return PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
}
