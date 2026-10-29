package org.ntnh_dls;

import android.app.Service;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout rootLayout;
    private LinearLayout menuLayout;
    private LinearLayout contentLayout;
    private TextView tvWatermark;
    private WindowManager.LayoutParams params;
    
    // Вкладки
    private TextView tabAim, tabEsp;

    private long lastFrameTime = 0;
    private int frameCount = 0;

    private final int COLOR_BG = Color.parseColor("#F2111111");
    private final int COLOR_ACCENT = Color.parseColor("#FF3366");
    private final int COLOR_TEXT_MAIN = Color.WHITE;
    private final int COLOR_TEXT_SUB = Color.parseColor("#AAAAAA");

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.START);

        // --- WATERMARK ---
        tvWatermark = new TextView(this);
        tvWatermark.setText("ntNH-DLS | External | 60 FPS");
        tvWatermark.setTextColor(COLOR_TEXT_MAIN);
        tvWatermark.setTextSize(14);
        tvWatermark.setPadding(20, 10, 20, 10);
        tvWatermark.setBackground(createBg(COLOR_BG, 15f, COLOR_ACCENT, 1));
        
        tvWatermark.setOnClickListener(v -> {
            if (menuLayout.getVisibility() == View.VISIBLE) {
                menuLayout.setVisibility(View.GONE);
            } else {
                menuLayout.setVisibility(View.VISIBLE);
            }
        });
        tvWatermark.setOnTouchListener(new DragListener());
        rootLayout.addView(tvWatermark);

        // --- МЕНЮ ---
        menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setBackground(createBg(COLOR_BG, 15f, Color.parseColor("#333333"), 1));
        menuLayout.setPadding(30, 30, 30, 30);
        menuLayout.setVisibility(View.VISIBLE);
        
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        menuParams.topMargin = 20;
        menuLayout.setLayoutParams(menuParams);

        // Шапка
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("MENU");
        title.setTextColor(COLOR_TEXT_MAIN);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        headerLayout.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnClose = new TextView(this);
        btnClose.setText("X");
        btnClose.setTextColor(Color.WHITE);
        btnClose.setTextSize(16);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setBackground(createBg(COLOR_ACCENT, 10f, 0, 0));
        btnClose.setWidth(60); btnClose.setHeight(60);
        btnClose.setOnClickListener(v -> stopSelf());
        headerLayout.addView(btnClose);
        menuLayout.addView(headerLayout);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
        divParams.topMargin = 20; divParams.bottomMargin = 20;
        menuLayout.addView(divider, divParams);

        // --- ВКЛАДКИ ---
        LinearLayout tabsLayout = new LinearLayout(this);
        tabsLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        tabAim = createTab("Aimbot");
        tabEsp = createTab("ESP");
        tabsLayout.addView(tabAim);
        tabsLayout.addView(tabEsp);
        menuLayout.addView(tabsLayout);

        // Контейнер для контента
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(0, 20, 0, 0);
        menuLayout.addView(contentLayout);

        // --- ОБРАБОТЧИКИ ВКЛАДОК ---
        tabAim.setOnClickListener(v -> {
            tabAim.setTextColor(COLOR_ACCENT);
            tabEsp.setTextColor(COLOR_TEXT_MAIN);
            showAimMenu();
        });
        
        tabEsp.setOnClickListener(v -> {
            tabEsp.setTextColor(COLOR_ACCENT);
            tabAim.setTextColor(COLOR_TEXT_MAIN);
            showEspMenu();
        });

        // Показываем Аим по умолчанию
        tabAim.setTextColor(COLOR_ACCENT);
        showAimMenu();

        rootLayout.addView(menuLayout);

        // --- ПАРАМЕТРЫ ОКНА ---
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

    // --- ЛОГИКА МЕНЮ ---
    private void showAimMenu() {
        contentLayout.removeAllViews();
        contentLayout.addView(createSwitch("Aim Enable"));
        contentLayout.addView(createSwitch("Silent Aim"));
        contentLayout.addView(createSwitch("Trigger Bot"));
    }

    private void showEspMenu() {
        contentLayout.removeAllViews();
        contentLayout.addView(createSwitch("ESP Players"));
        contentLayout.addView(createSwitch("2D Boxes"));
        contentLayout.addView(createSwitch("Health Bar"));
        contentLayout.addView(createSwitch("Distance"));
    }

    // --- UI УТИЛИТЫ ---
    private void startFpsCounter() {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (lastFrameTime == 0) lastFrameTime = frameTimeNanos;
                frameCount++;
                long diff = frameTimeNanos - lastFrameTime;
                if (diff > 1000000000L) {
                    int currentFps = (int) (frameCount * 1000000000L / diff);
                    tvWatermark.setText("ntNH-DLS | External | " + currentFps + " FPS");
                    frameCount = 0; lastFrameTime = frameTimeNanos;
                }
                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    private GradientDrawable createBg(int color, float radius, int strokeColor, int strokeWidth) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        if (strokeWidth > 0) bg.setStroke(strokeWidth, strokeColor);
        return bg;
    }

    private TextView createTab(String text) {
        TextView tab = new TextView(this);
        tab.setText(text);
        tab.setTextColor(COLOR_TEXT_MAIN);
        tab.setTextSize(16);
        tab.setPadding(0, 10, 40, 10);
        tab.setBackground(createBg(Color.TRANSPARENT, 0, 0, 0));
        return tab;
    }

    private Switch createSwitch(String text) {
        Switch sw = new Switch(this);
        sw.setText(text);
        sw.setTextColor(COLOR_TEXT_SUB);
        sw.setTextSize(15);
        sw.setPadding(0, 10, 0, 10);
        
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };
        int[] thumbColors = new int[] { COLOR_ACCENT, Color.GRAY };
        int[] trackColors = new int[] { Color.parseColor("#55FF3366"), Color.parseColor("#333333") };
        sw.setThumbTintList(new ColorStateList(states, thumbColors));
        sw.setTrackTintList(new ColorStateList(states, trackColors));
        
        return sw;
    }

    // Класс для перетаскивания (чтобы не дублировать код)
    private class DragListener implements View.OnTouchListener {
        private int initialX, initialY;
        private float initialTouchX, initialTouchY;
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x; initialY = params.y;
                    initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(rootLayout, params);
                    return true;
            }
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootLayout != null) windowManager.removeView(rootLayout);
    }
}
