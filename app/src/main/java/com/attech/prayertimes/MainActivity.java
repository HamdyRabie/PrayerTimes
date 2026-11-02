package com.attech.prayertimes;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(246,243,235), GREEN = Color.rgb(18,63,54),
        TEAL = Color.rgb(23,101,87), GOLD = Color.rgb(220,190,124), TEXT = Color.rgb(30,46,40),
        MUTED = Color.rgb(109,119,111), LINE = Color.rgb(232,233,224);
    private static final Locale AR = new Locale("ar", "EG");
    private static final String[] CITIES = {"العاشر من رمضان", "القاهرة", "الجيزة", "الزقازيق", "الإسكندرية", "المنصورة", "الإسماعيلية", "السويس", "بورسعيد", "أسيوط", "الأقصر", "أسوان"};
    private static final double[][] COORDS = {{30.2990,31.7410},{30.0444,31.2357},{30.0131,31.2089},{30.5877,31.5020},{31.2001,29.9187},{31.0409,31.3785},{30.5965,32.2715},{29.9668,32.5498},{31.2653,32.3019},{27.1783,31.1859},{25.6872,32.6396},{24.0889,32.8998}};
    private LinearLayout content;
    private TextView countdown;
    private TextView iqamaCountdown;
    private PrayerEngine.Event next;
    private PrayerEngine.Event nextIqama;
    private ScrollView scrollView;
    private LocalDate shownDay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            PrayerEngine.Config c = AppSettings.config(MainActivity.this);
            long now = System.currentTimeMillis();
            LocalDate today = Instant.ofEpochMilli(now).atZone(c.zoneId()).toLocalDate();
            if (!today.equals(shownDay) || (next != null && now >= next.time)
                || (nextIqama != null && now >= nextIqama.time)) render();
            updateCountdown();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setVolumeControlStream(AudioManager.STREAM_ALARM);
        PrayerScheduler.channel(this);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    @Override protected void onResume() {
        super.onResume();
        PrayerScheduler.schedule(this);
        render();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }

    private void render() {
        PrayerEngine.Config c = AppSettings.config(this);
        long now = System.currentTimeMillis();
        shownDay = Instant.ofEpochMilli(now).atZone(c.zoneId()).toLocalDate();
        next = PrayerEngine.next(now, c, false);
        nextIqama = PrayerEngine.nextIqama(now,c);
        int savedScroll = scrollView == null ? 0 : scrollView.getScrollY();
        LinearLayout root = column();
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        ScrollView scroll = new ScrollView(this);
        scrollView = scroll;
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1,-1));
        content = column();
        content.setPadding(dp(20),dp(18),dp(20),dp(26));
        scroll.addView(content, new ScrollView.LayoutParams(-1,-2));
        setContentView(root);
        root.requestApplyInsets();

        TextView eyebrow = label("الأذان بصوت " + VoiceCatalog.current(this).name, 13, TEAL, false);
        content.addView(eyebrow);
        TextView title = label("مواقيت الصلاة", 31, GREEN, true);
        content.addView(title);
        LinearLayout placeRow = row();
        TextView place = label("⌖  " + c.city, 15, MUTED, true);
        placeRow.addView(place, new LinearLayout.LayoutParams(0,-2,1));
        TextView change = label("تغيير المدينة", 13, TEAL, true);
        change.setPadding(dp(10),dp(14),dp(10),dp(14));
        change.setOnClickListener(v -> chooseCity());
        placeRow.addView(change);
        content.addView(placeRow);

        LinearLayout hero = column();
        hero.setPadding(dp(24),dp(20),dp(24),dp(23));
        hero.setBackground(shape(GREEN,28));
        LinearLayout heroTop = row();
        TextView small = label("الصلاة القادمة", 14, GOLD, false);
        heroTop.addView(small, new LinearLayout.LayoutParams(0,-2,1));
        ImageView moon = new ImageView(this);
        moon.setImageResource(R.drawable.ic_launcher_foreground);
        heroTop.addView(moon, new LinearLayout.LayoutParams(dp(54),dp(54)));
        hero.addView(heroTop);
        TextView prayer = label(next != null ? next.name() : "مواقيت اليوم", 32, Color.WHITE, true);
        hero.addView(prayer);
        TextView at = label(next != null ? time(next.time,c) : "—", 42, Color.WHITE, true);
        at.setTextDirection(View.TEXT_DIRECTION_RTL);
        hero.addView(at);
        countdown = label("", 15, GOLD, false);
        hero.addView(countdown);
        iqamaCountdown = label("",13,GOLD,false);
        iqamaCountdown.setPadding(0,dp(10),0,0);
        hero.addView(iqamaCountdown);
        block(hero, 8, 22);
        updateCountdown();

        TextView date = label(shownDay.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", AR).withDecimalStyle(DecimalStyle.of(AR))), 14, MUTED, false);
        content.addView(date);
        TextView section = label("صلوات اليوم", 22, TEXT, true);
        section.setPadding(0,dp(6),0,dp(10));
        content.addView(section);
        TextView adhanHint=label("المفتاح بجوار كل صلاة لتشغيل الأذان",12,MUTED,false);
        adhanHint.setPadding(0,0,0,dp(8));
        content.addView(adhanHint);
        long[] times = PrayerEngine.times(shownDay, c);
        LinearLayout prayers = card();
        for (int i=0; i<6; i++) {
            final int index = i;
            LinearLayout line = row();
            line.setPadding(dp(15),dp(12),dp(15),dp(12));
            boolean highlighted = next != null && i == next.index && next.time == times[i];
            if (highlighted) line.setBackground(shape(Color.rgb(233,241,233),16));
            LinearLayout text = column();
            text.addView(label(PrayerEngine.NAMES[i], 18, highlighted ? TEAL : TEXT, true));
            if(i == 1) text.addView(label("بدون أذان", 11, MUTED, false));
            else {
                TextView iqamaLabel=label(c.iqamaEnabled[i] ? "الإقامة " + time(PrayerEngine.iqamaTime(times[i],i,c),c) : "تذكير الإقامة متوقف",11,MUTED,false);
                iqamaLabel.setOnClickListener(v -> iqamaDialog(index));
                iqamaLabel.setContentDescription("موعد إقامة "+PrayerEngine.NAMES[i]+"، اضغط لضبط الدقائق");
                text.addView(iqamaLabel);
                if(c.offsets[i] != 0) text.addView(label("تعديل " + (c.offsets[i] > 0 ? "+" : "") + c.offsets[i] + " دقيقة",11,MUTED,false));
            }
            text.setContentDescription(PrayerEngine.NAMES[i]+"، تعديل الوقت بالدقائق");
            if (i != 1) text.setOnClickListener(v -> offsetDialog(index));
            line.addView(text,new LinearLayout.LayoutParams(0,-2,1));
            TextView time = label(time(times[i],c),18,highlighted ? TEAL : TEXT,true);
            time.setPadding(dp(12),0,dp(12),0);
            line.addView(time);
            if (i != 1) {
                Switch sw = new Switch(this);
                sw.setChecked(c.enabled[i]);
                sw.setContentDescription("أذان " + PrayerEngine.NAMES[i]);
                tint(sw);
                sw.setOnCheckedChangeListener((b,on) -> {
                    AppSettings.prefs(this).edit().putBoolean("prayer_"+index,on).apply();
                    PrayerScheduler.schedule(this);
                    render();
                });
                line.addView(sw);
            } else { View spacer = new View(this); line.addView(spacer,new LinearLayout.LayoutParams(dp(48),1)); }
            prayers.addView(line);
            if (i < 5) { View sep = new View(this); sep.setBackgroundColor(LINE); prayers.addView(sep,new LinearLayout.LayoutParams(-1,dp(1))); }
        }
        block(prayers,0,18);

        LinearLayout enable = card();
        enable.setPadding(dp(18),dp(14),dp(18),dp(14));
        LinearLayout enableLine = row();
        LinearLayout enableText = column();
        enableText.addView(label("تنبيهات الصلاة",18,TEXT,true));
        enableText.addView(label("تشغيل الأذان وتذكير الإقامة تلقائيًا",12,MUTED,false));
        enableLine.addView(enableText,new LinearLayout.LayoutParams(0,-2,1));
        Switch main = new Switch(this);
        main.setContentDescription("تشغيل تنبيهات الأذان والإقامة");
        main.setChecked(AppSettings.active(this)); tint(main);
        main.setOnCheckedChangeListener((b,on) -> {
            AppSettings.prefs(this).edit().putBoolean("active",on).apply();
            if (!on) stopService(new Intent(this,AdhanService.class));
            PrayerScheduler.schedule(this);
            render();
            if (on) setupPermissions();
        });
        enableLine.addView(main);
        enable.addView(enableLine);
        if (AppSettings.active(this)) {
            long alarm = AppSettings.prefs(this).getLong("next_alarm",0);
            String status;
            if (!PrayerScheduler.notificationsAllowed(this)) status = "يلزم السماح بإشعارات الأذان";
            else if (!PrayerScheduler.exactAllowed(this)) status = "يلزم السماح بالمنبهات والتذكيرات";
            else if (alarm == 0) status = "الأذان متوقف: فعّل أذان صلاة واحدة على الأقل";
            else {
                PrayerEngine.Event enabled = PrayerEngine.next(now,c,true);
                String tomorrow = Instant.ofEpochMilli(alarm).atZone(c.zoneId()).toLocalDate().equals(shownDay) ? "" : " غدًا";
                status = "الأذان التالي: " + (enabled != null ? enabled.name() + " " : "") + time(alarm,c) + tomorrow;
            }
            TextView statusView = label(status,13,TEAL,true);
            statusView.setPadding(0,dp(10),0,0);
            enable.addView(statusView);
            if (!PrayerScheduler.notificationsAllowed(this) || !PrayerScheduler.exactAllowed(this)) enable.addView(button("استكمال تفعيل التنبيهات",false,v -> setupPermissions()));
        }
        block(enable,0,18);

        LinearLayout iqamaCard = card();
        iqamaCard.setPadding(dp(18),dp(16),dp(18),dp(16));
        iqamaCard.addView(label("تذكير الإقامة",20,TEXT,true));
        iqamaCard.addView(label("جرس قصير وإشعار بعد الأذان. هذه دقائق مبدئية؛ عدّلها حسب موعد الإقامة في مسجدك.",12,MUTED,false));
        for(int i : PrayerEngine.PRAYERS) {
            final int index=i;
            LinearLayout item=row();
            TextView name=label(PrayerEngine.NAMES[i],16,TEXT,true);
            name.setOnClickListener(v -> iqamaDialog(index));
            item.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            Button minutes=button("بعد "+number(c.iqamaMinutes[i])+" د",false,v -> iqamaDialog(index));
            minutes.setTextSize(13);
            item.addView(minutes,new LinearLayout.LayoutParams(dp(100),-2));
            Switch toggle=new Switch(this); tint(toggle);
            toggle.setChecked(c.iqamaEnabled[i]);
            toggle.setContentDescription("تذكير إقامة "+PrayerEngine.NAMES[i]);
            toggle.setOnCheckedChangeListener((b,on)->{
                AppSettings.prefs(this).edit().putBoolean("iqama_enabled_"+index,on).apply(); changed();
            });
            item.addView(toggle);
            iqamaCard.addView(item);
        }
        String iqamaStatus;
        if(!AppSettings.active(this)) iqamaStatus="شغّل تنبيهات الصلاة لتفعيل تذكير الإقامة";
        else if(!PrayerScheduler.notificationsAllowed(this,PrayerScheduler.IQAMA_CHANNEL)) iqamaStatus="يلزم السماح بإشعارات الإقامة";
        else if(!PrayerScheduler.exactAllowed(this)) iqamaStatus="يلزم السماح بالمنبهات والتذكيرات";
        else if(nextIqama==null) iqamaStatus="كل تذكيرات الإقامة متوقفة";
        else if(AppSettings.prefs(this).getLong("next_iqama",0)==0) iqamaStatus="لم يتم ضبط تذكير الإقامة؛ راجع صلاحيات التطبيق";
        else iqamaStatus="الإقامة التالية: "+nextIqama.name()+" "+time(nextIqama.time,c)+daySuffix(nextIqama.time,c);
        TextView iqamaStatusView=label(iqamaStatus,13,TEAL,true);
        iqamaStatusView.setPadding(0,dp(10),0,dp(6));
        iqamaCard.addView(iqamaStatusView);
        iqamaCard.addView(button("صوت تنبيه الإقامة وإشعاراته",false,v -> openChannelSettings(PrayerScheduler.IQAMA_CHANNEL)));
        iqamaCard.addView(button("اختبار الإقامة بعد دقيقة",false,v -> testAlarm(true)));
        block(iqamaCard,0,18);

        LinearLayout sound = card();
        sound.setPadding(dp(18),dp(16),dp(18),dp(16));
        sound.addView(label("صوت الأذان",19,TEXT,true));
        VoiceCatalog.Voice selectedVoice=VoiceCatalog.current(this);
        sound.addView(label(selectedVoice.name,17,TEAL,true));
        sound.addView(label(selectedVoice.description,12,MUTED,false));
        sound.addView(button("اختيار صوت المؤذن ومعاينته",false,v -> chooseVoice()));
        TextView volumeLabel = label("مستوى الأذان: " + number(AppSettings.prefs(this).getInt("volume",100)) + "٪",13,MUTED,false);
        sound.addView(volumeLabel);
        SeekBar volume = new SeekBar(this);
        volume.setMax(90); volume.setProgress(AppSettings.prefs(this).getInt("volume",100)-10);
        volume.setProgressTintList(ColorStateList.valueOf(TEAL)); volume.setThumbTintList(ColorStateList.valueOf(TEAL));
        volume.setContentDescription("مستوى صوت الأذان من 10 إلى 100 بالمائة");
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s,int value,boolean fromUser) {
                volumeLabel.setText("مستوى الأذان: " + number(value+10) + "٪");
                if (fromUser) AppSettings.prefs(MainActivity.this).edit().putInt("volume",value+10).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        sound.addView(volume,new LinearLayout.LayoutParams(-1,dp(42)));
        sound.addView(button("▶  تجربة الأذان",true,v -> preview()));
        sound.addView(button("اختبار التنبيه بعد دقيقة",false,v -> testAlarm()));
        sound.addView(button("إيقاف الأذان وإلغاء الاختبار",false,v -> {
            PrayerScheduler.cancelTest(this); stopService(new Intent(this,AdhanService.class));
        }));
        sound.addView(label("الصوت يتبع مستوى المنبّه في الهاتف. استخدم أزرار الصوت هنا لضبطه.",12,MUTED,false));
        block(sound,0,18);

        LinearLayout options = card();
        options.setPadding(dp(18),dp(14),dp(18),dp(14));
        options.addView(label("إعدادات المواقيت",19,TEXT,true));
        String methodName = c.method.equals("EGYPTIAN") ? "الهيئة المصرية للمساحة" : c.method.equals("UMM_AL_QURA") ? "أم القرى" : "رابطة العالم الإسلامي";
        options.addView(button("طريقة الحساب: " + methodName,false,v -> chooseMethod()));
        options.addView(button("حساب العصر: " + (c.hanafi ? "حنفي" : "شافعي / جمهور الفقهاء"),false,v -> chooseMadhab()));
        options.addView(button("ضبط دقائق كل صلاة",false,v -> chooseOffset()));
        options.addView(button("إعدادات البطارية والإشعارات",false,v -> systemSettings()));
        options.addView(label("المواقيت تُحسب محليًا بدون إنترنت. راجعها مع تقويم مسجدك، ويمكنك تعديل دقائق كل صلاة.",12,MUTED,false));
        block(options,0,18);

        String error = AppSettings.prefs(this).getString("schedule_error",AppSettings.prefs(this).getString("playback_error",""));
        if (!error.isEmpty()) { TextView errorView=label(error,13,Color.rgb(153,74,35),false); block(errorView,0,12); }
        TextView about = label("مواقيت الصلاة  •  الإصدار ١٫١\nعن التطبيق ومصادر التسجيلات",12,MUTED,false);
        about.setGravity(Gravity.CENTER);
        about.setPadding(0,dp(6),0,dp(14));
        about.setOnClickListener(v -> about());
        content.addView(about);
        scroll.post(() -> scroll.scrollTo(0,savedScroll));
    }

    private void updateCountdown() {
        if (countdown == null || next == null) return;
        long seconds = Math.max(0,(next.time-System.currentTimeMillis()+999)/1000);
        long hours = seconds/3600, minutes = (seconds%3600)/60;
        countdown.setText(hours > 0 ? "متبقي " + number(hours) + " ساعة و" + number(minutes) + " دقيقة" : minutes > 0 ? "متبقي " + number(minutes) + " دقيقة" : "اقترب وقت الصلاة");
        if(iqamaCountdown!=null) {
            if(!AppSettings.active(this)) iqamaCountdown.setText("تنبيهات الأذان والإقامة متوقفة");
            else if(nextIqama==null) iqamaCountdown.setText("تذكيرات الإقامة متوقفة");
            else {
                long remaining=Math.max(1,(nextIqama.time-System.currentTimeMillis()+59_999)/60_000);
                String left=remaining>=60 ? number(remaining/60)+" س "+number(remaining%60)+" د" : number(remaining)+" دقيقة";
                iqamaCountdown.setText("إقامة "+nextIqama.name()+" بعد "+left);
            }
        }
    }
    private void setupPermissions() {
        if (!PrayerScheduler.appNotificationsAllowed(this)) {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101);
            } else openNotificationSettings();
            return;
        }
        if (!PrayerScheduler.exactAllowed(this)) {
            new AlertDialog.Builder(this).setTitle("تنبيه في موعد الصلاة")
                .setMessage("فعّل السماح بالمنبهات والتذكيرات للتطبيق حتى يعمل الأذان وتذكير الإقامة في موعدهما والهاتف مقفول.")
                .setPositiveButton("فتح الإعدادات",(d,w) -> launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName()))))
                .setNegativeButton("لاحقًا",null).show();
            return;
        }
        if(!PrayerScheduler.notificationsAllowed(this) || !PrayerScheduler.notificationsAllowed(this,PrayerScheduler.IQAMA_CHANNEL)) {
            openNotificationSettings(); return;
        }
        PrayerScheduler.schedule(this); render();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(code,permissions,results);
        if(code == 101 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) setupPermissions();
        else if(code == 101) new AlertDialog.Builder(this).setTitle("السماح بالإشعارات")
            .setMessage("تنبيهات الصلاة متوقفة إلى أن تسمح بإشعارات التطبيق. يمكنك السماح بها من الإعدادات.")
            .setPositiveButton("الإعدادات",(d,w)->openNotificationSettings()).setNegativeButton("لاحقًا",null).show();
    }
    private void openNotificationSettings() {
        launch(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
    }
    private void openChannelSettings(String channel) {
        if(!PrayerScheduler.appNotificationsAllowed(this)) { setupPermissions(); return; }
        launch(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,channel));
    }
    private void preview() {
        preview(VoiceCatalog.current(this));
    }
    private void preview(VoiceCatalog.Voice voice) {
        AudioManager am=getSystemService(AudioManager.class);
        if (am.getStreamVolume(AudioManager.STREAM_ALARM)==0) Toast.makeText(this,"صوت المنبّه مكتوم؛ ارفعه بأزرار الصوت",Toast.LENGTH_LONG).show();
        try { startForegroundService(new Intent(this,AdhanService.class).putExtra("index",-1).putExtra("voice_id",voice.id)); }
        catch(RuntimeException ex) { Toast.makeText(this,"تعذر بدء الصوت؛ راجع إعدادات التطبيق",Toast.LENGTH_LONG).show(); }
    }
    private void testAlarm() { testAlarm(false); }
    private void testAlarm(boolean iqama) {
        if (!AppSettings.active(this)) {
            Toast.makeText(this,"شغّل تنبيهات الصلاة أولًا ثم جرّب التنبيه",Toast.LENGTH_LONG).show(); return;
        }
        String channel=iqama?PrayerScheduler.IQAMA_CHANNEL:PrayerScheduler.CHANNEL;
        if (!PrayerScheduler.appNotificationsAllowed(this) || !PrayerScheduler.exactAllowed(this)) { setupPermissions(); return; }
        if (!PrayerScheduler.notificationsAllowed(this,channel)) { openChannelSettings(channel); return; }
        try {
            PrayerScheduler.testInOneMinute(this,iqama);
            new AlertDialog.Builder(this).setTitle(iqama?"تم ضبط اختبار الإقامة":"تم ضبط اختبار الأذان")
                .setMessage(iqama?"سيصل جرس قصير وإشعار للإقامة بعد دقيقة. اقفل الشاشة لتجربته. يمكنك إلغاء الاختبار من زر إيقاف الأذان وإلغاء الاختبار.":"سيعمل الأذان بعد دقيقة. اقفل الشاشة لاختبار الصوت في الخلفية. يمكنك إلغاء الاختبار من زر إيقاف الأذان وإلغاء الاختبار.")
                .setPositiveButton("حسنًا",null).show();
        } catch (RuntimeException e) { Toast.makeText(this,"تعذر ضبط الاختبار؛ راجع صلاحية المنبهات",Toast.LENGTH_LONG).show(); }
    }
    private void chooseVoice() {
        String[] names=new String[VoiceCatalog.VOICES.length];
        int initial=0;
        for(int i=0;i<names.length;i++) {
            names[i]=VoiceCatalog.VOICES[i].display();
            if(VoiceCatalog.VOICES[i].id.equals(VoiceCatalog.current(this).id)) initial=i;
        }
        final int[] choice={initial}; final boolean[] previewed={false};
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("اختر صوت الأذان")
            .setSingleChoiceItems(names,initial,(d,i)->choice[0]=i)
            .setPositiveButton("حفظ",(d,w)->{
                AppSettings.prefs(this).edit().putString("voice_id",VoiceCatalog.VOICES[choice[0]].id).apply(); render();
            }).setNeutralButton("معاينة الصوت",null).setNegativeButton("إلغاء",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
            previewed[0]=true; preview(VoiceCatalog.VOICES[choice[0]]);
        }));
        dialog.setOnDismissListener(d->{ if(previewed[0]) stopService(new Intent(this,AdhanService.class)); });
        dialog.show();
    }
    private void iqamaDialog(int index) {
        PrayerEngine.Config c=AppSettings.config(this);
        LinearLayout body=column(); body.setPadding(dp(22),dp(14),dp(22),0);
        body.addView(label("حدد عدد الدقائق بعد أذان "+PrayerEngine.NAMES[index]+" وفق إقامة مسجدك. يتغير موعد الإقامة يوميًا مع الأذان.",14,MUTED,false));
        EditText input=new EditText(this); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(c.iqamaMinutes[index])); input.setSelectAllOnFocus(true);
        body.addView(input,new LinearLayout.LayoutParams(-1,-2));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("إقامة "+PrayerEngine.NAMES[index])
            .setView(body).setPositiveButton("حفظ",null).setNegativeButton("إلغاء",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try {
                int minutes=Integer.parseInt(normalize(input.getText().toString()));
                if(minutes<1||minutes>120) throw new IllegalArgumentException();
                AppSettings.prefs(this).edit().putInt("iqama_minutes_"+index,minutes).apply(); changed(); dialog.dismiss();
            } catch(Exception ex) { input.setError("أدخل من ١ إلى ١٢٠ دقيقة"); }
        })); dialog.show();
    }
    private void chooseCity() {
        String[] choices=new String[CITIES.length+1]; System.arraycopy(CITIES,0,choices,0,CITIES.length); choices[CITIES.length]="مدينة أخرى / إدخال الإحداثيات";
        new AlertDialog.Builder(this).setTitle("اختر المدينة").setItems(choices,(d,index)->{
            if(index == CITIES.length) { customCity(); return; }
            AppSettings.city(this,CITIES[index],COORDS[index][0],COORDS[index][1],"Africa/Cairo");
            changed();
        }).setNegativeButton("إغلاق",null).show();
    }
    private void customCity() {
        PrayerEngine.Config c=AppSettings.config(this);
        LinearLayout fields=column(); fields.setPadding(dp(22),dp(12),dp(22),0);
        EditText name=field(fields,"اسم المدينة",c.city,false);
        EditText lat=field(fields,"خط العرض، مثال 30.299",Double.toString(c.latitude),true);
        EditText lon=field(fields,"خط الطول، مثال 31.741",Double.toString(c.longitude),true);
        EditText zone=field(fields,"المنطقة الزمنية، مثال Africa/Cairo",c.zone,false);
        fields.addView(label("أدخل اسم المنطقة الزمنية كما في المثال. خط العرض المدعوم من ٥٥ جنوبًا إلى ٥٥ شمالًا.",12,MUTED,false));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("موقع مخصص").setView(fields).setPositiveButton("حفظ",null).setNegativeButton("إلغاء",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try {
                double a=Double.parseDouble(normalize(lat.getText().toString())), b=Double.parseDouble(normalize(lon.getText().toString()));
                String z=zone.getText().toString().trim(), n=name.getText().toString().trim();
                if(!Double.isFinite(a) || !Double.isFinite(b) || Math.abs(a)>55 || Math.abs(b)>180 || n.isEmpty()) throw new IllegalArgumentException();
                if (!ZoneId.getAvailableZoneIds().contains(z)) throw new IllegalArgumentException();
                AppSettings.city(this,n,a,b,z); changed(); dialog.dismiss();
            } catch(Exception ex) { Toast.makeText(this,"راجع الإحداثيات واسم المنطقة الزمنية",Toast.LENGTH_LONG).show(); }
        })); dialog.show();
    }
    private String normalize(String s) {
        StringBuilder result=new StringBuilder();
        for(char ch:s.trim().toCharArray()) {
            if(ch >= '٠' && ch <= '٩') result.append((char)('0'+ch-'٠'));
            else if(ch=='٫' || ch==',') result.append('.'); else result.append(ch);
        } return result.toString();
    }
    private EditText field(LinearLayout parent,String hint,String value,boolean numeric) {
        TextView caption=label(hint,12,MUTED,false); parent.addView(caption);
        EditText e=new EditText(this); e.setSingleLine(true); e.setText(value); e.setTextSize(15); e.setTextDirection(View.TEXT_DIRECTION_LTR);
        e.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED : InputType.TYPE_CLASS_TEXT);
        parent.addView(e,new LinearLayout.LayoutParams(-1,-2)); return e;
    }
    private void chooseMethod() {
        String[] names={"الهيئة المصرية للمساحة", "رابطة العالم الإسلامي", "أم القرى"}, keys={"EGYPTIAN","MUSLIM_WORLD_LEAGUE","UMM_AL_QURA"};
        new AlertDialog.Builder(this).setTitle("طريقة حساب المواقيت").setItems(names,(d,i)->{
            AppSettings.prefs(this).edit().putString("method",keys[i]).apply(); changed();
            if(i==2) new AlertDialog.Builder(this).setMessage("أم القرى: العشاء بعد المغرب بـ٩٠ دقيقة. في رمضان أضف ٣٠ دقيقة للعشاء من ضبط الدقائق، حسب تقويم بلدك.").setPositiveButton("حسنًا",null).show();
        }).setNegativeButton("إلغاء",null).show();
    }
    private void chooseMadhab() {
        new AlertDialog.Builder(this).setTitle("حساب وقت العصر").setItems(new String[]{"شافعي / جمهور الفقهاء", "حنفي"},(d,i)->{
            AppSettings.prefs(this).edit().putBoolean("hanafi",i==1).apply(); changed();
        }).setNegativeButton("إلغاء",null).show();
    }
    private void chooseOffset() {
        String[] names=new String[5]; for(int i=0;i<5;i++) names[i]=PrayerEngine.NAMES[PrayerEngine.PRAYERS[i]];
        new AlertDialog.Builder(this).setTitle("اختر الصلاة لضبط الدقائق").setItems(names,(d,i)->offsetDialog(PrayerEngine.PRAYERS[i])).show();
    }
    private void offsetDialog(int index) {
        LinearLayout body=column(); body.setPadding(dp(22),dp(16),dp(22),0);
        body.addView(label("التعديل يغيّر الوقت المعروض وموعد الأذان معًا. الموجب يؤخّر الأذان والسالب يقدّمه.",14,MUTED,false));
        EditText input=new EditText(this); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setText(String.valueOf(AppSettings.config(this).offsets[index])); body.addView(input);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("دقائق " + PrayerEngine.NAMES[index]).setView(body)
            .setPositiveButton("حفظ",null).setNeutralButton("إعادة للصفر",(d,w)->{AppSettings.prefs(this).edit().putInt("offset_"+index,0).apply();changed();})
            .setNegativeButton("إلغاء",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try { int n=Integer.parseInt(normalize(input.getText().toString()));
                if(n < -30 || n > 30) throw new IllegalArgumentException();
                AppSettings.prefs(this).edit().putInt("offset_"+index,n).apply(); changed(); dialog.dismiss();
            } catch(Exception ex) { input.setError("أدخل عددًا من −٣٠ إلى +٣٠"); }
        })); dialog.show();
    }
    private void systemSettings() {
        new AlertDialog.Builder(this).setTitle("ضمان وصول الأذان")
            .setMessage("١. اسمح بالإشعارات والمنبهات.\n٢. من إعدادات البطارية للتطبيق اختر «غير مقيّد» إذا كان هاتفك يؤخّر الأذان.\n٣. ارفع صوت المنبّه واختبر الصوت.\n٤. إذا أوقفت التطبيق إجباريًا، افتحه مرة أخرى لتفعيل التنبيهات.")
            .setPositiveButton("إعدادات التطبيق",(d,w)->launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))))
            .setNeutralButton("الإشعارات",(d,w)->openNotificationSettings()).setNegativeButton("إغلاق",null).show();
    }
    private void about() {
        new AlertDialog.Builder(this).setTitle("مواقيت الصلاة")
            .setMessage("الإصدار ١٫١\nالأصوات: محمد رفعت، مشاري العفاسي، ناصر القطامي، أحمد النفيس.\nمصادر التسجيلات: شبكة الشفاء، السبيل، AlAdhan.\nحساب المواقيت: Adhan Java 1.2.1.\nالإقامة: جرس قصير وإشعار بعد عدد الدقائق الذي تضبطه لكل صلاة. القيم الافتراضية ليست جدول إقامة رسميًا.\nكل الأصوات مدمجة ولا يحتاج التطبيق إلى الإنترنت، ولا يرسل بياناتك.\nالتسجيل التاريخي لمحمد رفعت محفوظ بجودته الأصلية. راجع مواقيت الصلاة مع تقويم بلدك.")
            .setPositiveButton("حسنًا",null).show();
    }
    private String daySuffix(long millis,PrayerEngine.Config c) {
        LocalDate day=Instant.ofEpochMilli(millis).atZone(c.zoneId()).toLocalDate();
        return day.equals(shownDay)?"":day.equals(shownDay.plusDays(1))?" غدًا":" "+day.format(DateTimeFormatter.ofPattern("d/M",AR));
    }
    private void changed() { PrayerScheduler.schedule(this); render(); }
    private void launch(Intent intent) {
        try { startActivity(intent); } catch(RuntimeException e) { Toast.makeText(this,"افتح إعدادات التطبيق من إعدادات الهاتف",Toast.LENGTH_LONG).show(); }
    }
    private String number(long value) { return String.format(AR,"%d",value); }
    private String time(long millis,PrayerEngine.Config c) {
        return Instant.ofEpochMilli(millis).atZone(c.zoneId()).format(DateTimeFormatter.ofPattern("h:mm a",AR).withDecimalStyle(DecimalStyle.of(AR)));
    }
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView label(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.START); t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setFontFeatureSettings("kern"); t.setIncludeFontPadding(true); t.setLineSpacing(dp(2),1f);
        if(bold) t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        return t;
    }
    private GradientDrawable shape(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private LinearLayout card() { LinearLayout l=column(); l.setBackground(shape(Color.WHITE,22)); return l; }
    private void block(View view,int top,int bottom) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(top),0,dp(bottom)); content.addView(view,p);
    }
    private Button button(String caption,boolean primary,View.OnClickListener click) {
        Button b=new Button(this); b.setText(caption); b.setTextSize(14); b.setAllCaps(false); b.setMinHeight(dp(48)); b.setMinimumHeight(dp(48));
        b.setTextColor(primary?Color.WHITE:TEAL); b.setBackgroundTintList(ColorStateList.valueOf(primary?TEAL:Color.rgb(239,243,236)));
        b.setOnClickListener(click); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(4),0,dp(2)); b.setLayoutParams(p); return b;
    }
    private void tint(Switch s) {
        s.setThumbTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{TEAL,Color.rgb(169,174,167)}));
        s.setTrackTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{Color.rgb(164,204,182),LINE}));
    }
}
