package org.ntnh_dls;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout rootLayout;
    private LinearLayout menuLayout;
    private LinearLayout contentLayout;
    private TextView tvWatermark;
    private WindowManager.LayoutParams params;
    
    private FovView fovView;
    private int currentFovSize = 300; 

    private TextView tabAim, tabEsp;
    private long lastFrameTime = 0;
    private int frameCount = 0;
    
    // Для Антиотдачи
    private boolean isNoRecoilActive = false;

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

        // Слой FOV
        fovView = new FovView(this);
        WindowManager.LayoutParams fovParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE 
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(fovView, fovParams);

        // Основное меню
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.START);

        tvWatermark = new TextView(this);
        tvWatermark.setText("ntNH-DLS | External | 60 FPS");
        tvWatermark.setTextColor(COLOR_TEXT_MAIN);
        tvWatermark.setTextSize(14);
        tvWatermark.setPadding(20, 10, 20, 10);
        tvWatermark.setBackground(createBg(COLOR_BG, 15f, COLOR_ACCENT, 1));
        
        tvWatermark.setOnClickListener(v -> {
            menuLayout.setVisibility(menuLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });
        tvWatermark.setOnTouchListener(new DragListener());
        rootLayout.addView(tvWatermark);

        menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setBackground(createBg(COLOR_BG, 15f, Color.parseColor("#333333"), 1));
        menuLayout.setPadding(30, 30, 30, 30);
        menuLayout.setVisibility(View.VISIBLE);
        
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        menuParams.topMargin = 20;
        menuLayout.setLayoutParams(menuParams);

        // Header
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

        // Вкладки
        LinearLayout tabsLayout = new LinearLayout(this);
        tabsLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        tabAim = createTab("Aimbot");
        tabEsp = createTab("ESP");
        tabsLayout.addView(tabAim);
        tabsLayout.addView(tabEsp);
        menuLayout.addView(tabsLayout);

        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(0, 20, 0, 0);
        menuLayout.addView(contentLayout);

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

        tabAim.setTextColor(COLOR_ACCENT);
        showAimMenu();

        rootLayout.addView(menuLayout);

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

    private void showAimMenu() {
        contentLayout.removeAllViews();
        contentLayout.addView(createSwitch("Aim Enable"));
        
        TextView fovLabel = new TextView(this);
        fovLabel.setText("FOV Size: " + currentFovSize);
        fovLabel.setTextColor(COLOR_TEXT_SUB);
        fovLabel.setTextSize(14);
        fovLabel.setPadding(0, 10, 0, 5);
        contentLayout.addView(fovLabel);

        SeekBar fovSeekBar = new SeekBar(this);
        fovSeekBar.setMax(1000); 
        fovSeekBar.setProgress(currentFovSize);
        fovSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 50) progress = 50; 
                currentFovSize = progress;
                fovLabel.setText("FOV Size: " + currentFovSize);
                fovView.setFov(currentFovSize); 
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        contentLayout.addView(fovSeekBar);

        // Кнопка Антиотдачи
        Switch swNoRecoil = createSwitch("No Recoil (Свайпы)");
        swNoRecoil.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isNoRecoilActive = isChecked;
            if (isChecked) {
                startNoRecoilLoop();
            }
        });
        contentLayout.addView(swNoRecoil);
    }

    private void showEspMenu() {
        contentLayout.removeAllViews();
        contentLayout.addView(createSwitch("ESP Players"));
        contentLayout.addView(createSwitch("2D Boxes"));
        contentLayout.addView(createSwitch("Health Bar"));
    }

    // Запуск цикла свайпов
    private void startNoRecoilLoop() {
        new Thread(() -> {
            while (isNoRecoilActive) {
                try {
                    if (CheatService.getInstance() != null) {
                        CheatService.getInstance().doNoRecoilSwipe();
                    }
                    Thread.sleep(100); 
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    class FovView extends View {
        private Paint paint;
        private int radius = 300;

        public FovView(Context context) {
            super(context);
            paint = new Paint();
            paint.setColor(Color.parseColor("#FF3366")); 
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setAntiAlias(true);
        }

        public void setFov(int radius) {
            this.radius = radius;
            invalidate(); 
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            canvas.drawCircle(cx, cy, radius, paint); 
        }
    }

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
        if (fovView != null) windowManager.removeView(fovView);
        isNoRecoilActive = false; // Останавливаем цикл при закрытии
    }
          }
