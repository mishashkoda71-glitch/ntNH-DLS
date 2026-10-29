package org.ntnh_dls;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class CheatService extends AccessibilityService {
    private static CheatService instance;

    public static CheatService getInstance() {
        return instance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this; // Сохраняем ссылку для доступа из оверлея
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    // Метод для свайпа вниз (компенсация отдачи)
    public void doNoRecoilSwipe() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Path swipePath = new Path();
            // Свайп от центра экрана вниз на 50 пикселей
            swipePath.moveTo(500, 1000); 
            swipePath.lineTo(500, 1050); 

            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 50));
            
            // Выполняем свайп
            dispatchGesture(gestureBuilder.build(), null, null);
        }
    }
}
