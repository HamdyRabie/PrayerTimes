# Embedded audio — version 1.1

The three additional Adhan recordings are bundled as the original downloaded MP3 bytes. They have not been upsampled, re-encoded, or assigned an artificial higher bitrate. They are playable offline. The historical Muhammad Refaat recording remains available at its original quality.

| Recording | Sample rate | Audio bitrate | Duration |
| --- | --- | --- | --- |
| Muhammad Refaat | 32 kHz, mono | 32 kbps | 3:24.84 |
| Mishary Rashid Alafasy | 44.1 kHz, stereo | ~200 kbps VBR | 3:35.22 |
| Nasser Al-Qatami | 44.1 kHz, stereo | 128 kbps | 2:12.08 |
| Ahmad Al-Nafees | 44.1 kHz, stereo | 128 kbps | 3:33.68 |
| Iqama chime | 44.1 kHz, mono | PCM 16-bit | 0:03.40 |

## Sources

- Muhammad Refaat: [recording page](https://www.ashefaa.com/play-14332.html), [audio](https://www.ashefaa.com/ruqia/Azan/7.mp3).
- Mishary Rashid Alafasy: [AlAdhan download page](https://aladhan.com/download-adhans), entry “Adhan from Dubai's One TV by Mishary Rashid Alafasy”; [original MP3](https://cdn.aladhan.com/audio/adhans/a4.mp3).
- Nasser Al-Qatami: [Assabile Adhan collection](https://ar.assabile.com/adhan-call-prayer), track a117, attributed to Nasser Al-Qatami; [original MP3](https://media.assabile.com/assabile/adhan_3435370/6f509ec934a4.mp3).
- Ahmad Al-Nafees: [AlAdhan download page](https://aladhan.com/download-adhans), entry “Adhan by Ahmad al-Nafees”; [original MP3](https://cdn.aladhan.com/audio/adhans/a1.mp3).
- The Iqama chime is a generated sequence of three bell-like sine tones, stored as PCM with no source recording or spoken voice.

## Verification

Each complete audio stream was decoded with FFmpeg without decoder errors. The new MP3s' hashes were compared to the downloaded originals. Codec, sample rate, audio bitrate, duration and peak volume were inspected with FFprobe/FFmpeg. The packaged APK's audio bytes were also compared with the verified resources.

These are file-integrity and signal-format checks. A listening test on the target phone is still needed for perceived clarity and the phone's speaker level. No human listening test is claimed.

```text
mohamed_refaat_adhan.mp3  ee054cce38ef75fdb43f1fbb67504300afe575dd3dfe2ae7250c391bf6a60159
mishary_afasy_adhan.mp3   b816ca02d445647a6dd9d9b519e2255f4d6a3597958907f6a9cbab237dcd5b67
nasser_qatami_adhan.mp3   77c9e53d0ad753d61fd0122d211291892e826a64c139422dc274c968c860c3cc
ahmad_nafees_adhan.mp3    68c460c82eb977d914738917069d714d3e94eecc648c38e038683b40c79e98ae
```
