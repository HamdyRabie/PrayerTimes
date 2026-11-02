package com.attech.prayertimes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;

/** Meaningful JVM checks for dates, chronological ordering, offsets and alarm selection. */
public final class PrayerEngineTest {
    private static int checks;
    private static void check(boolean condition,String message) {
        checks++; if(!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        PrayerEngine.Config c=new PrayerEngine.Config();
        LocalDate reference=LocalDate.of(2026,11,2);
        double[][] places={{30.299,31.741},{30.0444,31.2357},{31.2001,29.9187},{24.0889,32.8998}};
        for(double[] place:places) {
            c.latitude=place[0];c.longitude=place[1];
            for(int d=0;d<365;d++) {
                LocalDate day=LocalDate.of(2026,1,1).plusDays(d);
                long[] times=PrayerEngine.times(day,c);
                for(int i=0;i<6;i++) {
                    check(Instant.ofEpochMilli(times[i]).atZone(c.zoneId()).toLocalDate().equals(day),"Prayer on wrong day: "+day);
                    if(i>0) check(times[i]>times[i-1],"Incorrect prayer order: "+day);
                }
                long afterIsha=times[5]+1000;
                PrayerEngine.Event next=PrayerEngine.next(afterIsha,c,true);
                check(next!=null && next.index==0,"After Isha must select tomorrow Fajr");
                check(next.time==PrayerEngine.times(day.plusDays(1),c)[0],"Tomorrow Fajr mismatch on "+day+" at "+place[0]+","+place[1]+": actual "+Instant.ofEpochMilli(next.time)+", expected "+Instant.ofEpochMilli(PrayerEngine.times(day.plusDays(1),c)[0]));
            }
        }
        c=new PrayerEngine.Config();
        long[] base=PrayerEngine.times(reference,c);
        long[] expectedMinutes={4*60+40,6*60+8,11*60+37,14*60+43,17*60+5,18*60+24};
        for(int i=0;i<6;i++) {
            java.time.ZonedDateTime local=Instant.ofEpochMilli(base[i]).atZone(c.zoneId());
            long minute=local.getHour()*60+local.getMinute();
            // Independent AlAdhan Egyptian-method snapshot; allow modest implementation differences.
            check(Math.abs(minute-expectedMinutes[i])<=2,"Independent reference mismatch, prayer "+i+": "+local);
        }
        c.offsets[2]=7;
        long[] shifted=PrayerEngine.times(reference,c);
        for(int i=0;i<6;i++) check(shifted[i]-base[i]==(i==2 ? 420000:0),"Offset changed wrong prayer");
        c.offsets[2]=0;
        for(int i:PrayerEngine.PRAYERS) c.enabled[i]=false;
        check(PrayerEngine.next(base[0]-1000,c,true)==null,"All disabled must not schedule");
        check(PrayerEngine.next(base[0]-1000,c,false).index==0,"Display must still show next prayer");
        c.enabled[5]=true;
        check(PrayerEngine.next(base[0],c,true).index==5,"Disabled prayers must be skipped");
        c.enabled[0]=true;
        check(PrayerEngine.next(base[5],c,true).index==0,"Boundary must not repeat the fired prayer");
        long[] utc=PrayerEngine.times(reference,c);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        check(Arrays.equals(utc,PrayerEngine.times(reference,c)),"Calculation depends on phone timezone");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        check(Arrays.equals(utc,PrayerEngine.times(reference,c)),"Calculation depends on JVM timezone");
        c.hanafi=true;
        long[] hanafi=PrayerEngine.times(reference,c);
        check(hanafi[3]>utc[3],"Hanafi Asr should be later");
        for(int i:new int[]{0,1,2,4,5}) check(hanafi[i]==utc[i],"Madhab affected another prayer");
        check(ZoneId.of("Africa/Cairo").getRules().getOffset(Instant.parse("2026-07-01T12:00:00Z")).getTotalSeconds()==10800,"Summer DST missing");
        check(ZoneId.of("Africa/Cairo").getRules().getOffset(Instant.parse("2026-11-02T12:00:00Z")).getTotalSeconds()==7200,"Winter offset wrong");
        System.out.println("PASS: "+checks+" prayer-time and scheduling checks");
        c=new PrayerEngine.Config();
        for(long time:PrayerEngine.times(reference,c)) System.out.println(Instant.ofEpochMilli(time).atZone(c.zoneId()));
    }
}
