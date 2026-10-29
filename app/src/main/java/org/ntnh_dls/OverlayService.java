package org.ntnh_dls;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout rootLayout;
    private LinearLayout menuLayout;
    private TextView tvFps;
    
    // Переменная для состояния Аима
    private boolean isAimEnabled = false;
    
    private long lastFrameTime = 0;
    private int frameCount = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // --- 1. FPS СЧЕТЧИК ---
        tvFps = new TextView(this);
        tvFps.setText("0 FPS");
        tvFps.setTextColor(Color.GREEN);
        tvFps.setTextSize(18);
        tvFps.setPadding(20, 10, 20, 10);
        tvFps.setBackgroundColor(Color.parseColor("#80000000")); 
        
        // По клику на FPS показываем/скрываем меню
        tvFps.setOnClickListener(v -> {
            if (menuLayout.getVisibility() == View.GONE) {
                menuLayout.setVisibility(View.VISIBLE);
            } else {
                menuLayout.setVisibility(View.GONE);
            }
        });
        rootLayout.addView(tvFps);

        // --- 2. МЕНЮ ---
        menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setBackgroundColor(Color.parseColor("#80000000"));
        menuLayout.setVisibility(View.VISIBLE); // СРАЗУ ВИДИМО!
        menuLayout.setPadding(30, 30, 30, 30);

        // Кнопка Aim (Рабочая)
        Button btnAim = new Button(this);
        btnAim.setText("Aim: OFF");
        btnAim.setBackgroundColor(Color.GRAY);
        btnAim.setOnClickListener(v -> {
            isAimEnabled = !isAimEnabled; // Переключаем состояние
            if (isAimEnabled) {
                btnAim.setText("Aim: ON");
                btnAim.setBackgroundColor(Color.GREEN);
            } else {
                btnAim.setText("Aim: OFF");
                btnAim.setBackgroundColor(Color.GRAY);
            }
        });
        menuLayout.addView(btnAim);

        Button btnEsp = new Button(this);
        btnEsp.setText("ESP Enable");
        menuLayout.addView(btnEsp);

        // --- ПОЛЗУНОК РАЗМЕРА ---
        TextView sizeLabel = new TextView(this);
        sizeLabel.setText("Размер меню:");
        sizeLabel.setTextColor(Color.WHITE);
        sizeLabel.setTextSize(16);
        menuLayout.addView(sizeLabel);

        SeekBar sizeSeekBar = new SeekBar(this);
        sizeSeekBar.setMax(150); 
        sizeSeekBar.setProgress(100); 
        
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 50) {
                    progress = 50; 
                    seekBar.setProgress(progress);
                }
                float scale = progress / 100f;
                rootLayout.setScaleX(scale);
                rootLayout.setScaleY(scale);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        menuLayout.addView(sizeSeekBar);

        // Кнопка закрытия
        Button btnClose = new Button(this);
        btnClose.setText("CLOSE");
        btnClose.setBackgroundColor(Color.RED);
        btnClose.setOnClickListener(v -> stopSelf());
        menuLayout.addView(btnClose);

        rootLayout.addView(menuLayout);

        // --- 3. ПАРАМЕТРЫ ОКНА ---
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 50; 

        windowManager.addView(rootLayout, params);

        startFpsCounter();
    }

    private void startFpsCounter() {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (lastFrameTime == 0) {
                    lastFrameTime = frameTimeNanos;
                }
                frameCount++;
                long diff = frameTimeNanos - lastFrameTime;
                
                if (diff > 1000000000L) {
                    int currentFps = (int) (frameCount * 1000000000L / diff);
                    tvFps.setText(currentFps + " FPS");
                    frameCount = 0;
                    lastFrameTime = frameTimeNanos;
                }
                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootLayout != null) {
            windowManager.removeView(rootLayout);
        }
    }
}
