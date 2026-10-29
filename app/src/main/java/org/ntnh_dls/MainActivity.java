package org.ntnh_dls;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        TextView text = new TextView(this);
        text.setText("ntNH-DLS Overlay");
        text.setTextSize(24);
        layout.addView(text);

        Button btnOverlay = new Button(this);
        btnOverlay.setText("Дать разрешение на оверлей");
        btnOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });
        layout.addView(btnOverlay);

        Button btnLaunch = new Button(this);
        btnLaunch.setText("Запустить Standoff 2");
        btnLaunch.setOnClickListener(v -> {
            // 1. Запускаем наш оверлей
            Intent serviceIntent = new Intent(this, OverlayService.class);
            startService(serviceIntent);

            // 2. Запускаем игру
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage("com.axlebolt.standoff2");
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, "Standoff 2 не установлен!", Toast.LENGTH_LONG).show();
            }
        });
        layout.addView(btnLaunch);

        setContentView(layout);
    }
}
