package com.karthik.tamilsub;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class SubService extends Service {
    WindowManager wm;
    TextView tv;
    SpeechRecognizer sr;
    Handler h = new Handler(Looper.getMainLooper());
    boolean running = false;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel("sub", "Tamil Subtitle",
            NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, "sub")
            .setContentTitle("Tamil Subtitle running")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(1, n);
        }

        if (tv == null) {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            tv = new TextView(this);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(20);
            tv.setBackgroundColor(0xAA000000);
            tv.setPadding(24, 12, 24, 12);
            tv.setGravity(Gravity.CENTER);
            tv.setText("Tamil subtitle ready...");
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.BOTTOM;
            p.y = 120;
            wm.addView(tv, p);
            running = true;
            listen();
        }
        return START_STICKY;
    }

    void listen() {
        if (!running) return;
        if (sr != null) sr.destroy();
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        Intent r = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        r.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        r.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        sr.setRecognitionListener(new RecognitionListener() {
            public void onResults(Bundle b) {
                ArrayList<String> x = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (x != null && !x.isEmpty()) translate(x.get(0));
                again();
            }
            public void onError(int e) { again(); }
            public void onReadyForSpeech(Bundle b) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float v) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onPartialResults(Bundle b) {}
            public void onEvent(int t, Bundle b) {}
        });
        sr.startListening(r);
    }

    void again() { h.postDelayed(this::listen, 300); }

    void translate(String s) {
        new Thread(() -> {
            try {
                URL u = new URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ta&dt=t&q="
                    + URLEncoder.encode(s, "UTF-8"));
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONArray a = new JSONArray(sb.toString()).getJSONArray(0);
                StringBuilder out = new StringBuilder();
                for (int i = 0; i < a.length(); i++) out.append(a.getJSONArray(i).getString(0));
                String result = out.toString();
                h.post(() -> { if (tv != null) tv.setText(result); });
            } catch (Exception e) { }
        }).start();
    }

    @Override
    public void onDestroy() {
        running = false;
        if (sr != null) sr.destroy();
        if (tv != null) { wm.removeView(tv); tv = null; }
        super.onDestroy();
    }
}
