package com.attech.prayertimes;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Offline astronomical calculations; epoch times are never interpreted as local UTC. */
public final class PrayerEngine {
    public static final String[] NAMES = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
    public static final int[] PRAYERS = {0, 2, 3, 4, 5};
    public static final class Config {
        public String city = "العاشر من رمضان";
        public double latitude = 30.2990, longitude = 31.7410;
        public String zone = "Africa/Cairo";
        public String method = "EGYPTIAN";
        public boolean hanafi = false;
        public int[] offsets = new int[6];
        public boolean[] enabled = {true, false, true, true, true, true};
        public boolean[] iqamaEnabled = {true, false, true, true, true, true};
        public int[] iqamaMinutes = {20, 0, 15, 15, 10, 15};
        public ZoneId zoneId() { return ZoneId.of(zone); }
    }
    public static final class Event {
        public final int index;
        public final long time;
        public final boolean iqama;
        public Event(int index, long time) { this(index,time,false); }
        public Event(int index, long time, boolean iqama) { this.index = index; this.time = time; this.iqama = iqama; }
        public String name() { return NAMES[index]; }
    }
    public static long iqamaTime(long adhanTime, int prayer, Config c) {
        if (prayer < 0 || prayer >= NAMES.length || prayer == 1) throw new IllegalArgumentException("ليست صلاة");
        return adhanTime + c.iqamaMinutes[prayer] * 60_000L;
    }
    /** Includes yesterday: Isha plus a user delay may fall after midnight. */
    public static Event nextIqama(long now, Config c) {
        LocalDate today = java.time.Instant.ofEpochMilli(now).atZone(c.zoneId()).toLocalDate();
        Event best = null;
        for (int d=-1; d<3; d++) {
            long[] ts = times(today.plusDays(d),c);
            for (int i : PRAYERS) {
                if (!c.iqamaEnabled[i]) continue;
                long due = iqamaTime(ts[i],i,c);
                if (due > now && (best == null || due < best.time)) best = new Event(i,due,true);
            }
        }
        return best;
    }
    public static long[] times(LocalDate day, Config c) {
        CalculationParameters params = CalculationMethod.valueOf(c.method).getParameters();
        params.madhab = c.hanafi ? Madhab.HANAFI : Madhab.SHAFI;
        PrayerTimes p = new PrayerTimes(new Coordinates(c.latitude, c.longitude),
            new DateComponents(day.getYear(), day.getMonthValue(), day.getDayOfMonth()), params);
        Date[] values = {p.fajr, p.sunrise, p.dhuhr, p.asr, p.maghrib, p.isha};
        long[] result = new long[6];
        for (int i = 0; i < 6; i++) {
            if (values[i] == null) throw new IllegalArgumentException("تعذر حساب المواقيت لهذا الموقع");
            // Adhan 1.2.1 retains the current millisecond in Calendar instances.
            // Normalize to the returned minute so repeated calculations are deterministic.
            result[i] = Math.floorDiv(values[i].getTime(), 60_000L) * 60_000L + c.offsets[i] * 60_000L;
        }
        return result;
    }
    public static Event next(long now, Config c, boolean enabledOnly) {
        LocalDate today = java.time.Instant.ofEpochMilli(now).atZone(c.zoneId()).toLocalDate();
        Event best = null;
        for (int d = 0; d < 3; d++) {
            long[] ts = times(today.plusDays(d), c);
            for (int i : PRAYERS) {
                if (enabledOnly && !c.enabled[i]) continue;
                if (ts[i] > now && (best == null || ts[i] < best.time)) best = new Event(i, ts[i]);
            }
            if (best != null) return best;
        }
        return null;
    }
    private PrayerEngine() {}
}
