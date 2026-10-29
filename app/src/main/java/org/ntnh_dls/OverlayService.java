package org.ntnh_dls;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout rootLayout;
    private LinearLayout contentLayout;
    private TextView tvFps;
    private WindowManager.LayoutParams params;

    // Цвета Zenin
    private final int COLOR_BG = Color.parseColor("#EE1A1A1A");
    private final int COLOR_ACCENT = Color.parseColor("#FFFF3366");
    private final int COLOR_TEXT = Color.WHITE;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Главный контейнер
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackground(createBackground(COLOR_BG, 20f, COLOR_ACCENT, 2));
        rootLayout.setPadding(30, 30, 30, 30);
        rootLayout.setElevation(10f);

        // --- 1. ВЕРХНЯЯ ПАНЕЛЬ (Заголовок и FPS) ---
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        tvFps = new TextView(this);
        tvFps.setText("ntNH-DLS | 60 FPS");
        tvFps.setTextColor(COLOR_ACCENT);
        tvFps.setTextSize(16);
        tvFps.setPadding(0, 0, 20, 0);
        
        // Тащим за эту панель
        tvFps.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x; initialY = params.y;
                        initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(rootLayout, params);
                        return true;
                }
                return false;
            }
        });
        topBar.addView(tvFps, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // Кнопка закрытия (X)
        TextView btnClose = new TextView(this);
        btnClose.setText("X");
        btnClose.setTextColor(COLOR_TEXT);
        btnClose.setTextSize(20);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setBackground(createBackground(COLOR_ACCENT, 10f, 0, 0));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(80, 80);
        btnClose.setLayoutParams(closeParams);
        btnClose.setOnClickListener(v -> stopSelf());
        topBar.addView(btnClose);

        rootLayout.addView(topBar);

        // --- 2. ВКЛАДКИ ---
        LinearLayout tabsLayout = new LinearLayout(this);
        tabsLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabsLayout.setPadding(0, 20, 0, 20);

        TextView tabAimbot = createTab("Aimbot");
        TextView tabEsp = createTab("ESP");
        tabsLayout.addView(tabAimbot);
        tabsLayout.addView(tabEsp);
        rootLayout.addView(tabsLayout);

        // --- 3. КОНТЕНТ (Сюда будем пихать настройки) ---
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.addView(contentLayout);

        // Показываем вкладку Aimbot по умолчанию
        showAimbotTab();

        // Обработчики вкладок
        tabAimbot.setOnClickListener(v -> showAimbotTab());
        tabEsp.setOnClickListener(v -> showEspTab());

        // --- 4. ПАРАМЕТРЫ ОКНА ---
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100; params.y = 100;

        windowManager.addView(rootLayout, params);
        startFpsCounter();
    }

    // --- ЛОГИКА ВКЛАДОК ---
    private void showAimbotTab() {
        contentLayout.removeAllViews();
        
        Switch aimSwitch = createSwitch("Aim Enable");
        contentLayout.addView(aimSwitch);

        Switch silentAim = createSwitch("Silent Aim");
        contentLayout.addView(silentAim);

        Switch triggerBot = createSwitch("Trigger Bot");
        contentLayout.addView(triggerBot);
    }

    private void showEspTab() {
        contentLayout.removeAllViews();

        Switch espPlayers = createSwitch("ESP Players");
        contentLayout.addView(espPlayers);

        Switch espBox = createSwitch("2D Boxes");
        contentLayout.addView(espBox);

        Switch espHealth = createSwitch("Health Bar");
        contentLayout.addView(espHealth);
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ДИЗАЙНА ---
    private TextView createTab(String text) {
        TextView tab = new TextView(this);
        tab.setText(text);
        tab.setTextColor(COLOR_TEXT);
        tab.setTextSize(18);
        tab.setPadding(30, 10, 30, 10);
        tab.setBackground(createBackground(Color.TRANSPARENT, 0, Color.TRANSPARENT, 0));
        tab.setOnClickListener(v -> {
            // Подсветка активной вкладки (упрощенно)
            tab.setTextColor(COLOR_ACCENT);
        });
        return tab;
    }

    private Switch createSwitch(String text) {
        Switch sw = new Switch(this);
        sw.setText(text);
        sw.setTextColor(COLOR_TEXT);
        sw.setTextSize(16);
        sw.setPadding(10, 10, 10, 10);
        // Красим переключатель в красный (через программную настройку сложно, оставим стандартный)
        return sw;
    }

    private GradientDrawable createBackground(int color, float radius, int strokeColor, int strokeWidth) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        if (strokeWidth > 0) bg.setStroke(strokeWidth, strokeColor);
        return bg;
    }

    private void startFpsCounter() {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (frameCount == 0) lastFrameTime = frameTimeNanos;
                frameCount++;
                long diff = frameTimeNanos - lastFrameTime;
                if (diff > 1000000000L) {
                    int currentFps = (int) (frameCount * 1000000000L / diff);
                    tvFps.setText("ntNH-DLS | " + currentFps + " FPS");
                    frameCount = 0; lastFrameTime = frameTimeNanos;
                }
                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    private long lastFrameTime = 0;
    private int frameCount = 0;

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootLayout != null) windowManager.removeView(rootLayout);
    }
}
