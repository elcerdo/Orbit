package com.example.pierre.orbit;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends Activity {
    private MainRenderer mainRenderer;
    private MainSurfaceView mainSurfaceView;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("Orbit", "create activity");
        ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = activityManager.getDeviceConfigurationInfo();
        if (info.reqGlEsVersion < 0x20000) {
            Log.e("Orbit", "your device doesn't support opengl es 2");
            return;
        }

        mainSurfaceView = new MainSurfaceView(this);
        mainSurfaceView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        mainSurfaceView.setEGLContextClientVersion(2);
        this.setContentView(mainSurfaceView);

        mainRenderer = new MainRenderer(this);
        mainSurfaceView.setRenderer(mainRenderer);

        gestureDetector = new GestureDetector(this, mainRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        {
            int pointer_up = evt.getActionMasked() == MotionEvent.ACTION_UP ? evt.getPointerId(evt.getActionIndex()) : -1;
            boolean turn_left = false;
            boolean turn_right = false;
            boolean burn = false;
            for (int kk = 0; kk < evt.getPointerCount(); kk++) {
                int pointer = evt.getPointerId(kk);
                if (pointer == pointer_up) continue;
                float xx = evt.getX(kk);
                float yy = mainRenderer.height - evt.getY(kk);
                if (yy > mainRenderer.button_size) continue;
                if (0 < xx && xx < mainRenderer.button_size) turn_left = true;
                if (mainRenderer.button_size < xx && xx < 2*mainRenderer.button_size) turn_right = true;
                if (mainRenderer.width-mainRenderer.button_size < xx && xx < mainRenderer.width) burn = true;
            }
            mainRenderer.turn_left = turn_left;
            mainRenderer.turn_right = turn_right;
            mainRenderer.burn = burn;
        }

        return gestureDetector.onTouchEvent(evt);
    }

}
