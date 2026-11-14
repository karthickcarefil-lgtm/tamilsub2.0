package com.karthik.tamilsub;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(48, 48, 48, 48);

        TextView t = new TextView(this);
        t.setText("Tamil Subtitle\nEnglish audio -> Tamil text");
        t.setTextSize(20);
        t.setGravity(Gravity.CENTER);
        l.addView(t);

        Button b1 = new Button(this);
        b1.setText("Step 1: Overlay permission");
        b1.setOnClickListener(v -> startActivity(new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName()))));
        l.addView(b1);

        Button b2 = new Button(this);
        b2.setText("Step 2: Mic permission");
        b2.setOnClickListener(v -> requestPermissions(new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS}, 1));
        l.addView(b2);

        Button b3 = new Button(this);
        b3.setText("Step 3: START");
        b3.setOnClickListener(v -> {
            boolean mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
            if (!Settings.canDrawOverlays(this) || !mic) {
                Toast.makeText(this, "Step 1 & 2 mudikkanum", Toast.LENGTH_LONG).show();
                return;
            }
            startForegroundService(new Intent(this, SubService.class));
            Toast.makeText(this, "Started! Ippo movie app-ku poi paarunga", Toast.LENGTH_LONG).show();
        });
        l.addView(b3);

        Button b4 = new Button(this);
        b4.setText("STOP");
        b4.setOnClickListener(v -> stopService(new Intent(this, SubService.class)));
        l.addView(b4);

        setContentView(l);
    }
}
