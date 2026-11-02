package com.attech.prayertimes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

public final class IqamaEngineTest {
    private static int checks;
    private static void check(boolean value,String message) {
        checks++; if(!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        PrayerEngine.Config c=new PrayerEngine.Config();
        LocalDate day=LocalDate.of(2026,11,2);
        long[] adhan=PrayerEngine.times(day,c);
        for(int index:PrayerEngine.PRAYERS) {
            check(PrayerEngine.iqamaTime(adhan[index],index,c)-adhan[index]==c.iqamaMinutes[index]*60_000L,"Per-prayer default delay mismatch");
            for(int delay:new int[]{1,5,10,15,20,30,60,120}) {
                c.iqamaMinutes[index]=delay;
                check(PrayerEngine.iqamaTime(adhan[index],index,c)==adhan[index]+delay*60_000L,"Incorrect user delay");
            }
        }
        c=new PrayerEngine.Config();
        Arrays.fill(c.enabled,false);
        PrayerEngine.Event iqama=PrayerEngine.nextIqama(adhan[0]+1000,c);
        check(iqama!=null && iqama.iqama && iqama.index==0,"Iqama must work with muted Adhan");
        check(PrayerEngine.next(adhan[0],c,true)==null,"Muted Adhan must stay muted");
        Arrays.fill(c.iqamaEnabled,false);
        check(PrayerEngine.nextIqama(adhan[0],c)==null,"Disabled Iqamas must not schedule");
        c.iqamaEnabled[1]=true;
        check(PrayerEngine.nextIqama(adhan[0],c)==null,"Sunrise must never have an Iqama");
        c=new PrayerEngine.Config(); c.offsets[2]=7; c.iqamaMinutes[2]=23;
        long[] shifted=PrayerEngine.times(day,c);
        check(PrayerEngine.iqamaTime(shifted[2],2,c)==adhan[2]+30*60_000L,"Prayer offset and Iqama delay must both apply");
        c=new PrayerEngine.Config();
        for(double[] city:new double[][]{{30.299,31.741},{31.2001,29.9187}}) {
            c.latitude=city[0];c.longitude=city[1];
            for(int d=0;d<365;d++) {
                LocalDate date=LocalDate.of(2026,1,1).plusDays(d);
                long[] times=PrayerEngine.times(date,c);
                for(int index:PrayerEngine.PRAYERS) {
                    long due=times[index]+c.iqamaMinutes[index]*60_000L;
                    PrayerEngine.Event before=PrayerEngine.nextIqama(due-1000,c);
                    check(before!=null && before.index==index && before.time==due,"Wrong next Iqama on "+date+" prayer "+index);
                    PrayerEngine.Event after=PrayerEngine.nextIqama(due,c);
                    check(after!=null && after.time>due,"Iqama must not repeat at its exact boundary");
                }
            }
        }
        c=new PrayerEngine.Config();
        c.latitude=51.5074;c.longitude=-0.1278;c.zone="Europe/London";
        Arrays.fill(c.iqamaEnabled,false);c.iqamaEnabled[5]=true;c.iqamaMinutes[5]=120;
        day=LocalDate.of(2026,6,21);
        adhan=PrayerEngine.times(day,c);
        long late=PrayerEngine.iqamaTime(adhan[5],5,c);
        LocalDate iqamaDate=Instant.ofEpochMilli(late).atZone(c.zoneId()).toLocalDate();
        check(iqamaDate.isAfter(day),"Overnight test must cross local midnight");
        PrayerEngine.Event overnight=PrayerEngine.nextIqama(late-1000,c);
        check(overnight!=null && overnight.index==5 && overnight.time==late,"Yesterday Isha Iqama was skipped after midnight");
        c=new PrayerEngine.Config();
        day=LocalDate.of(2026,11,2);adhan=PrayerEngine.times(day,c);
        Arrays.fill(c.enabled,false);Arrays.fill(c.iqamaEnabled,false);
        c.enabled[5]=true;c.iqamaEnabled[4]=true;
        c.iqamaMinutes[4]=(int)((adhan[5]-adhan[4])/60_000L);
        check(c.iqamaMinutes[4]<=120,"Coincident event test must use a valid delay");
        check(PrayerEngine.next(adhan[5]-1000,c,true).time==PrayerEngine.nextIqama(adhan[5]-1000,c).time,"Coincident Adhan and Iqama must both be represented");
        System.out.println("PASS: "+checks+" Iqama timing, independence and midnight checks");
    }
}
