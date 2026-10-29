package org.ntnh_dls;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    
    private EditText keyInput;
    private Button btnLaunch;

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

        keyInput = new EditText(this);
        keyInput.setHint("XXXX-1234-XXXX-1234");
        keyInput.setTextSize(18);
        layout.addView(keyInput);

        Button btnActivate = new Button(this);
        btnActivate.setText("Активировать ключ");
        layout.addView(btnActivate);

        btnLaunch = new Button(this);
        btnLaunch.setText("Запустить Standoff 2");
        btnLaunch.setEnabled(false);
        layout.addView(btnLaunch);

        btnActivate.setOnClickListener(v -> {
            String key = keyInput.getText().toString().trim().toUpperCase();
            String regex = "^[A-Z]{4}-\\d{4}-[A-Z]{4}-\\d{4}$";
            
            if (Pattern.matches(regex, key)) {
                if (key.equals("VVZR-2026-TEST-1234")) {
                    Toast.makeText(this, "✅ Ключ активирован!", Toast.LENGTH_LONG).show();
                    btnLaunch.setEnabled(true);
                    btnActivate.setEnabled(false);
                    keyInput.setEnabled(false);
                } else {
                    Toast.makeText(this, "❌ Ключ не найден (тест: VVZR-2026-TEST-1234)", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "⚠️ Неверный формат! Нужно: XXXX-1234-XXXX-1234", Toast.LENGTH_LONG).show();
            }
        });

        btnLaunch.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, OverlayService.class);
            startService(serviceIntent);

            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage("com.axlebolt.standoff2");
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, "Standoff 2 не установлен!", Toast.LENGTH_LONG).show();
            }
        });

        Button btnStop = new Button(this);
        btnStop.setText("Остановить оверлей");
        btnStop.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, OverlayService.class);
            stopService(serviceIntent);
            Toast.makeText(this, "Оверлей выключен", Toast.LENGTH_SHORT).show();
        });
        layout.addView(btnStop);

        setContentView(layout);
    }
                           }
