package com.attech.prayertimes;

import android.content.Context;

final class VoiceCatalog {
    static final class Voice {
        final String id,name,description;
        final int resource;
        Voice(String id,String name,String description,int resource) {
            this.id=id;this.name=name;this.description=description;this.resource=resource;
        }
        String display() { return name+"\n"+description; }
    }
    static final Voice[] VOICES = {
        new Voice("refaat","محمد رفعت","تسجيل تاريخي • ٣:٢٥",R.raw.mohamed_refaat_adhan),
        new Voice("afasy","مشاري راشد العفاسي","تسجيل رقمي • نحو 200 kbps • ٣:٣٥",R.raw.mishary_afasy_adhan),
        new Voice("qatami","ناصر القطامي","تسجيل رقمي • 128 kbps • ٢:١٢",R.raw.nasser_qatami_adhan),
        new Voice("nafees","أحمد النفيس","تسجيل رقمي • 128 kbps • ٣:٣٤",R.raw.ahmad_nafees_adhan)
    };
    static Voice find(String id) {
        for(Voice voice:VOICES) if(voice.id.equals(id)) return voice;
        return VOICES[0];
    }
    static Voice current(Context c) { return find(AppSettings.prefs(c).getString("voice_id","refaat")); }
}
