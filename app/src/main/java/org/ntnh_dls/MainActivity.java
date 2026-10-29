package org.ntnh_dls;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
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
    private MediaProjectionManager projectionManager;

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

        // 1. Разрешение оверлея
        Button btnOverlay = new Button(this);
        btnOverlay.setText("1. Дать разрешение на оверлей");
        btnOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });
        layout.addView(btnOverlay);

        // 2. Спец. возможности
        Button btnAccess = new Button(this);
        btnAccess.setText("2. Разрешить Спец. возможности");
        btnAccess.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        layout.addView(btnAccess);

        // 3. Захват экрана
        Button btnCapture = new Button(this);
        btnCapture.setText("3. Разрешить Захват Экрана (для ESP)");
        btnCapture.setOnClickListener(v -> {
            projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(projectionManager.createScreenCaptureIntent(), 1001);
        });
        layout.addView(btnCapture);

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
            if (key.equals("VVZR-2026-TEST-1234")) {
                Toast.makeText(this, "✅ Ключ активирован!", Toast.LENGTH_LONG).show();
                btnLaunch.setEnabled(true);
                btnActivate.setEnabled(false);
                keyInput.setEnabled(false);
            } else {
                Toast.makeText(this, "❌ Ключ не найден", Toast.LENGTH_LONG).show();
            }
        });

        btnLaunch.setOnClickListener(v -> {
            // Передаем данные захвата в оверлей
            Intent serviceIntent = new Intent(this, OverlayService.class);
            if (projectionManager != null && resultData != null) {
                 serviceIntent.putExtra("resultCode", resultCode);
                 serviceIntent.putExtra("data", resultData);
            }
            startService(serviceIntent);

            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage("com.axlebolt.standoff2");
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, "Standoff 2 не установлен!", Toast.LENGTH_LONG).show();
            }
        });

        setContentView(layout);
    }

    private int resultCode;
    private Intent resultData;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                this.resultCode = resultCode;
                this.resultData = data;
                Toast.makeText(this, "Захват экрана разрешен!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Захват экрана отклонен", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
