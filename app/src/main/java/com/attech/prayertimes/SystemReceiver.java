package com.attech.prayertimes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Rebuild alarms after boot or clock changes; never start audio from boot. */
public class SystemReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) { PrayerScheduler.schedule(c); }
}
