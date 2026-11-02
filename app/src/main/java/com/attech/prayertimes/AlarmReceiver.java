package com.attech.prayertimes;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        String action=intent.getAction();
        boolean iqama=PrayerScheduler.IQAMA_ACTION.equals(action)||PrayerScheduler.TEST_IQAMA_ACTION.equals(action);
        boolean test=PrayerScheduler.TEST_ACTION.equals(action)||PrayerScheduler.TEST_IQAMA_ACTION.equals(action);
        if (!iqama && !test && !PrayerScheduler.PRAYER_ACTION.equals(action)) return;
        long scheduled=intent.getLongExtra("scheduled",0), now=System.currentTimeMillis();
        int index=intent.getIntExtra("index",-1);
        if (!test) {
            long after=Math.max(now+1000,scheduled+1000);
            if (iqama) PrayerScheduler.scheduleIqama(c,after); else PrayerScheduler.scheduleAdhan(c,after);
        }
        if (!AppSettings.active(c)) return;
        if (!test) {
            if(index<0||index>=6||index==1) return;
            PrayerEngine.Config config=AppSettings.config(c);
            if(iqama ? !config.iqamaEnabled[index] : !config.enabled[index]) return;
        }
        if (scheduled<=0 || Math.abs(now-scheduled)>(iqama?5:15)*60_000L) return;
        String pref=iqama?"last_fire_iqama":"last_fire_adhan";
        String key=(test?"test":String.valueOf(index))+":"+scheduled;
        if(key.equals(AppSettings.prefs(c).getString(pref,""))) return;
        String channel=iqama?PrayerScheduler.IQAMA_CHANNEL:PrayerScheduler.CHANNEL;
        if (!PrayerScheduler.notificationsAllowed(c,channel)) return;
        AppSettings.prefs(c).edit().putString(pref,key).apply();
        if (iqama) {
            try { PrayerScheduler.showIqama(c,index,scheduled,test); }
            catch(RuntimeException e) { AppSettings.prefs(c).edit().putString("playback_error","تعذر إظهار تنبيه الإقامة. راجع صلاحية الإشعارات.").apply(); }
            return;
        }
        try {
            c.startForegroundService(new Intent(c,AdhanService.class).putExtra("index",index).putExtra("test_alarm",test));
        } catch(RuntimeException e) {
            AppSettings.prefs(c).edit().putString("playback_error","حان وقت الصلاة، لكن النظام منع تشغيل الصوت. افتح التطبيق وراجع إعدادات البطارية.").apply();
            Notification n=new Notification.Builder(c,PrayerScheduler.CHANNEL)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(test?"اختبار تنبيه الأذان":"حان وقت صلاة "+PrayerEngine.NAMES[index])
                .setContentText("اضغط لفتح التطبيق وتجربة الأذان").setContentIntent(PrayerScheduler.open(c)).setAutoCancel(true).build();
            c.getSystemService(NotificationManager.class).notify(42,n);
        }
    }
}
