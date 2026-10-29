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
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout rootLayout, menuLayout, contentLayout;
    private TextView tvWatermark;
    private WindowManager.LayoutParams params;
    private FovView fovView;
    private int currentFovSize = 300; 
    private TextView tabAim, tabEsp;
    
    // Для захвата экрана
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

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

        setupUI();
        startFpsCounter();
    }

    private void setupUI() {
        // Настройка FOV и Меню (сокращенно, как в прошлом коде)
        fovView = new FovView(this);
        WindowManager.LayoutParams fovParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(fovView, fovParams);

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        
        tvWatermark = new TextView(this);
        tvWatermark.setText("ntNH-DLS | External");
        tvWatermark.setTextColor(COLOR_TEXT_MAIN);
        tvWatermark.setBackground(createBg(COLOR_BG, 15f, COLOR_ACCENT, 1));
        
        menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setBackground(createBg(COLOR_BG, 15f, Color.GRAY, 1));
        menuLayout.setPadding(30, 30, 30, 30);
        
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.addView(contentLayout);
        
        // Кнопка Теста Захвата Экрана
        Switch testCapture = createSwitch("Проверка ESP (Захват экрана)");
        testCapture.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) testScreenCapture();
        });
        contentLayout.addView(testCapture);

        rootLayout.addView(tvWatermark);
        rootLayout.addView(menuLayout);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100; params.y = 100;
        windowManager.addView(rootLayout, params);
    }

    private void testScreenCapture() {
        Intent intent = new Intent(this, MainActivity.class); // Заглушка, данные берутся из MainActivity
        // В реальности здесь мы должны получить данные из MainActivity.
        // Так как мы не передали данные (в MainActivity пока заглушка), мы просто тестируем.
        // В следующем шаге мы передадим реальный захват.
        
        Toast.makeText(this, "Функция в разработке (нужен реальный захват)", Toast.LENGTH_LONG).show();
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
        public void setFov(int radius) { this.radius = radius; invalidate(); }
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, paint); 
        }
    }

    private void startFpsCounter() {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                // Просто обновляем FPS для галочки
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

    private Switch createSwitch(String text) {
        Switch sw = new Switch(this);
        sw.setText(text);
        sw.setTextColor(COLOR_TEXT_SUB);
        return sw;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootLayout != null) windowManager.removeView(rootLayout);
        if (fovView != null) windowManager.removeView(fovView);
    }
}
