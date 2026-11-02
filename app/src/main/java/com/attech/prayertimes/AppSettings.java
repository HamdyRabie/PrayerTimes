package com.attech.prayertimes;

import android.content.Context;
import android.content.SharedPreferences;

final class AppSettings {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("prayer_settings", Context.MODE_PRIVATE); }
    static PrayerEngine.Config config(Context c) {
        SharedPreferences p = prefs(c);
        PrayerEngine.Config s = new PrayerEngine.Config();
        s.city = p.getString("city", s.city);
        s.latitude = Double.longBitsToDouble(p.getLong("latitude", Double.doubleToLongBits(s.latitude)));
        s.longitude = Double.longBitsToDouble(p.getLong("longitude", Double.doubleToLongBits(s.longitude)));
        s.zone = p.getString("zone", s.zone);
        s.method = p.getString("method", s.method);
        s.hanafi = p.getBoolean("hanafi", false);
        for (int i : PrayerEngine.PRAYERS) {
            s.enabled[i] = p.getBoolean("prayer_" + i, true);
            s.offsets[i] = p.getInt("offset_" + i, 0);
            s.iqamaEnabled[i] = p.getBoolean("iqama_enabled_" + i, true);
            s.iqamaMinutes[i] = Math.max(1,Math.min(120,p.getInt("iqama_minutes_" + i, s.iqamaMinutes[i])));
        }
        return s;
    }
    static boolean active(Context c) { return prefs(c).getBoolean("active", false); }
    static void city(Context c, String name, double lat, double lon, String zone) {
        prefs(c).edit().putString("city", name)
            .putLong("latitude", Double.doubleToLongBits(lat))
            .putLong("longitude", Double.doubleToLongBits(lon)).putString("zone", zone).apply();
    }
}
