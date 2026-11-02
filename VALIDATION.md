# Validation — 1.1 — 2026-11-02

## Completed

- All application Java sources compiled against Android SDK 36 using Java 8 bytecode.
- Android resources and manifest linked with Android Build Tools 36.0.0.
- D8 converted application and bundled Adhan Java library to DEX with minimum API 26.
- APK zip alignment and APK Signature Scheme v2/v3 verification completed.
- All four Adhan MP3s and the Iqama PCM chime decoded from beginning to end using FFmpeg, without decoding errors.
- Three new Adhan files are unchanged byte-for-byte from their original downloads, at 44.1 kHz / 128–200 kbps.
- Signing certificate and package identifier match version 1.0; versionCode increased from 1 to 2.
- Iqama JVM suite: **7,354 checks passed**. Combined JVM total: **26,360 checks passed**.
- PrayerEngine JVM test suite: **19,006 checks passed**.

The prayer tests cover all 365 days of 2026 in 10th of Ramadan City, Cairo, Alexandria and Aswan: valid calendar day, chronological prayer ordering, and selection of tomorrow's Fajr after Isha. They also cover disabled prayers, all prayers disabled, exact boundaries after a fired alarm, independence from the JVM/device timezone, per-prayer offsets, the Hanafi Asr setting and Egypt winter/summer UTC offsets.

An independent AlAdhan Egyptian-method snapshot for 2026-11-02, 30.299 N / 31.741 E, was used with a maximum two-minute difference for implementation-specific rounding and transit adjustments:

| Prayer | App, Africa/Cairo | Independent reference |
| --- | --- | --- |
| Fajr | 04:40 | 04:40 |
| Sunrise | 06:08 | 06:08 |
| Dhuhr | 11:38 | 11:37 |
| Asr | 14:42 | 14:43 |
| Maghrib | 17:05 | 17:05 |
| Isha | 18:24 | 18:24 |

The original Adhan 1.2.1 Calendar implementation retains variable milliseconds. PrayerEngine normalizes its rounded results to the minute; tests verify stable repeated calculations so that alarm comparisons and row highlighting use consistent timestamps.

## Iqama checks

The new suite verifies per-prayer delays from 1 to 120 minutes; independent Adhan and Iqama switches; no Iqama for sunrise; cumulative prayer-time offsets and Iqama delays; all 365 days of 2026 in two Egyptian cities; exact trigger boundaries; an Isha Iqama that crosses local midnight in London; and simultaneous Adhan/Iqama times.

Adhan and Iqama use separate pending intents and rearm independently. They use user-visible `setAlarmClock` alarms so a short Iqama interval does not encounter the idle quota of `setExactAndAllowWhileIdle`. The Iqama notification uses its own channel and a bundled 3.4-second chime. These Android integration behaviors were checked in source and compilation, not on a running device.

## Device checks still required

No Android device or emulator was available in this build environment. The following are **not claimed as tested**:

- Installation and RTL layout on the target phone, including large system fonts.
- Notification permission and exact-alarm permission grant/denial flows.
- Sound playback, volume adjustment and Stop action on the target phone.
- Both one-minute tests with the screen locked, including device-specific battery restrictions.
- Iqama notification channel sound, voice selection preview/save, and simultaneous alarms on the target device.
- Alarm continuity after reboot, clock/timezone changes, updates, and process termination.
- Audio-focus behavior during calls and other media playback.

The app includes an explicit one-minute alarm test to make the background-playback check easy on the user's phone. The APK is a signed trial build; it is not represented as a store-approved production release.

## Re-run prayer checks

Run from the project root using JDK 17 (use `;` instead of `:` for the runtime classpath on Windows):

```sh
mkdir -p test-classes
java com.sun.tools.javac.Main --release 8 -encoding UTF-8 -cp app/libs/adhan-1.2.1.jar -d test-classes app/src/main/java/com/attech/prayertimes/PrayerEngine.java tests/PrayerEngineTest.java tests/IqamaEngineTest.java
java -cp test-classes:app/libs/adhan-1.2.1.jar com.attech.prayertimes.PrayerEngineTest
java -cp test-classes:app/libs/adhan-1.2.1.jar com.attech.prayertimes.IqamaEngineTest
```
